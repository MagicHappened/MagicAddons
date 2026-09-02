package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Instant
import java.time.Duration
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ClickEvent
import net.minecraft.ChatFormatting
import java.util.UUID
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.Level
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.Footprint
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.FarmlandBlock
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.VoxelShape
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.render.WorldRender
import org.magic.magicaddons.util.ChatUtils

/**
 * Shows the player how to build a layout: the soil row first, then the crops on it, each drawn at
 * its first stage. Missing blocks are ghosted, blocks in the way are outlined.
 */
object LayoutRenderState {
    init {
        EventBus.register(this)
    }

    /**
     * What a marked block is told to do: red swap it, orange work on it, purple remove it, blue
     * place it.
     */
    private enum class Mark(val color: Int) {
        Wrong(0xFFFF3333.toInt()),
        Adjust(0xFFFF9922.toInt()),
        Remove(0xFFAA44EE.toInt()),
        Missing(0xFF3399FF.toInt())
    }

    /** Enough colour to read the mark through, little enough to see the block under it. */
    private const val FILL_ALPHA: Int = 0x4D

    /** Far more solid than a mark's fill: a ghost is the block's texture tinted and then faded. */
    private const val GHOST_ALPHA: Int = 0xC0

    /** Pale on purpose: the tint multiplies the texture, and a saturated one drains the block's colour. */
    private const val GHOST_TINT: Int = 0xFFB8CCFF.toInt()

    /** The glow around a ghosted head, drawn as itself rather than multiplied over a texture. */
    const val GHOST_OUTLINE_COLOR: Int = 0xFF3399FF.toInt()

    /** Ground a hoe turns into other ground: untilled dirt is not the wrong block. */
    /** State a plan does not care about, because nothing the player does decides it. */
    private val IGNORED_PROPERTIES: List<IntegerProperty> = listOf(FarmlandBlock.MOISTURE)

