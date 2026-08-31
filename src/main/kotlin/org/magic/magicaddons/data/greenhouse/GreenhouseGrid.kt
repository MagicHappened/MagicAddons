package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.util.getBuildableArea
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.elementsBySoil
import org.magic.magicaddons.features.farming.greenhousePresets.DyingPlant
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import java.time.Instant

/** The y every greenhouse plants on, the grid is a single flat row of soil. */
const val GREENHOUSE_SOIL_Y: Int = 73

class GreenhouseGrid(
    var state: GridState,
    var layout: GreenhouseLayout
) {
    var plot: Plot? = null
    val width = 10
    val height = 10

    val elements = mutableListOf<ElementRuntimeState>()

    fun addElement(element: ElementRuntimeState, age: Long? = null) {
        if (age != null)
            element.instance.age = age
        layout.elementInstances += element.instance
        elements.add(element)
    }

    fun hasRuntime(): Boolean {
        return state.hasRuntimeReferences
    }
    fun slotEquals(slot: LayoutSlot): Boolean {
        return layout.getSlot(slot.x,slot.y)?.placedBlock == slot.placedBlock
    }

    fun getPosForSlot(slot: LayoutSlot): BlockPos? {
        val box = plot?.getBuildableArea() ?: return null

        val minX = box.minX.toInt()
        val minZ = box.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, GREENHOUSE_SOIL_Y, worldZ)
    }

    fun getPosForSlotCoords(x: Int, y: Int): BlockPos? {
        layout.getSlot(x,y)?.let {
            return getPosForSlot(it)
        }
        return null
    }

    fun getSlotAt(blockPos: BlockPos, matchY: Boolean = true): LayoutSlot? {
        val buildArea = plot?.getBuildableArea() ?: return null

        if (!buildArea.contains(Vec3.atCenterOf(blockPos))) return null

        if (matchY && blockPos.y != GREENHOUSE_SOIL_Y) return null

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        val gridX = blockPos.x - minX
        val gridY = blockPos.z - minZ

        return layout.getSlot(gridX, gridY)
    }


    /**
     * The element standing on [slot], including a crop bigger than one slot that covers it from its
     * top left corner, which is the only slot such a crop is filed under.
     */
    fun elementCovering(slot: LayoutSlot): ElementRuntimeState? = elements.find { element ->
        val origin = element.instance.slot
        val footprint = element.instance.cropDef.footprint

        slot.x in origin.x until origin.x + footprint.width &&
                slot.y in origin.y until origin.y + footprint.height
    }

    fun removeMatchingBlock(blockPos: BlockPos): ElementRuntimeState? {
        return elements.find { element ->
            element.blocksMap
                ?.keys
                ?.any { it == blockPos }
                ?: false
        }?.also {
            elements.remove(it)
            layout.elementInstances.remove(it.instance)
        }
    }

    fun removeMatchingEntity(entity: Entity): ElementRuntimeState? {
        return elements.find { element ->
            element.standEntities?.any { it == entity } ?: false
        }?.also {
            elements.remove(it)
            layout.elementInstances.remove(it.instance)
        }
    }

    fun createSlotData(): List<LayoutSlot>? {
        val world = Minecraft.getInstance().level ?: return null
        val plot = PlotAPI.getCurrentPlot() ?: return null
        if (plot != this.plot) return null

        val buildArea = plot.getBuildableArea()

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        return List(width * height) { index ->

            val gridX = index % width
            val gridY = index / width

            val worldX = minX + gridX
            val worldZ = minZ + gridY

            val state = world.getBlockState(
                BlockPos(worldX, 73, worldZ)
            )

            LayoutSlot(
                gridX,
                gridY,
                state
            )
        }
    }

    fun createSlotDataForGrid() {
        val world = Minecraft.getInstance().level ?: return
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (plot != this.plot) return

        val buildArea = plot.getBuildableArea()

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        for (index in 0 until (width * height)) {
            val gridX = index % width
            val gridY = index / width

            val worldX = minX + gridX
            val worldZ = minZ + gridY

            val state = world.getBlockState(
                BlockPos(worldX, 73, worldZ)
            )

            layout.getSlot(gridX, gridY)?.let {
                it.placedBlock = state
            }
        }
    }


    //todo need to make this also set plant specific data, aka if candidate "Fleshtrap" was found
    // already handling it need to see the when statement working for fleshtrap once someone grows some
    // (or me)
    /**
     * Reads the plot and brings [elements] into line with it.
     *
     * A merge rather than a rebuild: a plant the world still shows in the same slot keeps the
     * instance already held for it, so the age it was planted at, the water level read off its bar
     * and a growth stage a plant diagnostic confirmed all survive. Only what the world disagrees
     * about is written over. Reading the plot is the only way to learn that a plant was removed,
     * replaced or grown, and none of those should cost the plants around them their history.
     *
     * Returns what changed, for a caller that wants to say so.
     */
    fun setPlantData(): ReconcileResult {
        val visitedSlots = Array(width) { BooleanArray(height) }

        val level = Minecraft.getInstance().level ?: return ReconcileResult()
        val buildableArea = plot?.getBuildableArea() ?: return ReconcileResult()

        // a greenhouse is full of marker stands laid out in columns across the whole plot. They
        // hold nothing, occupy nothing, and belong to the plot rather than to any plant, so a crop
        // stand described without a skull to look for would happily bind to one
        val remainingStands = level.getEntitiesOfClass(ArmorStand::class.java, buildableArea)
            .filterNot { it.isMarker }
            .toMutableList()

        // taken from the instances rather than from the runtime wrappers around them. The wrappers
        // hold entities and blocks, which only mean anything while the plot is loaded, and are
        // rebuilt from nothing every time the grid is. The instances are what carry the age, the
        // water and the stage, and what is written to disk, so they are what a plant is remembered
        // as between one look at the plot and the next.
        val previous = layout.elementInstances.associateBy { it.slot.x to it.slot.y }
        val reconciled = mutableListOf<ElementRuntimeState>()
        val result = ReconcileResult()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (visitedSlots[y][x]) continue

                val slot = layout.getSlot(x, y) ?: continue

                val found = findElementAtSlot(slot, remainingStands) ?: continue
                //todo insert code here that catches certain types of mutations:
                // aka Fleshtraps and other stuff that might arise, and add
                // catching of hunger and bonus data.
                // fuck i forgot it needs to be saved to disk fucking fleshtrap grr

                val def = found.instance.cropDef

                remainingStands.removeAll((found.standEntities ?: emptyList()).toSet())

                if (x + def.footprint.width > width ||
                    y + def.footprint.height > height
                ) continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }

                val standing = previous[x to y]
                val runtime = if (standing != null && standing.elementId == found.instance.elementId) {
                    result.kept++
                    carryOver(standing, found)
                } else {
                    if (standing != null) result.replaced++ else result.added++
                    found
                }

                reconciled.add(runtime)
            }
        }

        result.removed = previous.count { (key, _) ->
            reconciled.none { it.instance.slot.x to it.instance.slot.y == key }
        }

        elements.clear()
        elements.addAll(reconciled)

        layout.elementInstances.clear()
        layout.elementInstances.addAll(reconciled.map { it.instance })

        return result
    }

    /**
     * The plant the world just described, wearing what was already known about the one standing
     * there. The world is right about which crop it is, where its blocks and stands are, and how
     * grown it looks; it says nothing about when it was planted or how much water it holds.
     */
    private fun carryOver(
        standing: GreenhouseElementInstance,
        found: ElementRuntimeState
    ): ElementRuntimeState {
        found.instance.age = standing.age

        // the plant is the one that was standing here, so it keeps the stage it was first seen at
        // rather than the one this scan happens to find it at
        found.instance.firstSeenStage = standing.firstSeenStage ?: found.instance.lowestStage

        // the prediction may have drained this plant past death, and here it stands: ticks were
        // skipped. The fewest skips that leave it alive put it one tick from dying, so that is
        // what is assumed, and said out loud, since watered now is the only way it stays standing
        val water = standing.waterLevel

        if (water != null && water <= WaterModel.DEATH && found.instance.cropDef.needsWater) {
            found.instance.waterLevel =
                WaterModel.aliveFloor(water, layout.waterEffectAt(found.instance.slot))
            found.instance.waterPredictedInDebt = true

            GreenhouseData.warnSurvivor(
                DyingPlant(found.instance.cropDef.name, layout.displayName(), layout.id)
            )
        } else {
            found.instance.waterLevel = water
            found.instance.waterPredictedInDebt = standing.waterPredictedInDebt
        }

        // a diagnostic pins a stage down to one number, a scan often cannot, so a reading already
        // taken is not thrown away for a guess that covers it
        val standingStage = standing.growthStage
        val foundStage = found.instance.growthStage

        if (standingStage is GrowthStageInfo.Known && foundStage is GrowthStageInfo.Estimated &&
            standingStage.stage in foundStage.range
        ) {
            found.instance.growthStage = standingStage
        }

        return found
    }

    /**
     * Moves every plant on by [ticks] growth ticks, for a greenhouse nobody is standing in.
     *
     * The result is always an estimate, even for a plant whose stage was known: nothing has been
     * looked at, this is only what the clock says should have happened. A plant already at its last
     * stage stays there. [tickMs] is how long one growth tick takes, so a plant also ages by the
     * time that passed and its decay keeps counting down.
     *
     * Water is dried out at the measured rate, taking whatever stands beside the plant into
     * account. See WaterModel and notes/water-formula.md. The wiki puts it as:
     *
     * > After each growth stage, a crop loses between 2-3 Water Level, which can be mitigated by
     * > the +50% Water Retain and +100% Improved Water Retain effects, and amplified by the -30%
     * > Water Drain negative effect respectively.
     * >
     * > If a crop has negative Water Level during a Growth Stage, it has a chance not to advance to
     * > the next stage.
     *
     * So even unbuffed the loss is a range rather than a number, and a plant in debt may not have
     * advanced at all, which is why the stage this writes is only ever an estimate.
     */
    /**
     * The garden clock as a craving value. The garden's customisable time reaches the client as
     * ordinary world time, so day and night are the vanilla windows of it.
     */
    private fun timeOfDayNow(): Int {
        val time = (Minecraft.getInstance().level?.overworldClockTime ?: 0L) % 24000L

        return if (time in 13000L..22999L) CropStandReader.CRAVES_NIGHT else CropStandReader.CRAVES_DAY
    }

    fun predictGrowth(ticks: Int, tickMs: Long) {
        if (ticks <= 0) return

        // the plants themselves, not the runtime wrappers around them. A wrapper holds entities
        // and blocks and so only exists while the plot is loaded, which is never the case for the
        // greenhouse this is for: one nobody is standing in. Working through the wrappers meant a
        // greenhouse away from the player had no plants to move on at all, so the clock advanced
        // and nothing else did
        layout.elementInstances.forEach { instance ->
            val maxStage = instance.cropDef.maxStage

            // a plant that has finished growing stops drinking: the game shows no countdown on a
            // fully grown plant, so the model takes no water off one. Judged by the lowest stage
            // it might be at, so a plant only probably grown keeps drying, worst case as ever.
            // Untested edge, to be watched for in game: the tick that completes a plant still
            // drains it here, so finishing at exactly -100 counts as dead until a scan says
            val lowestStage = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> stage.stage
                is GrowthStageInfo.Estimated -> stage.range.first
                null -> null
            }

            if (lowestStage != null && lowestStage >= maxStage) {
                instance.age = instance.age?.plus(ticks * tickMs)
                return@forEach
            }

            // a snoozling that has dropped asleep stays where it is until someone wakes it, a
            // noctilume craving the other time of day is stuck until the garden's clock comes
            // around, and a fleshtrap that has run its hunger out is stuck until it is fed. The
            // ticks pass all three by, and all three still dry out, since being stuck is not the
            // same as being spared
            val cravingUnfulfilled = instance.craving?.let { it != timeOfDayNow() } == true

            // a plant already in debt may be passed over entirely, taking neither its stage nor
            // its water, and nothing here can know which happened. The loss is counted anyway, so
            // what is shown is the worst it could be in, and the plant remembers that it is
            if ((instance.waterLevel ?: 0) < 0) instance.waterPredictedInDebt = true

            if (instance.isAsleep || cravingUnfulfilled || instance.isStarving) {
                if (instance.cropDef.needsWater) {
                    instance.waterLevel = instance.waterLevel?.let {
                        WaterModel.after(it, ticks, layout.waterEffectAt(instance.slot))
                    }
                }

                return@forEach
            }

            instance.age = instance.age?.plus(ticks * tickMs)

            if (instance.cropDef.needsWater) {
                instance.waterLevel = instance.waterLevel?.let {
                    WaterModel.after(it, ticks, layout.waterEffectAt(instance.slot))
                }
            }

            val range = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> stage.stage..stage.stage
                is GrowthStageInfo.Estimated -> stage.range
                null -> return@forEach
            }

            instance.growthStage = GrowthStageInfo.Estimated(
                (range.first + ticks).coerceAtMost(maxStage)..(range.last + ticks).coerceAtMost(maxStage)
            )
        }
    }

    /** What one reconcile did, counted by what happened to each plant. */
    data class ReconcileResult(
        var added: Int = 0,
        var removed: Int = 0,
        var replaced: Int = 0,
        var kept: Int = 0
    ) {
        val changed: Boolean get() = added > 0 || removed > 0 || replaced > 0
    }

    // temp for testing
    companion object {
        /**
         * The plant standing at [origin], or null when nothing described matches what is there.
         *
         * The one implementation. The scan knows the slot it is looking at and the exporter only
         * knows a position, so the soil is taken as given rather than looked up two different ways,
         * and [slot] is passed only so a matched plant can be filed against it.
         */
        fun findElementAt(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot
        ): ElementRuntimeState? {
            val candidates = elementsBySoil[soil] ?: return null

            var bestDef: CropDefinition? = null
            var bestGrowth: GrowthStageInfo? = null
            var bestStage: CropStage? = null
            var bestScore = -1
            var bestUsedStands: List<Entity>? = null
            var bestBlocks: Map<BlockPos, BlockState>? = null
            var bestLegacy = false

            for (candidate in candidates) {
                val stages = candidate.stageDefs.flatMap {
                    when (it) {
                        is CropStagePattern -> it.expand()
                        is CropStage -> listOf(it)
                    }
                }

                for (stage in stages) {
                    val result = stage.matchesStage(origin, remainingStands, candidate.footprint)

                    if (!result.matched) continue
                    if (result.score <= bestScore) continue

                    bestScore = result.score
                    bestDef = candidate
                    bestStage = stage
                    bestUsedStands = result.usedStands
                    bestBlocks = result.matchedBlocks
                    bestLegacy = result.rotationLegacy

                    val range = stage.stageRange
                    bestGrowth = if (range.first == range.last) {
                        GrowthStageInfo.Known(range.first)
                    } else {
                        GrowthStageInfo.Estimated(range)
                    }
                }
            }

            val definition = bestDef ?: return null

            // the dex cannot see from the data which stages predate normalized exports, so it
            // learns from every match that only got there through the rotation fallback
            if (bestLegacy) bestStage?.let { PlantDex.noteLegacy(definition.name, it.stageRange) }

            val instance = GreenhouseElementInstance(
                definition.skyblockId?.id ?: definition.name,
                slot = slot,
                growthStage = bestGrowth,
                cropDef = definition
            )

            // where this plant enters our records. Overwritten by whatever the plant standing here
            // already carried, in the reconcile that follows, so only a plant nobody has seen
            // before keeps the stage read here
            instance.firstSeenStage = instance.lowestStage

            // what winning this stage implies about the plant, filed before the stand readings:
            // a noctilume's craving is carried by which skull matched, not by anything a stand
            // will say afterwards
            bestStage?.traits?.let { instance.readings.putAll(it) }

            // read after matching, never during it, and from every stand around the plant rather
            // than only the ones the stage claimed: a hunger bar belongs to the plant without
            // being part of what makes it that plant
            bestStage?.read(standsAround(origin, definition.footprint))
                ?.let { instance.readings.putAll(it) }

            return ElementRuntimeState(
                instance = instance,
                standEntities = bestUsedStands,
                blocksMap = bestBlocks,
                rotationLegacy = bestLegacy
            )
        }

        /** Every stand sharing the space a crop of [footprint] occupies from [origin]. */
        private fun standsAround(origin: BlockPos, footprint: Footprint): List<ArmorStand> {
            val level = Minecraft.getInstance().level ?: return emptyList()

            val box = AABB(
                origin.x.toDouble(),
                origin.y.toDouble(),
                origin.z.toDouble(),
                (origin.x + footprint.width).toDouble(),
                (origin.y + READER_HEIGHT).toDouble(),
                (origin.z + footprint.height).toDouble()
            )

            return level.getEntitiesOfClass(ArmorStand::class.java, box).filterNot { it.isMarker }
        }

        /** How far above the soil a plant may hang something worth reading. */
        private const val READER_HEIGHT: Int = 5

    }

    /** The plant standing on [slot], through the one matcher. */
    fun findElementAtSlot(
        slot: LayoutSlot,
        remainingStands: MutableList<ArmorStand>
    ): ElementRuntimeState? {
        val soil = slot.placedBlock?.block ?: return null
        val origin = getPosForSlot(slot) ?: return null

        return findElementAt(origin, soil, remainingStands, slot)
    }

    fun getUnassignedBlockMap(): Map<BlockPos, BlockState> {
        val level = Minecraft.getInstance().level ?: return emptyMap()
        val area = plot?.getBuildableArea() ?: return emptyMap()

        val allBlocks = mutableMapOf<BlockPos, BlockState>()

        val minX = area.minX.toInt()
        val minZ = area.minZ.toInt()
        val maxX = area.maxX.toInt()
        val maxZ = area.maxZ.toInt()

        val minY = 74
        val maxY = 84

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val pos = BlockPos(x, y, z)
                    val state = level.getBlockState(pos)

                    if (!state.isAir) {
                        allBlocks[pos] = state
                    }
                }
            }
        }

        val usedPositions = elements
            .flatMap { it.blocksMap?.keys ?: emptySet() }
            .toSet()

        return allBlocks.filterKeys { it !in usedPositions }
    }

    fun getUnassignedArmorStands(): List<ArmorStand>? {
        val level = Minecraft.getInstance().level ?: return null
        val area = plot?.getBuildableArea() ?: return null
        val stands = level.getEntities(null, area)
            .filterIsInstance<ArmorStand>()
            .filterNot { it.isMarker }
            .toMutableList()
        elements.forEach {
            it.standEntities?.let { elements -> stands.removeAll(elements.toSet()) }
        }
        return stands.toList()
    }

    data class GridState(
        var lastUpdateTimestamp: Instant? = null,
        var needsUpdate: Boolean = false,
        var assignedLayout: GreenhouseLayout? = null,
        var hasRuntimeReferences: Boolean = false,
        var pendingGrowthTicks: Int? = null,
        /**
         * Whether the player has said they do not want to be told this plan is finished. Kept per
         * grid rather than per plan, since it is this greenhouse they are tired of hearing about.
         */
        var completionMuted: Boolean = false
    )

    override fun toString(): String {
        return "${layout.name ?: "unnamed"}: ${layout.id}"
    }
}