package org.magic.magicaddons.data.greenhouse.plot

import java.time.Instant
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DeadPlant
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

const val GREENHOUSE_SOIL_Y: Int = 73
const val GREENHOUSE_SIZE: Int = 10

const val CROP_HEIGHT: Int = 5

const val HUNGER_LOSS_PER_TICK: Int = 20

fun ticksUntilStarving(foodLevel: Int): Int = (foodLevel + HUNGER_LOSS_PER_TICK - 1) / HUNGER_LOSS_PER_TICK

class GreenhouseGrid(
    var state: GridState,
    var layout: PlotLayout
) {
    var plot: Plot? = null
    val width = GREENHOUSE_SIZE
    val height = GREENHOUSE_SIZE

    val scannedPlants = mutableListOf<ScannedPlant>()

    fun isScanned(): Boolean {
        return state.scanned
    }

    fun getPosForSlot(slot: LayoutSlot): BlockPos? {
        val buildArea = plot?.getBuildableArea() ?: return null

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, GREENHOUSE_SOIL_Y, worldZ)
    }

    fun bestRotationFor(plan: PlotLayout): Int =
        (0 until 4).maxBy { turns -> configurationForLayout(plan.turnedBy(turns)) }

    // compare 2 rotations and keep the one that is better
    fun compareRotations(plan: PlotLayout, currentRotation: Int): Int {
        val bestRotation = bestRotationFor(plan)

        return if (configurationForLayout(plan.turnedBy(bestRotation)) > configurationForLayout(plan.turnedBy(currentRotation))) bestRotation else currentRotation
    }

    class PlotConfiguration(val plants: Int, val clashingCropSlots: Int, val soil: Int) : Comparable<PlotConfiguration> {
        private val score: Int get() = plants * MATCHED_PLANT_SCORE - clashingCropSlots

        override fun compareTo(other: PlotConfiguration): Int =
            compareValuesBy(this, other, { it.score }, { it.soil })

        override fun toString(): String = "$plants plants, $clashingCropSlots clashing slots, $soil soil"
    }

    fun configurationForLayout(plan: PlotLayout): PlotConfiguration {
        val soil = plan.slots.count { plannedSlot ->
            val plannedSoil = plannedSlot.soil ?: return@count false
            plannedSoil == layout.getSlot(plannedSlot.x, plannedSlot.y)?.soil
        }
        val plants = plan.plants.count { plannedPlant ->
            scannedPlants.any {
                it.plant.cropDef == plannedPlant.cropDef &&
                        it.plant.slot.x == plannedPlant.slot.x && it.plant.slot.y == plannedPlant.slot.y
            }
        }
        val clashingCrops = scannedPlants.count { scanned ->
            val footprint = scanned.plant.cropDef.footprint
            (0 until footprint.width).any { offsetX ->
                (0 until footprint.height).any { offsetY ->
                    val x = scanned.plant.slot.x + offsetX
                    val y = scanned.plant.slot.y + offsetY
                    val plannedSoil = plan.getSlot(x, y)?.soil
                    plannedSoil != null && plannedSoil != layout.getSlot(x, y)?.soil
                }
            }
        }

        return PlotConfiguration(plants, clashingCrops, soil)
    }

    fun getPosForSlotCoords(x: Int, y: Int): BlockPos? {
        layout.getSlot(x,y)?.let {
            return getPosForSlot(it)
        }
        return null
    }

    fun assignedPlanAfterTurn(): PlotLayout? = state.assignedLayout?.turnedBy(state.planTurns)

    fun plannedPlantAt(x: Int, y: Int): Plant? {
        val plan = assignedPlanAfterTurn() ?: return null
        return plan.getSlot(x, y)?.let { plan.plantCovering(it) }
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
                it.soil = state.block
            }
        }
    }


    fun regionSetFromPositions(positions: Collection<BlockPos>): Set<Pair<Int, Int>> {
        val reach = CropRegistry.allCrops.maxOf { maxOf(it.footprint.width, it.footprint.height) } - 1
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

        val plantBeforeBySlot = layout.plants.associateBy { it.slot.x to it.slot.y }
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

                val plantBefore = plantBeforeBySlot[x to y]
                val scannedPlant = slot.soil?.let { soil ->
                    getPosForSlot(slot)?.let { matchPlantAt(it, soil, remainingStands, slot, standCache) }
                }

                val scannedPlantResult = if (plantBefore != null && plantBefore.isPlacedMutation && !scanMatchesPlacedPlant(plantBefore, scannedPlant)) {
                    plantBeforeIfFootprintFilled(plantBefore, remainingStands) ?: continue
                } else {
                    if (scannedPlant == null) continue
                    val def = scannedPlant.plant.cropDef

                    if (plantBefore != null && plantBefore.cropTypeEquals(scannedPlant.plant)) {
                        keepRecordedState(plantBefore, scannedPlant)
                    } else {
                        if (def.isMutation && state.lastScanTime != null) {
                            val placedNow = callbacks.placementConfirmed(def, scannedPlant.plant.slot, this)
                            if (scannedPlant.plant.placed || placedNow) {
                                callbacks.markAsPlaced(scannedPlant.plant)
                            } else if (plantBefore == null) {
                                callbacks.claimSpawnedMutation(scannedPlant.plant, layout)
                                capStageToTicksSinceScan(scannedPlant.plant)
                            }
                        }
                        scannedPlant
                    }
                }

                val def = scannedPlantResult.plant.cropDef

                remainingStands.removeAll((scannedPlantResult.stands ?: emptyList()).toSet())

                if (x + def.footprint.width > width ||
                    y + def.footprint.height > height
                ) continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }

                merged.add(scannedPlantResult)
            }
        }

        scannedPlants.clear()
        scannedPlants.addAll(merged)

        layout.plants.clear()
        layout.plants.addAll(merged.map { it.plant })

        return true
    }

    private fun capStageToTicksSinceScan(plant: Plant) {
        val ticks = state.ticksSinceLastScan
        if (ticks <= 0) return

        val stageRange = (plant.growthStage as? PlantStage.Estimated)?.range ?: return
        val highestStage = stageRange.last.coerceAtMost(ticks)
        if (highestStage < stageRange.first) return

        plant.growthStage =
            if (stageRange.first == highestStage) PlantStage.Known(highestStage) else PlantStage.Estimated(stageRange.first..highestStage)
    }

    private fun scanMatchesPlacedPlant(previous: Plant, scanned: ScannedPlant?): Boolean {
        val scannedPlant = scanned?.plant ?: return false

        return scannedPlant.cropTypeEquals(previous) || scannedPlant.cropDef === DeadPlant.definition
    }

    private fun plantBeforeIfFootprintFilled(
        previous: Plant,
        remainingStands: List<ArmorStand>
    ): ScannedPlant? {
        val origin = getPosForSlot(previous.slot) ?: return null

        val (stands, blocks) = existsInFootprint(origin, previous.cropDef.footprint, remainingStands)
        if (stands.isEmpty() && blocks.isEmpty()) return null

        return ScannedPlant(plant = previous, stands = stands, blocks = blocks)
    }

    private fun keepRecordedState(
        plantBefore: Plant,
        plantAfter: ScannedPlant
    ): ScannedPlant {
        val scannedPlant = plantAfter.plant
        scannedPlant.appearedAt = plantBefore.appearedAt

        scannedPlant.charge = plantBefore.charge
        scannedPlant.chargeKnown = plantBefore.chargeKnown

        scannedPlant.firstSeenStage = plantBefore.firstSeenStage ?: scannedPlant.lowestStage
        scannedPlant.placed = plantBefore.placed

        val readerKeys = scannedPlant.cropDef.stages.flatMapTo(mutableSetOf()) { stage -> stage.readers.map { it.key } } -
                StandReader.CHARGE - StandReader.ASLEEP
        plantBefore.readings.forEach { (key, value) ->
            if (key in readerKeys) scannedPlant.readings.putIfAbsent(key, value)
        }
        settleCharge(scannedPlant)

        val waterBefore = plantBefore.waterLevel

        if (waterBefore != null && waterBefore <= PlotPrediction.WATER_DEATH_LEVEL && scannedPlant.consumesWater) {
            scannedPlant.waterLevel =
                PlotPrediction.lowestWaterLevelStillAlive(waterBefore, waterEffectAt(layout, scannedPlant.slot))
            scannedPlant.waterBestCase = null
            scannedPlant.waterPredictedInDebt = true
        } else {
            scannedPlant.waterLevel = waterBefore
            scannedPlant.waterBestCase = plantBefore.waterBestCase
            scannedPlant.waterPredictedInDebt = plantBefore.waterPredictedInDebt
            scannedPlant.waterExact = plantBefore.waterExact
        }

        val previousStage = plantBefore.growthStage
        val scannedStage = scannedPlant.growthStage

        if (previousStage is PlantStage.Known && scannedStage is PlantStage.Estimated &&
            previousStage.stage in scannedStage.range
        ) {
            scannedPlant.growthStage = previousStage
        }

        return plantAfter
    }

    fun simulateGreenhouse(ticks: Int) {
        val losses = simulateLayout(layout, ticks)
        state.thunderlingsDestroyed = losses.thunderlingsDestroyed
        state.glasscornsReset = losses.glasscornsReset
    }

    fun predictedLayout(ticks: Int): PlotLayout {
        val layoutCopy = layout.freshCopy()

        simulateLayout(layoutCopy, ticks)

        return layoutCopy
    }

    // null if predicted to not grow
    fun ticksUntilGrown(from: PlotLayout, slot: LayoutSlot, tickMs: Long? = GreenhouseTickTime.tickMs): Int? {
        val knownTickMs = tickMs ?: return null
        val layoutCopy = from.freshCopy()
        val soggybud = layoutCopy.plants.find { it.slot.x == slot.x && it.slot.y == slot.y } ?: return null
        val ticksBeforeDecay = soggybud.decayRemainingMs?.let { (it / knownTickMs).toInt() }
        val limitTicks = minOf(ticksBeforeDecay ?: SOGGYBUD_GROWTH_LIMIT_TICKS, SOGGYBUD_GROWTH_LIMIT_TICKS)

        var ticks = 0
        while (!soggybud.isFullyGrown) {
            if (ticks >= limitTicks) return null
            simulateLayout(layoutCopy, 1)
            ticks++
        }
        return ticks
    }

    companion object {

        private const val SOGGYBUD_GROWTH_LIMIT_TICKS: Int = 200

        private const val MATCHED_PLANT_SCORE: Int = 2

        class PredictedLostPlants(var thunderlingsDestroyed: Int = 0, var glasscornsReset: Int = 0)

        fun simulateLayout(layout: PlotLayout, ticks: Int): PredictedLostPlants {
            val losses = PredictedLostPlants()
            if (ticks <= 0) return losses

            val soggybuds = layout.plants.filter { it.cropDef.drainsNeighbours }
            if (soggybuds.isEmpty()) {
                growPlants(layout, ticks, losses)
                return losses
            }

            // a grown soggybud stops draining; a grown plant is still drained
            repeat(ticks) {
                soggybudsDrainNeighbours(soggybuds.filter { !it.isFullyGrown }, layout)
                growPlants(layout, 1, losses)
            }
            return losses
        }

        /** taken before the tick's own loss, from every plant around it holding water, corners included */
        private fun soggybudsDrainNeighbours(soggybuds: List<Plant>, layout: PlotLayout) {
            soggybuds.forEach { soggybud ->
                var waterTaken = 0.0

                layout.plantsSurrounding(soggybud).forEach { donor ->
                    // soggybuds do not drain each other
                    if (donor.cropDef.drainsNeighbours || !donor.cropDef.needsWater) return@forEach

                    val plantWater = donor.waterLevel ?: return@forEach
                    if (plantWater <= 0.0) return@forEach

                    val waterGiven = minOf(PlotPrediction.DRAIN_PER_DONOR, plantWater)
                    donor.waterLevel = plantWater - waterGiven
                    // a drain is paid whether or not the donor's tick is skipped
                    donor.waterBestCase = donor.waterBestCase?.minus(waterGiven)
                    waterTaken += waterGiven
                }

                soggybud.waterLevel = (soggybud.waterLevel ?: 0.0) + waterTaken * PlotPrediction.DRAIN_KEPT
            }
        }

        private fun growPlants(layout: PlotLayout, ticks: Int, losses: PredictedLostPlants) {
            val gardenTime = dayOrNightNow()
            val overloaded = mutableListOf<Plant>()

            layout.plants.forEach { plant ->
                val maxStage = plant.cropDef.maxStage

                // hunger drops every tick, and a plant grows only on ticks it starts fed
                val hunger = plant.hunger
                val ticksFed = if (hunger == null) ticks else ticks.coerceAtMost(ticksUntilStarving(hunger))
                if (hunger != null) plant.readings[StandReader.HUNGER] = (hunger - ticks * HUNGER_LOSS_PER_TICK).coerceAtLeast(0)

                // a grown plant stops drinking, judged by its lowest possible stage
                val lowestStage = plant.lowestStage

                if (lowestStage != null && lowestStage >= maxStage && !plant.cropDef.resetsToFirstStage) {
                    return@forEach
                }

                // a stuck plant still dries out
                val stalledByTimeOfDay = plant.needsOtherTimeOfDay(gardenTime)
                val cravesTimeOfDay = plant.timeOfDayNeeded != null

                // a plant in debt may skip the tick
                val inDebt = (plant.waterLevel ?: 0.0) < 0
                if (inDebt) plant.waterPredictedInDebt = true

                // in debt the best case keeps its water, since a skipped tick costs none
                fun useWaterFor(ticks: Int) {
                    if (!plant.consumesWater || plant.cropDef.drainsNeighbours) return

                    val waterBefore = plant.waterLevel
                    plant.waterLevel = waterBefore?.let {
                        PlotPrediction.waterLevelAfter(it, ticks, waterEffectAt(layout, plant.slot))
                    }
                    plant.waterBestCase = if (inDebt) plant.waterBestCase ?: waterBefore else plant.waterLevel
                }

                if (plant.isAsleep || stalledByTimeOfDay || ticksFed == 0) {
                    useWaterFor(ticks)
                    return@forEach
                }

                // a plant craving a time of day grows one stage, then craves the other one and stalls
                val stagesGrown = if (cravesTimeOfDay) ticksFed.coerceAtMost(1) else ticksFed

                // only ticks spent growing cost water; a plant that starves first dries through every tick
                val stageToGrow = if (inDebt) plant.highestStage else lowestStage
                val stagesLeft = stageToGrow?.let { (maxStage - it).coerceAtLeast(0) }
                val drinkingTicks = if (stagesLeft == null || ticksFed < stagesLeft) ticks else stagesLeft

                useWaterFor(drinkingTicks)

                val stageRange = when (val stage = plant.growthStage) {
                    is PlantStage.Known -> stage.stage..stage.stage
                    is PlantStage.Estimated -> stage.range
                    null -> return@forEach
                }

                // a soggybud leaves a stage once it has stored enough, after this tick's drain
                if (plant.cropDef.drainsNeighbours) {
                    val storedWater = plant.waterLevel ?: 0.0

                    fun soggybudStageAfter(fromStage: Int): Int {
                        var stage = fromStage
                        repeat(ticksFed) {
                            if (stage < maxStage && storedWater >= PlotPrediction.DRAIN_PER_STAGE * stage) stage++
                        }
                        return stage
                    }

                    val lowestStageAfter = soggybudStageAfter(stageRange.first)
                    val highestStageAfter = soggybudStageAfter(stageRange.last)
                    plant.growthStage =
                        if (lowestStageAfter == highestStageAfter) PlantStage.Known(lowestStageAfter)
                        else PlantStage.Estimated(lowestStageAfter..highestStageAfter)
                    return@forEach
                }

                // a snoozling stops at a sleep stage
                val sleepStages = plant.cropDef.sleepStages

                fun nextSleepStage(fromStage: Int): Int =
                    sleepStages.filter { it > fromStage }.minOrNull()?.coerceAtMost(maxStage) ?: maxStage

                fun stageAfter(fromStage: Int, grownBy: Int): Int = when {
                    plant.cropDef.resetsToFirstStage -> (fromStage - 1 + grownBy) % maxStage + 1
                    else -> (fromStage + grownBy).coerceAtMost(nextSleepStage(fromStage))
                }

                // on negative water the low end stays and the high end grows
                val lowestStageAfter = if (inDebt) stageRange.first else stageAfter(stageRange.first, stagesGrown)
                val highestStageAfter = stageAfter(stageRange.last, stagesGrown)

                plant.growthStage =
                    if (lowestStageAfter == highestStageAfter) PlantStage.Known(lowestStageAfter)
                    else PlantStage.Estimated(lowestStageAfter..highestStageAfter)

                if (plant.cropDef.resetsToFirstStage && lowestStageAfter < stageRange.first) losses.glasscornsReset++

                if (cravesTimeOfDay && lowestStageAfter > stageRange.first) {
                    plant.readings[StandReader.NEEDS_TIME] =
                        if (plant.timeOfDayNeeded == StandReader.NEEDS_DAY) StandReader.NEEDS_NIGHT else StandReader.NEEDS_DAY
                }

                // asleep only once it grows into a sleep stage
                if (lowestStageAfter in sleepStages && lowestStageAfter > stageRange.first) plant.readings[StandReader.ASLEEP] = 1

                plant.cropDef.chargeRule?.let { chargeRule ->
                    plant.charge += chargeRule.perStage * (lowestStageAfter - stageRange.first)
                    if (plant.charge >= chargeRule.limit) overloaded += plant
                }
            }

            // a charged thunderling destroys itself on reaching its limit
            layout.plants.removeAll(overloaded)
            losses.thunderlingsDestroyed += overloaded.size
        }


        var callbacks: GridCallbacks = GridCallbacks.None

        fun waterEffectAt(layout: PlotLayout, slot: LayoutSlot): Int =
            if (callbacks.assumeFlatWater()) 0 else layout.waterEffectAt(slot)

        fun dayOrNightNow(): Int {
            val time = (Minecraft.getInstance().level?.overworldClockTime ?: 0L) % 24000L

            return if (time in 13000L..22999L) {
                StandReader.NEEDS_NIGHT
            } else {
                StandReader.NEEDS_DAY
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
            var bestGrowthStage: PlantStage? = null
            var bestStage: CropStage? = null
            var bestScore = -1
            var bestMatchingHeadPoses = -1
            var bestUsedStands: List<Entity>? = null
            var bestBlocks: Map<BlockPos, BlockState>? = null

            for (cropCandidate in cropCandidates) {
                for (stageCandidate in cropCandidate.stages) {
                    val stageResult = stageCandidate.matchesStage(
                        origin, remainingStands, cropCandidate,
                        ignoreStemAge = cropCandidate.stemAgeVaries,
                        standCache = standCache
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
                        PlantStage.Known(stageRange.first)
                    } else {
                        PlantStage.Estimated(stageRange)
                    }
                }
            }

            // if no crop match
            val matchedCrop = bestCrop ?: return placedThisSession(origin, soil, remainingStands, slot)

            val plant = Plant(
                matchedCrop.elementId,
                slot = slot,
                growthStage = bestGrowthStage,
                cropDef = matchedCrop
            )

            plant.firstSeenStage = plant.lowestStage

            callbacks.forgetPlayerPlacementAt(origin)

            // noctilume needs based on which version matched
            bestStage?.traits?.let { plant.readings.putAll(it) }

            // while watering cans are out, the game's water bars replace the plants' own
            val waterBarsExpected = callbacks.waterBarsExpected()
            val standsToRead = currentStandsInFootprint(origin, matchedCrop.footprint).filterNot { stand ->
                waterBarsExpected && stand.customName?.let { StandReader.looksLikeWaterBar(it) } == true
            }
            bestStage?.readValues(standsToRead)?.let { plant.readings.putAll(it) }
            settleCharge(plant)


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

            val (stands, blocks) = existsInFootprint(origin, placedCrop.footprint, remainingStands)
            if (stands.isEmpty() && blocks.isEmpty()) {
                callbacks.forgetPlayerPlacementAt(origin)
                return null
            }

            val plant = Plant(
                placedCrop.elementId,
                slot = slot,
                growthStage = PlantStage.Known(placedCrop.stagePlacedAt),
                cropDef = placedCrop
            )
            plant.placed = true
            plant.firstSeenStage = placedCrop.stagePlacedAt

            return ScannedPlant(plant = plant, stands = stands, blocks = blocks)
        }

        /**
         * A bar read this scan is the charge. Without one, a charge never read or told is at least what
         * the stage implies, since the plant spawned at stage 1 with none and gained a stage's worth each
         * stage since.
         */
        private fun settleCharge(plant: Plant) {
            val rule = plant.cropDef.chargeRule ?: return
            val shown = plant.readings[StandReader.CHARGE]

            if (shown != null) {
                plant.charge = rule.clampToNearest2k(shown)
                plant.chargeKnown = true
                return
            }

            if (plant.chargeKnown) return

            val stage = plant.highestStage ?: 1
            plant.charge = maxOf(plant.charge, rule.chargeByStageNum(stage))

            // a plant that has not grown a stage yet holds nothing, so stage 1 is read off, not guessed
            plant.chargeKnown = stage <= 1
        }

        private fun currentStandsInFootprint(origin: BlockPos, footprint: Footprint): List<ArmorStand> {
            val level = Minecraft.getInstance().level ?: return emptyList()
            return level.getEntitiesOfClass(ArmorStand::class.java, footprint.spaceAbove(origin, CROP_HEIGHT))
        }

        private fun existsInFootprint(
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

    data class GridState(
        var lastScanTime: Instant? = null,
        var needsRescan: Boolean = false,
        var assignedLayout: PlotLayout? = null,
        var scanned: Boolean = false,
        var ticksSinceLastScan: Int = 0,
        var buildAnnounced: Boolean = false,
        /** quarter turns the assigned layout is laid with */
        var planTurns: Int = 0
    ) {
        /** read from disk, resolved once the presets load */
        var assignedLayoutId: String? = null

        var thunderlingsDestroyed: Int = 0
        var glasscornsReset: Int = 0
    }

    override fun toString(): String = layout.displayName()
}
