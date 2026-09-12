package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Instant
import java.time.Duration
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import java.util.UUID
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.Level
import org.magic.magicaddons.data.greenhouse.CROP_HEIGHT
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.Footprint
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.FarmlandBlock
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.render.WorldRenderer
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils

/**
 * preset hologram in world renderer starting with soil blocks then plants
 */
object LayoutRenderState {

    /** full block outline for stand only plants */
    private val FULL_BLOCK: VoxelShape = Shapes.block()

    private const val PULSE_ALPHA_LOW: Int = 0x28
    private const val PULSE_ALPHA_HIGH: Int = 0x70

    /** Enough color to read the mark through, little enough to see the block under it. */
    private const val FILL_ALPHA: Int = 0x4D

    /** how solid a ghost block is drawn */
    private fun ghostAlpha(): Int = GreenhousePresets.plantAlpha()

    /** Pale on purpose: the tint multiplies the texture, and a saturated one drains the block's colour. */
    private const val GHOST_TINT: Int = 0xFFB8CCFF.toInt()

    /** The glow around a ghosted head, drawn as itself rather than multiplied over a texture. */
    @JvmStatic
    val ghostOutlineColor: Int get() = PlannerMark.Missing.color

    /** State a plan does not care about, because nothing the player does decides it. */
    private val IGNORED_PROPERTIES: List<IntegerProperty> = listOf(FarmlandBlock.MOISTURE)

    /**
     * Blocks whose state is the world's business: fire keeps an age and a face per neighbour, both
     * changing on their own, which made a correctly placed fire flicker between right and wrong.
     */
    private val STATE_IS_NOT_OURS: Set<Block> = setOf(Blocks.FIRE)

    /** Ground a hoe turns into other ground: untilled dirt is not the wrong block. */
    private val TILLABLE: Set<Block> = setOf(
        Blocks.DIRT,
        Blocks.GRASS_BLOCK,
        Blocks.COARSE_DIRT,
        Blocks.ROOTED_DIRT,
        Blocks.DIRT_PATH,
        Blocks.FARMLAND
    )

    /** The same warning worn by an entity, which is tinted rather than outlined. */
    const val RED_TINT: Int = 0x90FF0000.toInt()


    /** Which half of the job the player is on. */
    enum class Phase {
        Soil,
        Crops
    }

