package org.magic.magicaddons.features.farming.greenhousePresets.render

import com.mojang.blaze3d.vertex.PoseStack
import java.time.Duration
import java.time.Instant
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.ScannedPlant
import org.magic.magicaddons.data.greenhouse.plot.CROP_HEIGHT
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.PlannerNeeds
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.render.WorldRenderer
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId

object LayoutRenderState {

    private val FULL_BLOCK: VoxelShape = Shapes.block()

    private const val PULSE_ALPHA_LOW: Int = 0x28
    private const val PULSE_ALPHA_HIGH: Int = 0x70

    private const val FILL_ALPHA: Int = 0x4D

    private fun ghostAlpha(): Int = GreenhousePresets.plantAlpha()

    private const val GHOST_TINT: Int = 0xFFB8CCFF.toInt()

    @JvmStatic
    fun ghostOutlineColorOf(stand: UUID): Int {
        val held = heldCrop()

        return if (held != null && plannerLayout.standCrops[stand] == held) PlannerMark.InHand.color else PlannerMark.Missing.color
    }

    private fun heldCrop(): String? =
        Minecraft.getInstance().player?.mainHandItem?.getSkyBlockId()?.id?.let { CropRegistry.findByIdOrName(it)?.elementId }

    private val TILLABLE: Set<Block> = setOf(
        Blocks.DIRT,
        Blocks.GRASS_BLOCK,
        Blocks.COARSE_DIRT,
        Blocks.ROOTED_DIRT,
        Blocks.DIRT_PATH,
        Blocks.FARMLAND
    )

    const val OBSTRUCTION_TINT: Int = 0x90FF0000.toInt()

    enum class Phase {
        Soil,
        Crops
    }

    private class PlannerLayout(
        val phase: Phase,
        val marks: Map<BlockPos, Pair<VoxelShape, PlannerMark>>,
        val ghosts: Map<BlockPos, BlockState>,
        val badStands: Set<UUID>,
        val obstructedSoils: Set<BlockPos>,
        val ghostStandsByPlant: Map<String, List<ArmorStand>>,

        val watchMarks: Map<BlockPos, Pair<VoxelShape, PlannerMark>> = emptyMap(),
        val watchStands: Map<UUID, Int> = emptyMap(),
        val ghostCrops: Map<BlockPos, String> = emptyMap(),
        val standCrops: Map<UUID, String> = emptyMap()
    ) {
        val ghostStands: List<ArmorStand> = ghostStandsByPlant.values.flatten()

        val comparisonKey: String = buildString {
            append(phase).append('|')
            marks.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value.second).append(',') }
            append('|')
            ghosts.entries.sortedBy { it.key.asLong() }
                .forEach { append(it.key.asLong()).append(':').append(it.value).append(',') }
            append('|')
            badStands.map { it.toString() }.sorted().forEach { append(it).append(',') }
            append('|')
            obstructedSoils.map { it.asLong() }.sorted().forEach { append(it).append(',') }
            append('|')
            ghostStandsByPlant.keys.sorted().forEach { append(it).append(';') }
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
        if (!GreenhouseData.inOwnGarden()) return 0

        val current = plannerLayout