    /**
     * Blocks whose state is the world's business: fire keeps an age and a face per neighbour, both
     * changing on their own, which made a correctly placed fire flicker between right and wrong.
     */
    private val STATE_IS_NOT_OURS: Set<Block> = setOf(Blocks.FIRE)

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
     * Everything the plan says, held as one object: a frame drawn mid-rescan would otherwise catch
     * the new answer to one question beside the old answer to the next.
     */
    private class Plan(
        val phase: Phase,
        val marks: Map<BlockPos, Pair<VoxelShape, Mark>>,
        val ghosts: Map<BlockPos, BlockState>,
        val badStands: Set<UUID>,
        /**
         * Ghost stands kept apart by crop, so an unchanged crop keeps its stands. Rebuilding an
         * entity is not the same to a renderer as leaving it alone.
         */
        val standGroups: Map<String, List<ArmorStand>>
    ) {
        val ghostStands: List<ArmorStand> = standGroups.values.flatten()

        /** What this plan asks for, as one string, so two plans compare without comparing stands. */
        val signature: String = buildString {
            append(phase).append('|')
            marks.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value.second).append(',') }
            append('|')
            ghosts.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value).append(',') }
            append('|')
            badStands.map { it.toString() }.sorted().forEach { append(it).append(',') }
            append('|')
            standGroups.keys.sorted().forEach { append(it).append(';') }
        }

        companion object {
            val NOTHING = Plan(Phase.Soil, emptyMap(), emptyMap(), emptySet(), emptyMap())
        }
    }

    @Volatile
    private var plan: Plan = Plan.NOTHING


    /** Which half of the job the player is on. */
    val phase: Phase get() = plan.phase

    /** Stands in the way of a crop. Tinted rather than outlined, since entities draw one at a time. */
    val badStandsUUID: Set<UUID> get() = plan.badStands

    /** The stands a ghosted crop is made of, drawn as part of showing what to plant. */
    val ghostStands: List<ArmorStand> get() = plan.ghostStands

    /** Whether the plan was finished the last time it was worked out. */
    private var lastFinished: Boolean = false

    /** When the player was last told, so finishing twice quickly is only said once. */
    private var announcedAt: Instant? = null

    /** How long after saying it the plan holds its tongue, however often it is finished again. */
    private val ANNOUNCE_COOLDOWN: Duration = Duration.ofSeconds(30)

    /** Crops with no first stage described yet, so each is only ever mentioned once. */
    private val reportedMissingStage = mutableSetOf<String>()

    /** Draws the plan from the frame's own render pass, against the camera that frame uses. */
    fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val plan = this.plan
        val marks = plan.marks
        val ghosts = plan.ghosts
        marks.forEach { (pos, mark) ->
            WorldRender.mark(
                poseStack, collector, cameraPos, pos, mark.first, mark.second.color, FILL_ALPHA
            )
        }

        ghosts.forEach { (pos, state) ->
            WorldRender.ghost(
                poseStack, collector, cameraPos, pos, state,
                GHOST_TINT, Mark.Missing.color, GHOST_ALPHA
            )
        }
    }

    /** Starts over on the plan of whichever greenhouse the player is in. */
    fun show() {
        // the phase rides in the plan now, and refresh works it out from the plot rather than
        // being told, so starting over is forgetting what has been said and looking again
        reportedMissingStage.clear()

        refresh()
    }

    fun hide() {
        plan = Plan.NOTHING
        reportedMissingStage.clear()
    }

    /** Works out what to draw from what the plot holds now. Cheap enough to run on every change. */
    fun refresh() {
        val grid = GreenhouseData.getCurrentGrid()

        // losing sight of the greenhouse is not the same as having nothing to draw, so the last
        // plan stays up rather than blinking out on every unreadable moment
        if (grid == null) return

        val level = Minecraft.getInstance().level ?: return

        // the plan belongs to this greenhouse, so standing in another shows that one's plan or
        // nothing rather than carrying the last one around the garden
        val layout = grid.state.assignedLayout

        if (layout == null) {
            hide()
            return
        }

        val marks = mutableMapOf<BlockPos, Pair<VoxelShape, Mark>>()
        val ghosts = mutableMapOf<BlockPos, BlockState>()
        val badStands = mutableSetOf<UUID>()
        val standGroups = mutableMapOf<String, List<ArmorStand>>()

        // what is already up, to take the unchanged parts of it over rather than build them again
        val previous = plan

        var soilComplete = true

        layout.slots.forEach { slot ->
            val wanted = slot.placedBlock ?: return@forEach
            val pos = grid.getPosForSlotCoords(slot.x, slot.y) ?: return@forEach

            if (!compare(level, pos, wanted, marks, ghosts)) soilComplete = false
        }

        val soilPhase = if (soilComplete) Phase.Crops else Phase.Soil

        if (soilComplete) {
            layout.elementInstances.forEach { instance ->
                // by what covers the slot rather than what starts on it: a two by two beginning one
                // slot over still stands here
                val growing = grid.elementCovering(instance.slot)

                // the target plant appears on its own once the ingredients are right, so nothing is
                // planned for it, but it needs an empty slot to appear on
                if (instance.slot.slotMark == LayoutSlot.Marking.Target) {
                    if (growing != null) markInTheWay(level, growing, marks, badStands)
                    return@forEach
                }

                if (growing != null) {
                    // the right plant, so there is nothing to plan and nothing in the way of it
                    if (growing.instance.cropDef == instance.cropDef) return@forEach

                    markInTheWay(level, growing, marks, badStands)
                    return@forEach
                }

                val soil = grid.getPosForSlotCoords(instance.slot.x, instance.slot.y)
                    ?: return@forEach

                // a plant at a stage nobody has described matches nothing, and planning for its slot
                // as bare put a ghost inside it. Anything growing on the soil is a plant
                if (isOccupied(level, soil, instance.cropDef.footprint)) return@forEach

                val stage = ghostStageOf(instance.cropDef) ?: return@forEach
                val render = stage.toRenderData(level, soil, instance.cropDef.footprint, instance.cropDef.standPoses)

                render.blockMap.forEach { (pos, state) ->
                    compare(level, pos, state, marks, ghosts)
                }

                // a crop in the same place at the same stage wants the same stands it already
                // has, so it keeps them. Only a crop that actually changed is built anew
                val key = "${instance.slot.x},${instance.slot.y}," +
                        "${instance.cropDef.name},${stage.stageRange}"

                standGroups[key] = previous.standGroups[key] ?: render.stands

                // a stand already standing in the crop's space is in the way of it
                val footprint = instance.cropDef.footprint
                val space = AABB(
                    soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
                    (soil.x + footprint.width).toDouble(),
                    (soil.y + CROP_HEIGHT).toDouble(),
                    (soil.z + footprint.height).toDouble()
                )

                level.getEntitiesOfClass(ArmorStand::class.java, space)
                    .forEach { badStands.add(it.uuid) }
            }
        }

        val next = Plan(soilPhase, marks, ghosts, badStands, standGroups)

        // a plan asking for what is already up is not a new plan: swapping it in handed the renderer
        // a fresh set of ghost stands for nothing
        if (next.signature == plan.signature) return

        // one swap, so nothing drawn is ever half of this plan and half of the last
        plan = next

        announceIfFinished(grid, layout, next)
    }

    /**
     * Sends the finished message once, when a plan first has nothing left to mark or ghost. A plan
     * that finishes again within half a minute is not announced twice.
     */
    private fun announceIfFinished(grid: GreenhouseGrid, layout: GreenhouseLayout, next: Plan) {
        if (grid.state.completionMuted) return

        val finished = next.marks.isEmpty() && next.ghosts.isEmpty() && next.badStands.isEmpty()
        val was = lastFinished

        lastFinished = finished

        if (!finished || was) return

        val now = Instant.now()
        if (announcedAt?.let { now.isBefore(it.plus(ANNOUNCE_COOLDOWN)) } == true) return

        announcedAt = now

        ChatUtils.sendWithPrefix(
            "${layout.displayName()} successfully built on ${grid.layout.displayName()}"
        )
        ChatUtils.send(
            Component.literal("  Turn the planner off for this greenhouse? ")
                .withStyle(ChatFormatting.GRAY)
                .append(answer("YES", ChatFormatting.GREEN, "unplan"))
                .append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                .append(answer("NO", ChatFormatting.RED, "keepPlanner"))
        )
    }

    /** One of the two answers, as a word the player clicks rather than a command they type. */
    private fun answer(word: String, color: ChatFormatting, command: String): Component =
        Component.literal(word).withStyle(
            Style.EMPTY
                .withColor(color)
                .withClickEvent(ClickEvent.RunCommand("/MagicAddons internal $command"))
                .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to answer $word")))
        )

    /**
     * Marks a plant as being in the way rather than absent.
     *
     * Its blocks are outlined and its stands are tinted, the same as anything else standing where
     * a crop has to go, so the player is told to take it out rather than told nothing at all.
     */
    private fun markInTheWay(
        level: Level,
        growing: ElementRuntimeState,
        marks: MutableMap<BlockPos, Pair<VoxelShape, Mark>>,
        badStands: MutableSet<UUID>
    ) {
        growing.blocksMap?.keys?.forEach { pos ->
            marks[pos] = level.getBlockState(pos).getShape(level, pos) to Mark.Wrong
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

        val space = AABB(
            soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
            (soil.x + footprint.width).toDouble(),
            (soil.y + CROP_HEIGHT).toDouble(),
            (soil.z + footprint.height).toDouble()
        )

        return level.getEntitiesOfClass(ArmorStand::class.java, space).any { !it.isMarker }
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
        marks: MutableMap<BlockPos, Pair<VoxelShape, Mark>>,
        ghosts: MutableMap<BlockPos, BlockState>
    ): Boolean {
        val standing = level.getBlockState(pos)

        if (standing.sameEnoughAs(wanted)) return true

        // nothing belongs here, so anything standing here is in the way
        if (wanted.isAir) {
            if (standing.isAir) return true

            marks[pos] = standing.getShape(level, pos) to Mark.Remove
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

        marks[pos] = standing.getShape(level, pos) to if (adjustable) Mark.Adjust else Mark.Wrong
        return false
    }

    /**
     * What a crop looks like when put down: a base crop starts at its first stage, a mutation is
     * placed already grown. A crop with no stage recorded is skipped and named once.
     */
    private fun ghostStageOf(definition: CropDefinition): CropStage? {
        val stages = definition.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }

        val stage = if (definition.isMutation) {
            stages.filter { definition.maxStage in it.stageRange }
                .maxByOrNull { it.stageRange.last }
        } else {
            stages.filter { 1 in it.stageRange }
                .minByOrNull { it.stageRange.first }
        }

        if (stage == null && reportedMissingStage.add(definition.name)) {
            val which = if (definition.isMutation) "last" else "first"

            ChatUtils.sendWithPrefix("No $which stage described for ${definition.name}, skipping it.")
        }

        return stage
    }

    /** How far above the soil a crop can reach, for finding what is standing in its way. */
    private const val CROP_HEIGHT: Int = 5
}