    /**
     * everything about the hologram, stage or soil phase, what marks, ghosts to render and things in the way
     */
    private class PlannerLayout(
        val phase: Phase,
        val marks: Map<BlockPos, Pair<VoxelShape, PlannerMark>>,
        val ghosts: Map<BlockPos, BlockState>,
        val badStands: Set<UUID>,
        /** soils to replace with another block. */
        val blocked: Set<BlockPos>,
        /**
         * ghost stands are separated by crops to not reconstruct plant ghosts that havent been touched.
         */
        val standGroups: Map<String, List<ArmorStand>>,

        val watchMarks: Map<BlockPos, Pair<VoxelShape, PlannerMark>> = emptyMap(),
        val watchStands: Map<UUID, Int> = emptyMap()
    ) {
        val ghostStands: List<ArmorStand> = standGroups.values.flatten()

        /** plan strings to compare, cheaper than using entity matching. */
        val plannerLayoutString: String = buildString {
            append(phase).append('|')
            marks.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value.second).append(',') }
            append('|')
            ghosts.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value).append(',') }
            append('|')
            badStands.map { it.toString() }.sorted().forEach { append(it).append(',') }
            append('|')
            blocked.map { it.asLong() }.sorted().forEach { append(it).append(',') }
            append('|')
            standGroups.keys.sorted().forEach { append(it).append(';') }
            append('|')
            watchMarks.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value.second).append(',') }
            append('|')
            watchStands.keys.map { it.toString() }.sorted().forEach { append(it).append(',') }
        }

        companion object {
            val NOTHING = PlannerLayout(Phase.Soil, emptyMap(), emptyMap(), emptySet(), emptySet(), emptyMap())
        }
    }

    @Volatile
    private var plannerLayout: PlannerLayout = PlannerLayout.NOTHING

    fun standTint(stand: UUID): Int {
        val current = plannerLayout

        return when {
            stand in current.badStands -> RED_TINT
            else -> current.watchStands[stand] ?: 0
        }
    }

    val ghostStands: List<ArmorStand> get() = plannerLayout.ghostStands

    val hasSomethingToShow: Boolean
        get() = plannerLayout.marks.isNotEmpty() || plannerLayout.ghosts.isNotEmpty() || plannerLayout.badStands.isNotEmpty() || plannerLayout.blocked.isNotEmpty()

    private var lastFinished: Boolean = false

    private var lastAnnouncedAt: Instant? = null

    private val ANNOUNCE_COOLDOWN: Duration = Duration.ofSeconds(30)

    /** crops with no first stage to avoid repeat messages */
    private val reportedMissingStage = mutableSetOf<String>()

    /** Draws the plan from the frame's own render pass, against the camera that frame uses. */
    fun submitPlan(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val plan = this.plannerLayout
        if (plan.marks.isEmpty() && plan.ghosts.isEmpty() && plan.watchMarks.isEmpty()) return

        // gathered first and handed over as one batch a render type; see WorldRender.Batch
        val presetBatch = WorldRenderer.BlockRenderBatch(cameraPos)
        val pulse = WorldRenderer.pulsedAlpha(PULSE_ALPHA_LOW, PULSE_ALPHA_HIGH)

        plan.marks.forEach { (pos, mark) -> presetBatch.fillWithOutline(pos, mark.first, mark.second.color, FILL_ALPHA) }
        plan.watchMarks.forEach { (pos, mark) -> presetBatch.fillWithOutline(pos, mark.first, mark.second.color, pulse) }
        plan.ghosts.forEach { (pos, state) -> presetBatch.ghostBlockWithOutline(pos, state, GHOST_TINT, PlannerMark.Missing.color, ghostAlpha()) }
        presetBatch.submitBatch(poseStack, collector)
    }

    /** Starts over on the plan of whichever greenhouse the player is in. */
    /** Forgets which crops were reported missing, then refreshes. */
    fun show() {
        reportedMissingStage.clear()

        refresh()
    }

    fun hide() {
        plannerLayout = PlannerLayout.NOTHING
        reportedMissingStage.clear()
    }

    /** Works out what to draw from what the plot holds now. Cheap enough to run on every change. */
    fun refresh() {
        val grid = GreenhouseData.getCurrentGrid()

        PlannerNeeds.arriveAt(grid)

        // losing sight of the greenhouse is not the same as having nothing to draw, so the last
        // plan stays up rather than blinking out on every unreadable moment
        if (grid == null) return

        val level = Minecraft.getInstance().level ?: return

        // the plan belongs to this greenhouse, so standing in another shows that one's plan or
        // nothing rather than carrying the last one around the garden
        val assigned = grid.state.assignedLayout

        if (assigned == null) {
            hide()
            return
        }

        // turned the way it was found to fit on assign
        val layout = assigned.turned(grid.state.planTurns)

        val marks = mutableMapOf<BlockPos, Pair<VoxelShape, PlannerMark>>()
        val ghosts = mutableMapOf<BlockPos, BlockState>()
        val badStands = mutableSetOf<UUID>()
        val blocked = mutableSetOf<BlockPos>()
        val standGroups = mutableMapOf<String, List<ArmorStand>>()

        // what is already up, to take the unchanged parts of it over rather than build them again
        val previous = plannerLayout

        var soilComplete = true
        val soilNeeded = linkedMapOf<Block, Int>()
        val cropsNeeded = linkedMapOf<CropDefinition, Int>()

        layout.slots.forEach { slot ->
            val wanted = slot.placedBlock ?: return@forEach
            val pos = grid.getPosForSlotCoords(slot.x, slot.y) ?: return@forEach

            if (compare(level, pos, wanted, layout.soilsAcceptedAt(slot), marks, ghosts)) return@forEach

            soilComplete = false
            if (needsPlacing(level, pos, wanted)) soilNeeded.merge(wanted.block, 1, Int::plus)
        }

        val soilPhase = if (soilComplete) Phase.Crops else Phase.Soil

        if (soilComplete) {
            layout.elementInstances.forEach { instance ->
                // every slot the plant would cover, by what covers each rather than what starts on
                // it: a two by two beginning one slot over still stands here
                val footprintSlots = buildList {
                    for (dx in 0 until instance.cropDef.footprint.width) {
                        for (dy in 0 until instance.cropDef.footprint.height) {
                            layout.getSlot(instance.slot.x + dx, instance.slot.y + dy)?.let { add(it) }
                        }
                    }
                }
                val growing = footprintSlots.mapNotNull { grid.elementCovering(it) }.distinct()

                // a target appears on its own, so nothing is planned for its slot and whatever grows
                // there is the watch pass's business rather than the building's
                if (instance.slot.slotMark == LayoutSlot.Marking.Target) return@forEach

                if (growing.isNotEmpty()) {
                    // the right plant in the right place, so there is nothing to plan and nothing in the way
                    val right = growing.singleOrNull()?.takeIf {
                        instance.accepts(it.instance.cropDef) &&
                                it.instance.slot.x == instance.slot.x && it.instance.slot.y == instance.slot.y
                    }
                    if (right != null) return@forEach

                    growing.forEach { markInTheWay(level, it, marks, badStands) }
                    return@forEach
                }

                val soil = grid.getPosForSlotCoords(instance.slot.x, instance.slot.y)
                    ?: return@forEach

                // a plant at a stage nobody has described matches nothing, and planning for its slot
                // as bare put a ghost inside it. Anything growing on the soil is a plant
                if (isOccupied(level, soil, instance.cropDef.footprint)) {
                    blocked.add(soil)
                    return@forEach
                }

                cropsNeeded.merge(instance.cropDef, 1, Int::plus)

                val stage = ghostStageOf(instance.cropDef) ?: return@forEach
                val render = stage.toRenderData(level, soil, instance.cropDef.footprint, instance.cropDef.standPoses, instance.cropDef.rotatesWithPlot)

                render.blockMap.forEach { (pos, state) ->
                    // the plant's own blocks, which have no second form the way its soil does
                    compare(level, pos, state, emptySet(), marks, ghosts)
                }

                // a crop in the same place at the same stage wants the same stands it already
                // has, so it keeps them. Only a crop that actually changed is built anew
                val key = "${instance.slot.x},${instance.slot.y}," +
                        "${instance.cropDef.name},${stage.stageRange}"

                standGroups[key] = previous.standGroups[key] ?: render.stands

                // a stand already standing in the crop's space is in the way of it
                level.getEntitiesOfClass(ArmorStand::class.java, instance.cropDef.footprint.spaceAbove(soil, CROP_HEIGHT))
                    .filter { EntityUtils.carriesAnything(it) }
                    .forEach { badStands.add(it.uuid) }
            }
        }

        val watchMarks = mutableMapOf<BlockPos, Pair<VoxelShape, PlannerMark>>()
        val watchStands = mutableMapOf<UUID, Int>()
        watchTargets(level, grid, layout, watchMarks, watchStands)

        val next = PlannerLayout(soilPhase, marks, ghosts, badStands, blocked, standGroups, watchMarks, watchStands)

        if (soilComplete) PlannerNeeds.tellPlants(grid, cropsNeeded)
        else PlannerNeeds.tellSoil(grid, soilNeeded)

        // a plan asking for what is already up is not a new plan: swapping it in handed the renderer
        // a fresh set of ghost stands for nothing
        if (next.plannerLayoutString == plannerLayout.plannerLayoutString) return

        // one swap, so nothing drawn is ever half of this plan and half of the last
        plannerLayout = next

        announceIfFinished(grid, layout, next)
    }

    /**
     * Sends the finished message once, when a plan first has nothing left to mark or ghost. A plan
     * that finishes again within half a minute is not announced twice.
     */
    private fun announceIfFinished(grid: GreenhouseGrid, layout: GreenhouseLayout, next: PlannerLayout) {
        if (grid.state.buildAnnounced) return

        // a crop skipped for a slot that reads as taken is not a crop that got planted
        val finished = next.marks.isEmpty() && next.ghosts.isEmpty() && next.badStands.isEmpty() &&
                next.blocked.isEmpty()
        val was = lastFinished

        lastFinished = finished

        if (!finished || was) return

        val now = Instant.now()
        if (lastAnnouncedAt?.let { now.isBefore(it.plus(ANNOUNCE_COOLDOWN)) } == true) return

        lastAnnouncedAt = now
        grid.state.buildAnnounced = true

        // the plan stays on the greenhouse after it is built: it is what the target marks are read from
        ChatUtils.sendWithPrefix(
            "${layout.displayName()} successfully built on ${grid.layout.displayName()}"
        )
    }

    /**
     * What the target slots of a running plan say: green on a target mutation ready to take, red on
     * anything else that grew in its footprint.
     */
    private fun watchTargets(
        level: Level,
        grid: GreenhouseGrid,
        layout: GreenhouseLayout,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        stands: MutableMap<UUID, Int>
    ) {
        if (!GreenhousePresets.harvestHighlightOn()) return

        layout.elementInstances
            .filter { it.slot.slotMark == LayoutSlot.Marking.Target }
            .forEach { target ->
                val footprint = target.cropDef.footprint
                val covering = buildList {
                    for (offsetX in 0 until footprint.width) {
                        for (offsetY in 0 until footprint.height) {
                            layout.getSlot(target.slot.x + offsetX, target.slot.y + offsetY)
                                ?.let { slot -> grid.elementCovering(slot)?.let { add(it) } }
                        }
                    }
                }.distinct()

                covering.forEach { growing ->
                    // the target itself is only worth saying something about once it can be taken
                    val mark = when {
                        !target.accepts(growing.instance.cropDef) -> PlannerMark.Blocking
                        harvestable(growing.instance) -> PlannerMark.Ready
                        else -> return@forEach
                    }

                    markPlant(level, growing, marks, mark).forEach { stands[it] = mark.color }
                    soilOf(grid, growing).forEach { marks[it] = FULL_BLOCK to mark }
                }
            }
    }

    /** Whether a mutation that appeared on a target slot has grown out; a one stage crop arrives grown. */
    private fun harvestable(plant: GreenhouseElementInstance): Boolean =
        plant.cropDef.isMutation && !plant.placed && (plant.highestStage ?: 0) >= plant.cropDef.maxStage

    /** The soil under a plant, so a crop made only of stands still has a box to pulse. */
    private fun soilOf(grid: GreenhouseGrid, growing: ElementRuntimeState): List<BlockPos> {
        val origin = growing.instance.slot
        val footprint = growing.instance.cropDef.footprint

        return buildList {
            for (offsetX in 0 until footprint.width) {
                for (offsetY in 0 until footprint.height) {
                    grid.getPosForSlotCoords(origin.x + offsetX, origin.y + offsetY)?.let { add(it) }
                }
            }
        }
    }

    /** Marks every block a plant is made of and hands back its stands, for the caller to tint. */
    private fun markPlant(
        level: Level,
        growing: ElementRuntimeState,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        mark: PlannerMark
    ): List<UUID> {
        growing.blocksMap?.keys?.forEach { pos ->
            marks[pos] = level.getBlockState(pos).getShape(level, pos) to mark
        }

        return growing.standEntities?.map { it.uuid } ?: emptyList()
    }

    /**
     * Marks a plant as being in the way rather than absent.
     *
     * Its blocks are outlined and its stands are tinted, the same as anything else standing where
     * a crop has to go, so the player is told to take it out rather than told nothing at all.
     */
    private fun markInTheWay(
        level: Level,
        growing: ElementRuntimeState,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        badStands: MutableSet<UUID>
    ) {
        growing.blocksMap?.keys?.forEach { pos ->
            marks[pos] = level.getBlockState(pos).getShape(level, pos) to PlannerMark.Wrong
        }

        growing.standEntities?.forEach { badStands.add(it.uuid) }
    }

    /**
     * Whether anything is growing on the soil a plan wants to fill. Any block or armor stand counts,
     * even one no definition matches, so a plan is never drawn through an existing plant.
     */
    private fun isOccupied(level: Level, soil: BlockPos, footprint: Footprint): Boolean {
        for (offsetX in 0 until footprint.width) {
            for (offsetY in 0 until footprint.height) {
                if (!level.getBlockState(soil.offset(offsetX, 1, offsetY)).isAir) return true
            }
        }

        // hypixel hangs a player's level and name off invisible stands that follow them about, and
        // one walking past a slot is not a plant standing in it
        return level.getEntitiesOfClass(ArmorStand::class.java, footprint.spaceAbove(soil, CROP_HEIGHT))
            .any { !it.isMarker && EntityUtils.carriesAnything(it) }
    }

    /**
     * Whether two states are the same as far as a plan cares. Farmland goes damp near water, and
     * calling that out would paint a finished greenhouse orange over nothing anyone can fix.
     */
    private fun BlockState.sameEnoughAs(other: BlockState): Boolean {
        if (this == other) return true
        if (block != other.block) return false

        if (block in STATE_IS_NOT_OURS) return true

        // every ignored property is copied across before comparing, so what is left is only the
        // state a player actually chose
        val normalised = IGNORED_PROPERTIES.fold(this) { state, property ->
            if (state.hasProperty(property) && other.hasProperty(property)) {
                state.setValue(property, other.getValue(property))
            } else {
                state
            }
        }

        return normalised == other
    }

    /**
     * Says what is wrong at [pos] given that [wanted] belongs there, filing it under the mark that
     * tells the player what to do about it. Returns whether the spot is already as it should be.
     */
    private fun compare(
        level: Level,
        pos: BlockPos,
        wanted: BlockState,
        accepted: Set<Block>,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        ghosts: MutableMap<BlockPos, BlockState>
    ): Boolean {
        val standing = level.getBlockState(pos)

        if (standing.sameEnoughAs(wanted)) return true

        // ground the plant would grow in anyway, which the preset simply did not happen to name:
        // a dead plant takes any of eight soils, and digging one out for another grows nothing new.
        // A cell the preset wants bare is still wanted bare
        if (!wanted.isAir && standing.block in accepted) return true

        // nothing belongs here, so anything standing here is in the way
        if (wanted.isAir) {
            if (standing.isAir) return true

            marks[pos] = standing.getShape(level, pos) to PlannerMark.Remove
            return false
        }

        if (standing.isAir) {
            ghosts[pos] = wanted
            return false
        }

        // the same block in the wrong state, or ground that only wants working on, is not a block
        // to dig out and replace
        val adjustable = standing.block == wanted.block ||
                (standing.block in TILLABLE && wanted.block in TILLABLE)

        marks[pos] = standing.getShape(level, pos) to if (adjustable) PlannerMark.Adjust else PlannerMark.Wrong
        return false
    }

    /** Whether the slot wants a block the player has to bring, rather than one to till or work on. */
    private fun needsPlacing(level: Level, pos: BlockPos, wanted: BlockState): Boolean {
        if (wanted.isAir) return false

        val standing = level.getBlockState(pos)
        if (standing.isAir) return true

        return standing.block != wanted.block &&
                !(standing.block in TILLABLE && wanted.block in TILLABLE)
    }

    /**
     * What a crop looks like when put down: a base crop starts at its first stage, a mutation is
     * placed already grown. A crop with no stage recorded is skipped and named once.
     */
    private fun ghostStageOf(definition: CropDefinition): CropStage? {
        val at = definition.stagePlacedAt
        val candidates = definition.stages.filter { at in it.stageRange }
        val stage = candidates.firstOrNull { it.placed } ?: candidates.firstOrNull()

        if (stage == null && reportedMissingStage.add(definition.name)) {
            ChatUtils.sendWithPrefix("No stage $at described for ${definition.name}, skipping it.")
        }

        return stage
    }
}