        return when {
            stand in current.badStands -> OBSTRUCTION_TINT
            else -> current.watchStands[stand] ?: 0
        }
    }

    val ghostStands: List<ArmorStand> get() = if (GreenhouseData.inOwnGarden()) plannerLayout.ghostStands else emptyList()

    val hasSomethingToShow: Boolean
        get() = plannerLayout.marks.isNotEmpty() || plannerLayout.ghosts.isNotEmpty() || plannerLayout.badStands.isNotEmpty() || plannerLayout.obstructedSoils.isNotEmpty()

    private var lastFinished: Boolean = false

    private var lastAnnouncedAt: Instant? = null

    private val ANNOUNCE_COOLDOWN: Duration = Duration.ofSeconds(30)

    private val reportedMissingStage = mutableSetOf<String>()

    fun submitPlan(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        if (!GreenhouseData.inOwnGarden()) return

        val plan = this.plannerLayout
        if (plan.marks.isEmpty() && plan.ghosts.isEmpty() && plan.watchMarks.isEmpty()) return

        val presetBatch = WorldRenderer.BlockRenderBatch(cameraPos)
        val pulse = WorldRenderer.pulsedAlpha(PULSE_ALPHA_LOW, PULSE_ALPHA_HIGH)

        plan.marks.forEach { (pos, mark) -> presetBatch.fillWithOutline(pos, mark.first, mark.second.color, FILL_ALPHA) }
        plan.watchMarks.forEach { (pos, mark) -> presetBatch.fillWithOutline(pos, mark.first, mark.second.color, pulse) }
        val held = heldCrop()
        plan.ghosts.forEach { (pos, state) ->
            val mark = if (held != null && plan.ghostCrops[pos] == held) PlannerMark.InHand else PlannerMark.Missing
            presetBatch.ghostBlockWithOutline(pos, state, GHOST_TINT, mark.color, ghostAlpha())
        }
        presetBatch.submitBatch(poseStack, collector)
    }

    fun show() {
        reportedMissingStage.clear()

        refresh()
    }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        refresh()
    }

    fun refresh() {
        if (!GreenhouseData.inOwnGarden()) {
            plannerLayout = PlannerLayout.NOTHING
            return
        }

        val grid = GreenhouseData.getCurrentGrid()

        PlannerNeeds.arriveAt(grid)

        if (grid == null) return

        val level = Minecraft.getInstance().level ?: return

        val assigned = grid.state.assignedLayout

        if (assigned == null) {
            reportedMissingStage.clear()

            val watchMarks = mutableMapOf<BlockPos, Pair<VoxelShape, PlannerMark>>()
            val watchStands = mutableMapOf<UUID, Int>()
            watchHarvestable(level, grid, watchMarks, watchStands)

            plannerLayout = PlannerLayout(Phase.Soil, emptyMap(), emptyMap(), emptySet(), emptySet(), emptyMap(), watchMarks, watchStands)
            return
        }

        val layout = assigned.turnedBy(grid.state.planTurns)

        val marks = mutableMapOf<BlockPos, Pair<VoxelShape, PlannerMark>>()
        val ghosts = mutableMapOf<BlockPos, BlockState>()
        val ghostCrops = mutableMapOf<BlockPos, String>()
        val standCrops = mutableMapOf<UUID, String>()
        val badStands = mutableSetOf<UUID>()
        val obstructedSoils = mutableSetOf<BlockPos>()
        val ghostStandsByPlant = mutableMapOf<String, List<ArmorStand>>()

        val previous = plannerLayout

        var soilComplete = true
        val soilNeeded = linkedMapOf<Block, Int>()
        val cropsNeeded = linkedMapOf<CropDefinition, Int>()

        layout.slots.forEach { slot ->
            val wantedSoil = slot.soil ?: return@forEach
            val pos = grid.getPosForSlotCoords(slot.x, slot.y) ?: return@forEach
            val wantedState = wantedSoil.defaultBlockState()

            if (markBlockDifference(level, pos, wantedState, layout.soilsPlantAcceptsAt(slot), marks, ghosts)) return@forEach

            soilComplete = false
            if (needsPlacing(level, pos, wantedState)) soilNeeded.merge(wantedSoil, 1, Int::plus)
        }

        val phase = if (soilComplete) Phase.Crops else Phase.Soil

        if (soilComplete) {
            layout.plants.forEach { layoutPlant ->
                val footprintSlots = buildList {
                    for (dx in 0 until layoutPlant.cropDef.footprint.width) {
                        for (dy in 0 until layoutPlant.cropDef.footprint.height) {
                            layout.getSlot(layoutPlant.slot.x + dx, layoutPlant.slot.y + dy)?.let { add(it) }
                        }
                    }
                }
                val growing = footprintSlots.mapNotNull { grid.elementCoveringSlot(it) }.distinct()

                val target = layoutPlant.slot.mark == LayoutSlot.Marking.Target

                if (growing.isNotEmpty()) {
                    val right = growing.singleOrNull()?.takeIf {
                        layoutPlant.acceptsCrop(it.plant.cropDef) &&
                                it.plant.slot.x == layoutPlant.slot.x && it.plant.slot.y == layoutPlant.slot.y
                    }
                    if (right != null) return@forEach

                    growing.forEach { markInTheWay(level, it, marks, badStands) }
                    return@forEach
                }

                val soil = grid.getPosForSlotCoords(layoutPlant.slot.x, layoutPlant.slot.y)
                    ?: return@forEach

                if (markObstructions(level, soil, layoutPlant.cropDef.footprint, marks, badStands)) {
                    obstructedSoils.add(soil)
                    return@forEach
                }
                if (target) return@forEach

                cropsNeeded.merge(layoutPlant.cropDef, 1, Int::plus)

                val stage = ghostStageOf(layoutPlant.cropDef) ?: return@forEach
                val render = stage.hologramStageAt(level, soil, layoutPlant.cropDef)

                render.blockMap.forEach { (pos, state) ->
                    markBlockDifference(level, pos, state, emptySet(), marks, ghosts)
                    if (pos in ghosts) ghostCrops[pos] = layoutPlant.cropDef.elementId
                }

                val key = "${layoutPlant.slot.x},${layoutPlant.slot.y}," +
                        "${layoutPlant.cropDef.name},${stage.stageRange}"

                ghostStandsByPlant[key] = previous.ghostStandsByPlant[key] ?: render.stands
                ghostStandsByPlant[key]?.forEach { standCrops[it.uuid] = layoutPlant.cropDef.elementId }

                val space = layoutPlant.cropDef.footprint.spaceAbove(soil, CROP_HEIGHT)
                level.getEntitiesOfClass(ArmorStand::class.java, space)
                    .filter { space.contains(it.position()) && EntityUtils.carriesAnything(it) }
                    .forEach { badStands.add(it.uuid) }
            }
        }

        val watchMarks = mutableMapOf<BlockPos, Pair<VoxelShape, PlannerMark>>()
        val watchStands = mutableMapOf<UUID, Int>()
        if (GreenhousePresets.harvestHighlightOnlyTargets()) watchTargets(level, grid, layout, watchMarks, watchStands)
        else watchHarvestable(level, grid, watchMarks, watchStands)

        val next = PlannerLayout(phase, marks, ghosts, badStands, obstructedSoils, ghostStandsByPlant, watchMarks, watchStands, ghostCrops, standCrops)

        if (soilComplete) PlannerNeeds.tellPlants(grid, cropsNeeded)
        else PlannerNeeds.tellSoil(grid, soilNeeded)

        if (next.comparisonKey == plannerLayout.comparisonKey) return

        plannerLayout = next

        announceIfFinished(grid, layout, next, cropsNeeded.isEmpty())
    }

    private fun announceIfFinished(grid: GreenhouseGrid, layout: PlotLayout, next: PlannerLayout, nothingToPlace: Boolean) {
        if (grid.state.buildAnnounced) return

        val finished = nothingToPlace && next.marks.isEmpty() && next.ghosts.isEmpty() &&
                next.badStands.isEmpty() && next.obstructedSoils.isEmpty()
        val was = lastFinished

        lastFinished = finished

        if (!finished || was) return

        val now = Instant.now()
        if (lastAnnouncedAt?.let { now.isBefore(it.plus(ANNOUNCE_COOLDOWN)) } == true) return

        lastAnnouncedAt = now
        grid.state.buildAnnounced = true

        ChatUtils.sendWithPrefix(
            "${layout.displayName()} successfully built on ${grid.layout.displayName()}"
        )
    }

    private fun watchHarvestable(
        level: Level,
        grid: GreenhouseGrid,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        stands: MutableMap<UUID, Int>
    ) {
        if (!GreenhousePresets.harvestHighlightOn()) return

        grid.scannedPlants
            .filter { GreenhousePresets.isHarvestable(it.plant) }
            .forEach { growing -> markReady(grid, growing, marks) }
    }

    private fun markReady(grid: GreenhouseGrid, growing: ScannedPlant, marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>) {
        val soil = grid.getPosForSlot(growing.plant.slot) ?: return
        val footprint = growing.plant.cropDef.footprint
        val box = Shapes.create(AABB(0.0, 0.0, 0.0, footprint.width.toDouble(), 1.0, footprint.height.toDouble()))

        marks[soil] = box to PlannerMark.Ready
    }

    private fun watchTargets(
        level: Level,
        grid: GreenhouseGrid,
        layout: PlotLayout,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        stands: MutableMap<UUID, Int>
    ) {
        if (!GreenhousePresets.harvestHighlightOn()) return

        layout.plants
            .filter { it.slot.mark == LayoutSlot.Marking.Target }
            .forEach { target ->
                val footprint = target.cropDef.footprint
                val covering = buildList {
                    for (offsetX in 0 until footprint.width) {
                        for (offsetY in 0 until footprint.height) {
                            layout.getSlot(target.slot.x + offsetX, target.slot.y + offsetY)
                                ?.let { slot -> grid.elementCoveringSlot(slot)?.let { add(it) } }
                        }
                    }
                }.distinct()

                covering.forEach { growing ->
                    when {
                        !target.acceptsCrop(growing.plant.cropDef) -> {
                            markPlant(level, growing, marks, PlannerMark.Blocking).forEach { stands[it] = PlannerMark.Blocking.color }
                            soilOf(grid, growing).forEach { marks[it] = FULL_BLOCK to PlannerMark.Blocking }
                        }
                        GreenhousePresets.isHarvestable(growing.plant) -> markReady(grid, growing, marks)
                    }
                }
            }
    }

    private fun soilOf(grid: GreenhouseGrid, growing: ScannedPlant): List<BlockPos> {
        val origin = growing.plant.slot
        val footprint = growing.plant.cropDef.footprint

        return buildList {
            for (offsetX in 0 until footprint.width) {
                for (offsetY in 0 until footprint.height) {
                    grid.getPosForSlotCoords(origin.x + offsetX, origin.y + offsetY)?.let { add(it) }
                }
            }
        }
    }

    private fun markPlant(
        level: Level,
        growing: ScannedPlant,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        mark: PlannerMark
    ): List<UUID> {
        growing.blocks?.keys?.forEach { pos ->
            marks[pos] = level.getBlockState(pos).getShape(level, pos) to mark
        }

        return growing.stands?.map { it.uuid } ?: emptyList()
    }

    private fun markInTheWay(
        level: Level,
        plants: ScannedPlant,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        badStands: MutableSet<UUID>
    ) {
        plants.blocks?.keys?.forEach { pos ->
            marks[pos] = level.getBlockState(pos).getShape(level, pos) to PlannerMark.Wrong
        }

        plants.stands?.forEach { badStands.add(it.uuid) }
    }

    private fun markObstructions(
        level: Level,
        soil: BlockPos,
        footprint: Footprint,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        badStands: MutableSet<UUID>
    ): Boolean {
        var found = false

        for (offsetX in 0 until footprint.width) {
            for (offsetY in 0 until footprint.height) {
                for (offsetUp in 1..CROP_HEIGHT) {
                    val pos = soil.offset(offsetX, offsetUp, offsetY)
                    val state = level.getBlockState(pos)
                    if (state.isAir) continue

                    marks[pos] = state.getShape(level, pos) to PlannerMark.Wrong
                    found = true
                }
            }
        }

        val space = footprint.spaceAbove(soil, CROP_HEIGHT)
        level.getEntitiesOfClass(ArmorStand::class.java, space)
            .filter { space.contains(it.position()) && !it.isMarker && (!it.isInvisible || EntityUtils.carriesAnything(it)) }
            .forEach {
                badStands.add(it.uuid)
                found = true
            }

        return found
    }

    private fun markBlockDifference(
        level: Level,
        pos: BlockPos,
        wanted: BlockState,
        accepted: Set<Block>,
        marks: MutableMap<BlockPos, Pair<VoxelShape, PlannerMark>>,
        ghosts: MutableMap<BlockPos, BlockState>
    ): Boolean {
        val standing = level.getBlockState(pos)

        if (standing.block == wanted.block) return true

        if (!wanted.isAir && standing.block in accepted) return true

        if (wanted.isAir) {
            if (standing.isAir) return true

            marks[pos] = standing.getShape(level, pos) to PlannerMark.Remove
            return false
        }

        if (standing.isAir) {
            ghosts[pos] = wanted
            return false
        }

        val adjustable = standing.block == wanted.block ||
                (standing.block in TILLABLE && wanted.block in TILLABLE)

        marks[pos] = standing.getShape(level, pos) to if (adjustable) PlannerMark.Adjust else PlannerMark.Wrong
        return false
    }

    private fun needsPlacing(level: Level, pos: BlockPos, wanted: BlockState): Boolean {
        if (wanted.isAir) return false

        val standing = level.getBlockState(pos)
        if (standing.isAir) return true

        return standing.block != wanted.block &&
                !(standing.block in TILLABLE && wanted.block in TILLABLE)
    }

    private fun ghostStageOf(definition: CropDefinition): CropStage? {
        val at = definition.stagePlacedAt
        val stage = definition.stages.firstOrNull { at in it.stageRange }

        if (stage == null && reportedMissingStage.add(definition.name)) {
            ChatUtils.sendWithPrefix("No stage $at described for ${definition.name}, skipping it.")
        }

        return stage
    }
}
