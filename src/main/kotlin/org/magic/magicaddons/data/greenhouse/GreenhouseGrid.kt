package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.util.getBuildableArea
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import java.time.Instant

/** The y every greenhouse plants on, the grid is a single flat row of soil. */
const val GREENHOUSE_SOIL_Y: Int = 73

/** A greenhouse is ten by ten. */
const val GREENHOUSE_SIZE: Int = 10

/** How far above the soil a plant reaches: its stands are read and whatever is in its way is found within this. */
const val CROP_HEIGHT: Int = 5

class GreenhouseGrid(
    var state: GridState,
    var layout: GreenhouseLayout
) {
    var plot: Plot? = null
    val width = GREENHOUSE_SIZE
    val height = GREENHOUSE_SIZE

    val elements = mutableListOf<ElementRuntimeState>()

    fun hasRuntime(): Boolean {
        return state.hasRuntimeReferences
    }

    fun getPosForSlot(slot: LayoutSlot): BlockPos? {
        val box = plot?.getBuildableArea() ?: return null

        val minX = box.minX.toInt()
        val minZ = box.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, GREENHOUSE_SOIL_Y, worldZ)
    }

    /** the quarter turn of [wanted] matching most of what stands here */
    fun bestTurnFor(wanted: GreenhouseLayout): Int =
        (0 until 4).maxBy { turns -> agreement(wanted.turned(turns)) }

    /** how many of [wanted]'s soil blocks and plants are in place */
    private fun agreement(wanted: GreenhouseLayout): Int {
        val soil = wanted.slots.count { slot ->
            val wantedBlock = slot.placedBlock?.block ?: return@count false
            wantedBlock == layout.getSlot(slot.x, slot.y)?.placedBlock?.block
        }
        val plants = wanted.elementInstances.count { instance ->
            elements.any {
                it.instance.cropDef == instance.cropDef &&
                        it.instance.slot.x == instance.slot.x && it.instance.slot.y == instance.slot.y
            }
        }

        return soil + plants
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


    /** The element standing on a slot, including a big crop covering it from its corner. */
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
                BlockPos(worldX, GREENHOUSE_SOIL_Y, worldZ)
            )

            layout.getSlot(gridX, gridY)?.let {
                it.placedBlock = state
            }
        }
    }


    /** The slots a change at [positions] can have reached: every slot within a crop's width of each. */
    fun regionAround(positions: Collection<BlockPos>): Set<Pair<Int, Int>> {
        val reach = CropRegistry.all.maxOf { maxOf(it.footprint.width, it.footprint.height) } - 1
        val region = mutableSetOf<Pair<Int, Int>>()
        positions.forEach { pos ->
            val slot = getSlotAt(pos, matchY = false) ?: return@forEach
            for (x in (slot.x - reach).coerceAtLeast(0)..(slot.x + reach).coerceAtMost(width - 1)) {
                for (y in (slot.y - reach).coerceAtLeast(0)..(slot.y + reach).coerceAtMost(height - 1)) {
                    region.add(x to y)
                }
            }
        }
        return region
    }

    /** Whether any slot of [element]'s footprint lies in [region]. */
    private fun touches(element: ElementRuntimeState, region: Set<Pair<Int, Int>>): Boolean {
        val origin = element.instance.slot
        val footprint = element.instance.cropDef.footprint
        for (dx in 0 until footprint.width) {
            for (dy in 0 until footprint.height) {
                if ((origin.x + dx) to (origin.y + dy) in region) return true
            }
        }
        return false
    }

    /** [region] grown to hold the whole footprint of every plant it touches, so a big crop's corner is read with the rest. */
    private fun wholePlants(region: Set<Pair<Int, Int>>): Set<Pair<Int, Int>> {
        val grown = region.toMutableSet()
        elements.filter { touches(it, region) }.forEach { element ->
            val origin = element.instance.slot
            val footprint = element.instance.cropDef.footprint
            for (dx in 0 until footprint.width) {
                for (dy in 0 until footprint.height) grown.add((origin.x + dx) to (origin.y + dy))
            }
        }
        return grown
    }

    /**
     * Reads the plot and brings the elements into line with it. A merge, not a rebuild: a plant
     * still in its slot keeps its age, water and confirmed stage.
     *
     * Given a [region], only its slots are read again and the plants outside it stay as they are.
     */
    fun setPlantData(touchedRegion: Set<Pair<Int, Int>>? = null) {
        val region = touchedRegion?.let { wholePlants(it) }
        val visitedSlots = Array(width) { BooleanArray(height) }

        val level = Minecraft.getInstance().level ?: return
        val buildableArea = plot?.getBuildableArea() ?: return

        // a greenhouse is full of plot marker stands that hold nothing and belong to no plant, so a
        // crop stand described without a skull would bind to one
        val remainingStands = level.getEntitiesOfClass(ArmorStand::class.java, buildableArea)
            .filterNot { it.isMarker }
            .toMutableList()

        // from the instances, not the runtime wrappers: the wrappers are rebuilt every scan, while
        // the instances carry the age, water and stage, and are what goes to disk
        val previous = layout.elementInstances.associateBy { it.slot.x to it.slot.y }
        val readings = CropStage.StandReadings()
        val reconciled = mutableListOf<ElementRuntimeState>()

        // outside the region nothing is read again: those plants, their slots and their stands are taken as they are
        if (region != null) {
            elements.filterNot { touches(it, region) }.forEach { kept ->
                reconciled.add(kept)
                remainingStands.removeAll((kept.standEntities ?: emptyList()).toSet())
                val origin = kept.instance.slot
                val footprint = kept.instance.cropDef.footprint
                for (dx in 0 until footprint.width) {
                    for (dy in 0 until footprint.height) {
                        if (origin.x + dx < width && origin.y + dy < height) visitedSlots[origin.y + dy][origin.x + dx] = true
                    }
                }
            }
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (visitedSlots[y][x]) continue
                if (region != null && x to y !in region) continue

                val slot = layout.getSlot(x, y) ?: continue

                val found = findElementAtSlot(slot, remainingStands, readings) ?: continue

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
                    carryOver(standing, found)
                } else {
                    if (standing == null && def.isMutation && state.lastUpdateTimestamp != null) {
                        // only a plant the player was seen putting down counts as placed; anything
                        // else where nothing stood at the last look grew there on its own
                        val placedNow = callbacks.takePlacement(def, found.instance.slot, this)
                        if (found.instance.placed || placedNow) {
                            callbacks.claimPlacedPlant(found.instance)
                        } else {
                            callbacks.claimSpawnedMutation(found.instance, layout)
                            capToTicksSinceLook(found.instance)
                        }
                    }
                    found
                }

                reconciled.add(runtime)
            }
        }

        elements.clear()
        elements.addAll(reconciled)

        layout.elementInstances.clear()
        layout.elementInstances.addAll(reconciled.map { it.instance })
    }

    /**
     * Narrows a plant that spawned since the last look: it started at stage one then, so it cannot
     * be further along than the ticks that have passed. One tick means it is still at stage one.
     */
    private fun capToTicksSinceLook(instance: GreenhouseElementInstance) {
        val ticks = state.pendingGrowthTicks
        if (ticks <= 0) return

        val range = (instance.growthStage as? GrowthStageInfo.Estimated)?.range ?: return
        val last = range.last.coerceAtMost(ticks)
        if (last < range.first) return

        instance.growthStage =
            if (range.first == last) GrowthStageInfo.Known(last) else GrowthStageInfo.Estimated(range.first..last)
    }

    /**
     * The plant the world just described, wearing what was known about the one already standing
     * there: the world knows the crop and its stage, not when it was planted or what it holds.
     */
    private fun carryOver(
        standing: GreenhouseElementInstance,
        found: ElementRuntimeState
    ): ElementRuntimeState {
        found.instance.age = standing.age

        // the plant is the one that was standing here, so it keeps the stage it was first seen at
        // rather than the one this scan happens to find it at
        found.instance.firstSeenStage = standing.firstSeenStage ?: found.instance.lowestStage
        found.instance.placed = standing.placed

        // predicted past death and still standing means ticks were skipped. The fewest that leave
        // it alive put it one tick from dying, so that is assumed and said out loud
        val water = standing.waterLevel

        if (water != null && water <= WaterModel.DEATH && found.instance.needsWater) {
            found.instance.waterLevel =
                WaterModel.aliveFloor(water, layout.waterEffectAt(found.instance.slot))
            found.instance.waterPredictedInDebt = true

            callbacks.warnSurvivor(
                DyingPlant(found.instance.cropDef.name, layout.displayName(), layout.id)
            )
        } else {
            found.instance.waterLevel = water
            found.instance.waterPredictedInDebt = standing.waterPredictedInDebt
            found.instance.waterExact = standing.waterExact
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
     * Moves every plant on by that many ticks, for a greenhouse nobody is standing in. Always an
     * estimate: a plant in water debt may not have advanced at all. Water model: notes/water-formula.md.
     */
    fun predictGrowth(ticks: Int, tickMs: Long) {
        if (ticks <= 0) return

        // the plants themselves, not the runtime wrappers: a wrapper only exists while the plot is
        // loaded, which is never true of the greenhouse this is for
        advance(layout.elementInstances, layout, ticks, tickMs)
    }

    /**
     * The plot as it would stand after that many more ticks, built on plants of its own so nothing
     * real is moved on.
     */
    fun predictedLayout(ticks: Int, tickMs: Long): GreenhouseLayout {
        val preview = GreenhouseLayout(id = layout.id, name = layout.name, size = layout.size)

        preview.slots.forEach { slot ->
            val theirs = layout.getSlot(slot.x, slot.y)
            slot.placedBlock = theirs?.placedBlock
            slot.slotMark = theirs?.slotMark
        }

        layout.elementInstances.forEach { instance ->
            val slot = preview.getSlot(instance.slot.x, instance.slot.y) ?: return@forEach
            preview.elementInstances.add(instance.copyForPrediction(slot))
        }

        advance(preview.elementInstances, preview, ticks, tickMs)

        return preview
    }

    private fun advance(
        instances: List<GreenhouseElementInstance>,
        layout: GreenhouseLayout,
        ticks: Int,
        tickMs: Long
    ) {
        if (ticks <= 0) return

        val gardenTime = timeOfDayNow()

        instances.forEach { instance ->
            val maxStage = instance.cropDef.maxStage

            // a finished plant stops drinking, so no water is taken off one. Judged by the lowest
            // stage it might be at, so a plant only probably grown keeps drying
            val lowestStage = instance.lowestStage

            if (lowestStage != null && lowestStage >= maxStage) {
                instance.age = instance.age?.plus(ticks * tickMs)
                return@forEach
            }

            // a sleeping snoozling, a noctilume craving the other time of day and a starved fleshtrap
            // are all stuck, and all still dry out: being stuck is not being spared
            val cravingUnfulfilled = instance.cravesOtherTime(gardenTime)

            // a plant in debt may be passed over entirely and nothing here can know, so the loss is
            // counted anyway and the plant remembers that it is a worst case
            val inDebt = (instance.waterLevel ?: 0) < 0
            if (inDebt) instance.waterPredictedInDebt = true

            if (instance.isAsleep || cravingUnfulfilled || instance.isStarving) {
                if (instance.needsWater) {
                    instance.waterLevel = instance.waterLevel?.let {
                        WaterModel.after(it, ticks, layout.waterEffectAt(instance.slot))
                    }
                }

                return@forEach
            }

            instance.age = instance.age?.plus(ticks * tickMs)

            // a plant stops drinking once it has grown out, so only the ticks it spends growing take
            // water off it. In debt the low end never moves, so it is given every tick
            val stagesLeft = lowestStage?.let { (maxStage - it).coerceAtLeast(0) } ?: ticks
            val drinkingTicks = if (inDebt) ticks else ticks.coerceAtMost(stagesLeft)

            if (instance.needsWater) {
                instance.waterLevel = instance.waterLevel?.let {
                    WaterModel.after(it, drinkingTicks, layout.waterEffectAt(instance.slot))
                }
            }

            val range = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> stage.stage..stage.stage
                is GrowthStageInfo.Estimated -> stage.range
                null -> return@forEach
            }

            // a snoozling drops asleep on arriving at a sleep stage, so the ticks after it were never
            // served and it must not be walked past
            val sleepStages = instance.cropDef.sleepStages

            fun ceiling(from: Int): Int =
                sleepStages.filter { it > from }.minOrNull()?.coerceAtMost(maxStage) ?: maxStage

            // in debt every tick may have been skipped, so the low end stays where it was while the
            // high end takes every tick: a 3 becomes 3 to 4
            val first = if (inDebt) range.first else (range.first + ticks).coerceAtMost(ceiling(range.first))
            val last = (range.last + ticks).coerceAtMost(ceiling(range.last))

            // both ends landing on the same stage leaves nothing to estimate
            instance.growthStage =
                if (first == last) GrowthStageInfo.Known(first) else GrowthStageInfo.Estimated(first..last)

            // judged by the lowest it might be at, so a plant only probably asleep is still called
            // awake: the warning for one that has stopped growing is worth being sure about
            if (first in sleepStages) instance.readings[CropStandReader.ASLEEP] = 1
        }
    }

    companion object {

        /** Who a scan reports placements and survivors to; the feature installs itself here. */
        var callbacks: GridCallbacks = GridCallbacks.None

        /** The garden clock as a craving value: its custom time reaches the client as world time. */
        fun timeOfDayNow(): Int {
            val time = (Minecraft.getInstance().level?.overworldClockTime ?: 0L) % 24000L

            return if (time in 13000L..22999L) {
                CropStandReader.CRAVES_NIGHT
            } else {
                CropStandReader.CRAVES_DAY
            }
        }

        /**
         * The plant standing at a position, or null when nothing described matches. The soil is
         * taken as given, since the scan and the exporter know it different ways.
         */
        fun findElementAt(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot,
            readings: CropStage.StandReadings = CropStage.StandReadings()
        ): ElementRuntimeState? {
            val candidates = CropRegistry.elementsBySoil[soil] ?: return null

            var bestDef: CropDefinition? = null
            var bestGrowth: GrowthStageInfo? = null
            var bestStage: CropStage? = null
            var bestScore = -1
            var bestUsedStands: List<Entity>? = null
            var bestBlocks: Map<BlockPos, BlockState>? = null
            var bestLegacy = false

            for (candidate in candidates) {
                for (stage in candidate.stages) {
                    val result = stage.matchesStage(origin, remainingStands, candidate.footprint, candidate.rotatesWithPlot, readings = readings)

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

            val definition = bestDef ?: return placedWithoutLook(origin, soil, remainingStands, slot)

            // the dex cannot see from the data which stages predate normalized exports, so it
            // learns from every match that only got there through the rotation fallback
            if (bestLegacy) bestStage?.let { PlantDex.noteLegacy(definition.name, it.stageRange) }

            val instance = GreenhouseElementInstance(
                definition.elementId,
                slot = slot,
                growthStage = bestGrowth,
                cropDef = definition
            )

            // where this plant enters our records, overwritten in the reconcile by whatever the
            // plant already standing here carried
            instance.firstSeenStage = instance.lowestStage

            // matched through its placed look, so it was put down, whoever remembers it or not;
            // and the memory of putting it down has served its purpose
            if (bestStage?.placed == true) instance.placed = true
            callbacks.forgetPlacementAt(origin)

            // what winning this stage implies, filed before the stand readings: a noctilume's craving
            // is carried by which skull matched
            bestStage?.traits?.let { instance.readings.putAll(it) }

            // read after matching and from every stand around the plant: a hunger bar belongs to the
            // plant without being part of what makes it that plant
            bestStage?.read(standsAround(origin, definition.footprint))
                ?.let { instance.readings.putAll(it) }

            return ElementRuntimeState(
                instance = instance,
                standEntities = bestUsedStands,
                blocksMap = bestBlocks,
                rotationLegacy = bestLegacy
            )
        }

        /**
         * The crop the player put down here this session, when nothing recorded matches what stands
         * on the soil: taken as that crop at its placed stage, flagged placed, with whatever stands
         * and blocks sit in its footprint. Without this a placed mutation with no placed look
         * recorded was invisible, and the collector could never learn it was placed.
         */
        private fun placedWithoutLook(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot
        ): ElementRuntimeState? {
            val definition = callbacks.placedHereAt(origin) ?: return null
            if (soil !in definition.requiredSoil) return null

            val level = Minecraft.getInstance().level ?: return null
            val footprint = definition.footprint
            val stands = standsAround(origin, footprint).filter { it in remainingStands }
            val blocks = mutableMapOf<BlockPos, BlockState>()
            for (dx in 0 until footprint.width) {
                for (dz in 0 until footprint.height) {
                    for (dy in 1..CROP_HEIGHT) {
                        val pos = origin.offset(dx, dy, dz)
                        val state = level.getBlockState(pos)
                        if (!state.isAir) blocks[pos] = state
                    }
                }
            }
            if (stands.isEmpty() && blocks.isEmpty()) {
                callbacks.forgetPlacementAt(origin)
                return null
            }

            val instance = GreenhouseElementInstance(
                definition.elementId,
                slot = slot,
                growthStage = GrowthStageInfo.Known(definition.stagePlacedAt),
                cropDef = definition
            )
            instance.placed = true
            instance.firstSeenStage = definition.stagePlacedAt

            return ElementRuntimeState(instance = instance, standEntities = stands, blocksMap = blocks)
        }

        /** Every stand sharing the space a crop of [footprint] occupies from [origin]. */
        private fun standsAround(origin: BlockPos, footprint: Footprint): List<ArmorStand> {
            val level = Minecraft.getInstance().level ?: return emptyList()

            return level.getEntitiesOfClass(ArmorStand::class.java, footprint.spaceAbove(origin, CROP_HEIGHT))
                .filterNot { it.isMarker }
        }
    }

    /** The plant standing on [slot], through the one matcher. */
    fun findElementAtSlot(
        slot: LayoutSlot,
        remainingStands: MutableList<ArmorStand>,
        readings: CropStage.StandReadings = CropStage.StandReadings()
    ): ElementRuntimeState? {
        val soil = slot.placedBlock?.block ?: return null
        val origin = getPosForSlot(slot) ?: return null

        return findElementAt(origin, soil, remainingStands, slot, readings)
    }

    data class GridState(
        var lastUpdateTimestamp: Instant? = null,
        var needsUpdate: Boolean = false,
        var assignedLayout: GreenhouseLayout? = null,
        var hasRuntimeReferences: Boolean = false,
        var pendingGrowthTicks: Int = 0,
        /** Whether this greenhouse has already said its plan was built. */
        var buildAnnounced: Boolean = false,
        /** quarter turns the assigned layout is laid with */
        var planTurns: Int = 0
    ) {
        /** The assigned layout's id as read from disk, resolved to the layout once the presets are loaded too. */
        var assignedLayoutId: String? = null
    }

    override fun toString(): String = layout.displayName()
}
