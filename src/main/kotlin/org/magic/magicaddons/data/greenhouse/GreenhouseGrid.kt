package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.data.greenhouse.elements.DeadPlant
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

const val GREENHOUSE_SOIL_Y: Int = 73
const val GREENHOUSE_SIZE: Int = 10

/** blocks above the soil searched for a plant's stands and blocks */
const val CROP_HEIGHT: Int = 5

const val HUNGER_LOSS_PER_TICK: Int = 20

fun ticksUntilHungry(hunger: Int): Int = (hunger + HUNGER_LOSS_PER_TICK - 1) / HUNGER_LOSS_PER_TICK

class GreenhouseGrid(
    var state: GridState,
    var layout: GreenhouseLayout
) {
    var plot: Plot? = null
    val width = GREENHOUSE_SIZE
    val height = GREENHOUSE_SIZE

    val scannedPlants = mutableListOf<ScannedPlant>()

    fun isScannedThisVisit(): Boolean {
        return state.scannedThisVisit
    }

    fun getPosForSlot(slot: LayoutSlot): BlockPos? {
        val buildArea = plot?.getBuildableArea() ?: return null

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, GREENHOUSE_SOIL_Y, worldZ)
    }

    fun bestTurnFor(plan: GreenhouseLayout): Int =
        (0 until 4).maxBy { turns -> agreementWith(plan.turned(turns)) }

    /** keeps [currentTurns] unless another turn agrees strictly better */
    fun bestTurnKeeping(plan: GreenhouseLayout, currentTurns: Int): Int {
        val bestTurns = bestTurnFor(plan)

        return if (agreementWith(plan.turned(bestTurns)) > agreementWith(plan.turned(currentTurns))) bestTurns else currentTurns
    }

    /** plants compare before soil */
    class Agreement(val plants: Int, val soil: Int) : Comparable<Agreement> {
        override fun compareTo(other: Agreement): Int =
            compareValuesBy(this, other, { it.plants }, { it.soil })

        override fun toString(): String = "$plants plants, $soil soil"
    }

    fun agreementWith(plan: GreenhouseLayout): Agreement {
        val soil = plan.slots.count { plannedSlot ->
            val plannedSoil = plannedSlot.soil?.block ?: return@count false
            plannedSoil == layout.getSlot(plannedSlot.x, plannedSlot.y)?.soil?.block
        }
        val plants = plan.plants.count { plannedPlant ->
            scannedPlants.any {
                it.plant.cropDef == plannedPlant.cropDef &&
                        it.plant.slot.x == plannedPlant.slot.x && it.plant.slot.y == plannedPlant.slot.y
            }
        }

        return Agreement(plants, soil)
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


    fun elementCoveringSlot(slot: LayoutSlot): ScannedPlant? = scannedPlants.find { scannedPlant ->
        val origin = scannedPlant.plant.slot
        val footprint = scannedPlant.plant.cropDef.footprint

        slot.x in origin.x until origin.x + footprint.width &&
                slot.y in origin.y until origin.y + footprint.height
    }

    fun removePlantWithBlockAt(blockPos: BlockPos): ScannedPlant? {
        return scannedPlants.find { scannedPlant ->
            scannedPlant.blocks
                ?.keys
                ?.any { it == blockPos }
                ?: false
        }?.also {
            scannedPlants.remove(it)
            layout.plants.remove(it.plant)
        }
    }

    fun readSoilBlocks() {
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
                it.soil = state
            }
        }
    }


    /** every slot within a crop's width of each position */
    fun scanSlotsReachedFrom(positions: Collection<BlockPos>): Set<Pair<Int, Int>> {
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

    private fun footprintOverlaps(scannedPlant: ScannedPlant, region: Set<Pair<Int, Int>>): Boolean {
        val origin = scannedPlant.plant.slot
        val footprint = scannedPlant.plant.cropDef.footprint
        for (dx in 0 until footprint.width) {
            for (dy in 0 until footprint.height) {
                if ((origin.x + dx) to (origin.y + dy) in region) return true
            }
        }
        return false
    }

    private fun withWholeFootprints(region: Set<Pair<Int, Int>>): Set<Pair<Int, Int>> {
        val grown = region.toMutableSet()
        scannedPlants.filter { footprintOverlaps(it, region) }.forEach { scannedPlant ->
            val origin = scannedPlant.plant.slot
            val footprint = scannedPlant.plant.cropDef.footprint
            for (dx in 0 until footprint.width) {
                for (dy in 0 until footprint.height) grown.add((origin.x + dx) to (origin.y + dy))
            }
        }
        return grown
    }

    fun rescanPlants(onlySlots: Set<Pair<Int, Int>>? = null): Boolean {
        val region = onlySlots?.let { withWholeFootprints(it) }
        val visitedSlots = Array(width) { BooleanArray(height) }

        val level = Minecraft.getInstance().level ?: return false
        val buildableArea = plot?.getBuildableArea() ?: return false

        // ignore marker stands
        val remainingStands = level.getEntitiesOfClass(ArmorStand::class.java, buildableArea)
            .filterNot { it.isMarker }
            .toMutableList()

        // the instances carry age, water and stage; the wrappers are rebuilt each scan
        val previousBySlot = layout.plants.associateBy { it.slot.x to it.slot.y }
        val standCache = CropStage.StandCache()
        val merged = mutableListOf<ScannedPlant>()

        // outside the region nothing is read again
        if (region != null) {
            scannedPlants.filterNot { footprintOverlaps(it, region) }.forEach { kept ->
                merged.add(kept)
                remainingStands.removeAll((kept.stands ?: emptyList()).toSet())
                val origin = kept.plant.slot
                val footprint = kept.plant.cropDef.footprint
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

                val previous = previousBySlot[x to y]
                val scanned = readPlantOn(slot, remainingStands, standCache)

                // a placed mutation stays until the soil is bare or it decays
                val runtime = if (previous != null && previous.isPlacedMutation && !shouldReplacePlacedPlant(previous, scanned)) {
                    stillPlaced(previous, remainingStands) ?: continue
                } else {
                    val scannedPlant = scanned ?: continue
                    val def = scannedPlant.plant.cropDef

                    if (previous != null && previous.elementId == scannedPlant.plant.elementId) {
                        keepRecordedState(previous, scannedPlant)
                    } else {
                        // only a plant placed down counts as placed
                        if (def.isMutation && state.lastScanTime != null) {
                            val placedNow = callbacks.placementConfirmed(def, scannedPlant.plant.slot, this)
                            if (scannedPlant.plant.placed || placedNow) {
                                callbacks.markAsPlaced(scannedPlant.plant)
                            } else if (previous == null) {
                                callbacks.claimSpawnedMutation(scannedPlant.plant, layout)
                                capToTicksSinceLook(scannedPlant.plant)
                            }
                        }
                        scannedPlant
                    }
                }

                val def = runtime.plant.cropDef

                remainingStands.removeAll((runtime.stands ?: emptyList()).toSet())

                if (x + def.footprint.width > width ||
                    y + def.footprint.height > height
                ) continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }

                merged.add(runtime)
            }
        }

        scannedPlants.clear()
        scannedPlants.addAll(merged)

        layout.plants.clear()
        layout.plants.addAll(merged.map { it.plant })

        return true
    }

    private fun capToTicksSinceLook(plant: Plant) {
        val ticks = state.ticksSinceLastScan
        if (ticks <= 0) return

        val stageRange = (plant.growthStage as? GrowthStageInfo.Estimated)?.range ?: return
        val highestStage = stageRange.last.coerceAtMost(ticks)
        if (highestStage < stageRange.first) return

        plant.growthStage =
            if (stageRange.first == highestStage) GrowthStageInfo.Known(highestStage) else GrowthStageInfo.Estimated(stageRange.first..highestStage)
    }

    /** the same crop, or the dead plant it decays into */
    private fun shouldReplacePlacedPlant(previous: Plant, scanned: ScannedPlant?): Boolean {
        val scannedPlant = scanned?.plant ?: return false

        return scannedPlant.elementId == previous.elementId || scannedPlant.cropDef === DeadPlant.definition
    }

    /** null once the soil is bare */
    private fun stillPlaced(
        previous: Plant,
        remainingStands: MutableList<ArmorStand>
    ): ScannedPlant? {
        val origin = getPosForSlot(previous.slot) ?: return null

        val (stands, blocks) = whatRemainsInFootprint(origin, previous.cropDef.footprint, remainingStands)
        if (stands.isEmpty() && blocks.isEmpty()) return null

        return ScannedPlant(plant = previous, stands = stands, blocks = blocks)
    }

    private fun keepRecordedState(
        previous: Plant,
        scanned: ScannedPlant
    ): ScannedPlant {
        val scannedPlant = scanned.plant
        scannedPlant.age = previous.age

        scannedPlant.firstSeenStage = previous.firstSeenStage ?: scannedPlant.lowestStage
        scannedPlant.placed = previous.placed

        val readerKeys = scannedPlant.cropDef.stages.flatMapTo(mutableSetOf()) { stage -> stage.readers.map { it.key } }
        previous.readings.forEach { (key, value) ->
            if (key in readerKeys) scannedPlant.readings.putIfAbsent(key, value)
        }

        val previousWater = previous.waterLevel

        if (previousWater != null && previousWater <= WaterModel.DEATH_LEVEL && scannedPlant.consumesWater) {
            scannedPlant.waterLevel =
                WaterModel.lowestWaterLevelStillAlive(previousWater, waterEffectAt(layout, scannedPlant.slot))
            scannedPlant.waterBestCase = null
            scannedPlant.waterPredictedInDebt = true

            callbacks.warnSurvivor(
                DyingPlant(scannedPlant.cropDef.name, layout.displayName(), layout.id)
            )
        } else {
            scannedPlant.waterLevel = previousWater
            scannedPlant.waterBestCase = previous.waterBestCase
            scannedPlant.waterPredictedInDebt = previous.waterPredictedInDebt
            scannedPlant.waterExact = previous.waterExact
        }

        val previousStage = previous.growthStage
        val scannedStage = scannedPlant.growthStage

        if (previousStage is GrowthStageInfo.Known && scannedStage is GrowthStageInfo.Estimated &&
            previousStage.stage in scannedStage.range
        ) {
            scannedPlant.growthStage = previousStage
        }

        return scanned
    }

    fun simulateGreenhouse(ticks: Int, tickMs: Long) {
        simulateLayout(layout, ticks, tickMs)
    }

    /** a copy of the layout after that many ticks */
    fun predictedLayout(ticks: Int, tickMs: Long): GreenhouseLayout {
        val layoutCopy = layout.deepCopy()

        simulateLayout(layoutCopy, ticks, tickMs)

        return layoutCopy
    }

    /** null when the soggybud decays or passes the limit before it is grown */
    fun ticksUntilGrown(from: GreenhouseLayout, slot: LayoutSlot, tickMs: Long): Int? {
        val layoutCopy = from.deepCopy()
        val soggybud = layoutCopy.plants.find { it.slot.x == slot.x && it.slot.y == slot.y } ?: return null
        val limitTicks = soggybud.decayRemainingMs?.let { (it / tickMs).toInt() } ?: SOGGYBUD_GROWTH_LIMIT_TICKS

        var ticks = 0
        while (!soggybud.isFullyGrown) {
            if (ticks >= limitTicks) return null
            simulateLayout(layoutCopy, 1, tickMs)
            ticks++
        }
        return ticks
    }

    companion object {

        private const val SOGGYBUD_GROWTH_LIMIT_TICKS: Int = 200

        fun simulateLayout(layout: GreenhouseLayout, ticks: Int, tickMs: Long) {
            if (ticks <= 0) return

            val soggybuds = layout.plants.filter { it.cropDef.drainsNeighbours }
            if (soggybuds.isEmpty()) {
                growPlants(layout, ticks, tickMs)
                return
            }

            // a grown soggybud stops draining; a grown plant is still drained
            repeat(ticks) {
                soggybudsDrainNeighbours(soggybuds.filter { !it.isFullyGrown }, layout)
                growPlants(layout, 1, tickMs)
            }
        }

        /** taken before the tick's own loss, from every plant around it holding water, corners included */
        private fun soggybudsDrainNeighbours(soggybuds: List<Plant>, layout: GreenhouseLayout) {
            soggybuds.forEach { soggybud ->
                var waterTaken = 0.0

                layout.plantsAround(soggybud).forEach { donor ->
                    // soggybuds do not drain each other
                    if (donor.cropDef.drainsNeighbours || !donor.cropDef.needsWater) return@forEach

                    val plantWater = donor.waterLevel ?: return@forEach
                    if (plantWater <= 0.0) return@forEach

                    val waterGiven = minOf(WaterModel.DRAIN_PER_DONOR, plantWater)
                    donor.waterLevel = plantWater - waterGiven
                    // a drain is paid whether or not the donor's tick is skipped
                    donor.waterBestCase = donor.waterBestCase?.minus(waterGiven)
                    waterTaken += waterGiven
                }

                soggybud.waterLevel = (soggybud.waterLevel ?: 0.0) + waterTaken * WaterModel.DRAIN_KEPT
            }
        }

        private fun growPlants(layout: GreenhouseLayout, ticks: Int, tickMs: Long) {
            val gardenTime = dayOrNightNow()

            layout.plants.forEach { plant ->
                val maxStage = plant.cropDef.maxStage

                // hunger drops every tick, and a plant grows only on ticks it starts fed
                val hunger = plant.hunger
                val ticksFed = if (hunger == null) ticks else ticks.coerceAtMost(ticksUntilHungry(hunger))
                if (hunger != null) plant.readings[CropStandReader.HUNGER] = (hunger - ticks * HUNGER_LOSS_PER_TICK).coerceAtLeast(0)

                // a grown plant stops drinking, judged by its lowest possible stage
                val lowestStage = plant.lowestStage

                if (lowestStage != null && lowestStage >= maxStage) {
                    plant.age = plant.age?.plus(ticks * tickMs)
                    return@forEach
                }

                // a stuck plant still dries out
                val stalledByTimeOfDay = plant.needsOtherTimeOfDay(gardenTime)

                // a plant in debt may skip the tick
                val inDebt = (plant.waterLevel ?: 0.0) < 0
                if (inDebt) plant.waterPredictedInDebt = true

                // in debt the best case keeps its water, since a skipped tick costs none
                fun useWaterFor(ticks: Int) {
                    if (!plant.consumesWater || plant.cropDef.drainsNeighbours) return

                    val waterBefore = plant.waterLevel
                    plant.waterLevel = waterBefore?.let {
                        WaterModel.waterLevelAfter(it, ticks, waterEffectAt(layout, plant.slot))
                    }
                    plant.waterBestCase = if (inDebt) plant.waterBestCase ?: waterBefore else plant.waterLevel
                }

                if (plant.isAsleep || stalledByTimeOfDay || ticksFed == 0) {
                    useWaterFor(ticks)
                    return@forEach
                }

                plant.age = plant.age?.plus(ticksFed * tickMs)

                // only ticks spent growing cost water; a plant that starves first dries through every tick
                val stageToGrow = if (inDebt) plant.highestStage else lowestStage
                val stagesLeft = stageToGrow?.let { (maxStage - it).coerceAtLeast(0) }
                val drinkingTicks = if (stagesLeft == null || ticksFed < stagesLeft) ticks else stagesLeft

                useWaterFor(drinkingTicks)

                val stageRange = when (val stage = plant.growthStage) {
                    is GrowthStageInfo.Known -> stage.stage..stage.stage
                    is GrowthStageInfo.Estimated -> stage.range
                    null -> return@forEach
                }

                // a soggybud leaves a stage once it has stored enough, after this tick's drain
                if (plant.cropDef.drainsNeighbours) {
                    val storedWater = plant.waterLevel ?: 0.0

                    fun soggybudStageAfter(fromStage: Int): Int {
                        var stage = fromStage
                        repeat(ticksFed) {
                            if (stage < maxStage && storedWater >= WaterModel.DRAIN_PER_STAGE * stage) stage++
                        }
                        return stage
                    }

                    val lowestStageAfter = soggybudStageAfter(stageRange.first)
                    val highestStageAfter = soggybudStageAfter(stageRange.last)
                    plant.growthStage =
                        if (lowestStageAfter == highestStageAfter) GrowthStageInfo.Known(lowestStageAfter)
                        else GrowthStageInfo.Estimated(lowestStageAfter..highestStageAfter)
                    return@forEach
                }

                // a snoozling stops at a sleep stage
                val sleepStages = plant.cropDef.sleepStages

                fun nextSleepStage(fromStage: Int): Int =
                    sleepStages.filter { it > fromStage }.minOrNull()?.coerceAtMost(maxStage) ?: maxStage

                // in debt the low end stays and the high end grows
                val lowestStageAfter =
                    if (inDebt) stageRange.first else (stageRange.first + ticksFed).coerceAtMost(nextSleepStage(stageRange.first))
                val highestStageAfter = (stageRange.last + ticksFed).coerceAtMost(nextSleepStage(stageRange.last))

                plant.growthStage =
                    if (lowestStageAfter == highestStageAfter) GrowthStageInfo.Known(lowestStageAfter)
                    else GrowthStageInfo.Estimated(lowestStageAfter..highestStageAfter)

                // asleep only once it grows into a sleep stage
                if (lowestStageAfter in sleepStages && lowestStageAfter > stageRange.first) plant.readings[CropStandReader.ASLEEP] = 1
            }
        }


        var callbacks: GridCallbacks = GridCallbacks.None

        fun waterEffectAt(layout: GreenhouseLayout, slot: LayoutSlot): Int =
            if (callbacks.assumeFlatWater()) 0 else layout.waterEffectAt(slot)

        fun dayOrNightNow(): Int {
            val time = (Minecraft.getInstance().level?.overworldClockTime ?: 0L) % 24000L

            return if (time in 13000L..22999L) {
                CropStandReader.NEEDS_NIGHT
            } else {
                CropStandReader.NEEDS_DAY
            }
        }

        fun matchPlantAt(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot,
            standCache: CropStage.StandCache = CropStage.StandCache()
        ): ScannedPlant? {
            val cropCandidates = CropRegistry.cropsBySoil[soil] ?: return null

            var bestCrop: CropDefinition? = null
            var bestGrowthStage: GrowthStageInfo? = null
            var bestStage: CropStage? = null
            var bestScore = -1
            var bestMatchingHeadPoses = -1
            var bestUsedStands: List<Entity>? = null
            var bestBlocks: Map<BlockPos, BlockState>? = null

            for (cropCandidate in cropCandidates) {
                for (stageCandidate in cropCandidate.stages) {
                    val stageResult = stageCandidate.matchesStage(
                        origin, remainingStands, cropCandidate.footprint, cropCandidate.rotatesWithPlot, standCache = standCache
                    ) ?: continue

                    if (stageResult.score < bestScore) continue
                    if (stageResult.score == bestScore && stageResult.matchingHeadPoses <= bestMatchingHeadPoses) continue

                    bestScore = stageResult.score
                    bestMatchingHeadPoses = stageResult.matchingHeadPoses
                    bestCrop = cropCandidate
                    bestStage = stageCandidate
                    bestUsedStands = stageResult.usedStands
                    bestBlocks = stageResult.matchedBlocks

                    val stageRange = stageCandidate.stageRange
                    bestGrowthStage = if (stageRange.first == stageRange.last) {
                        GrowthStageInfo.Known(stageRange.first)
                    } else {
                        GrowthStageInfo.Estimated(stageRange)
                    }
                }
            }

            val matchedCrop = bestCrop ?: return placedThisSession(origin, soil, remainingStands, slot)

            val plant = Plant(
                matchedCrop.elementId,
                slot = slot,
                growthStage = bestGrowthStage,
                cropDef = matchedCrop
            )

            plant.firstSeenStage = plant.lowestStage

            callbacks.forgetPlayerPlacementAt(origin)

            // a noctilume's craving comes from which skull matched
            bestStage?.traits?.let { plant.readings.putAll(it) }

            // while watering cans are out, the game's water bars replace the plants' own
            val waterBarsExpected = callbacks.waterBarsExpected()
            val standsToRead = currentStandsInFootprint(origin, matchedCrop.footprint).filterNot { stand ->
                waterBarsExpected && stand.customName?.let { CropStandReader.looksLikeWaterBar(it) } == true
            }
            bestStage?.readValues(standsToRead)?.let { plant.readings.putAll(it) }

            return ScannedPlant(
                plant = plant,
                stands = bestUsedStands,
                blocks = bestBlocks
            )
        }

        /** a mutation is placed with a look no grown stage has */
        private fun placedThisSession(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot
        ): ScannedPlant? {
            val placedCrop = callbacks.placedCropAt(origin) ?: return null
            if (soil !in placedCrop.requiredSoil) return null

            val (stands, blocks) = whatRemainsInFootprint(origin, placedCrop.footprint, remainingStands)
            if (stands.isEmpty() && blocks.isEmpty()) {
                callbacks.forgetPlayerPlacementAt(origin)
                return null
            }

            val plant = Plant(
                placedCrop.elementId,
                slot = slot,
                growthStage = GrowthStageInfo.Known(placedCrop.stagePlacedAt),
                cropDef = placedCrop
            )
            plant.placed = true
            plant.firstSeenStage = placedCrop.stagePlacedAt

            return ScannedPlant(plant = plant, stands = stands, blocks = blocks)
        }

        /** markers included: bars over a plant are marker stands */
        private fun currentStandsInFootprint(origin: BlockPos, footprint: Footprint): List<ArmorStand> {
            val level = Minecraft.getInstance().level ?: return emptyList()

            return level.getEntitiesOfClass(ArmorStand::class.java, footprint.spaceAbove(origin, CROP_HEIGHT))
        }

        /** The unclaimed stands and the blocks above the soil across [footprint] from [origin]. */
        private fun whatRemainsInFootprint(
            origin: BlockPos,
            footprint: Footprint,
            remainingStands: List<ArmorStand>
        ): Pair<List<ArmorStand>, Map<BlockPos, BlockState>> {
            val level = Minecraft.getInstance().level ?: return emptyList<ArmorStand>() to emptyMap()
            val stands = currentStandsInFootprint(origin, footprint).filter { it in remainingStands }
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
            return stands to blocks
        }
    }

    fun readPlantOn(
        slot: LayoutSlot,
        remainingStands: MutableList<ArmorStand>,
        standCache: CropStage.StandCache = CropStage.StandCache()
    ): ScannedPlant? {
        val soil = slot.soil?.block ?: return null
        val origin = getPosForSlot(slot) ?: return null

        return matchPlantAt(origin, soil, remainingStands, slot, standCache)
    }

    data class GridState(
        var lastScanTime: Instant? = null,
        var needsRescan: Boolean = false,
        var assignedLayout: GreenhouseLayout? = null,
        var scannedThisVisit: Boolean = false,
        var ticksSinceLastScan: Int = 0,
        var buildAnnounced: Boolean = false,
        /** quarter turns the assigned layout is laid with */
        var planTurns: Int = 0
    ) {
        /** read from disk, resolved once the presets load */
        var assignedLayoutId: String? = null
    }

    override fun toString(): String = layout.displayName()
}
