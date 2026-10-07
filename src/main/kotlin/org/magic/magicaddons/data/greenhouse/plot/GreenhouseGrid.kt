package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils
import java.time.Instant
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DeadPlant
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.ChorusCollision
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

    fun rescanPlants(onlySlots: Set<Pair<Int, Int>>? = null, shouldKeepUnmatchedPlants: Boolean = false): Boolean {
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
        val spawnedMutations = mutableListOf<Plant>()
        val scannedCropBySlot = HashMap<Pair<Int, Int>, String>()

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
                scannedPlant?.let { scannedCropBySlot[x to y] = it.plant.cropDef.name }

                val isStillStanding = plantBefore != null && scannedPlant == null && holdsOwnCropParts(plantBefore)
                val isPlantBeforeKept = plantBefore != null && (plantBefore.isPlacedMutation || shouldKeepUnmatchedPlants || isStillStanding) &&
                        !scanMatchesPlantBefore(plantBefore, scannedPlant)

                val scannedPlantResult = if (plantBefore != null && isPlantBeforeKept) {
                    val kept = plantBeforeIfFootprintFilled(plantBefore, remainingStands) ?: continue
                    if (isStillStanding && !shouldKeepUnmatchedPlants && !plantBefore.isPlacedMutation) noteMissedOnce(plantBefore, x to y)
                    kept
                } else {
                    if (scannedPlant != null) slotsRecheckedForMiss.remove(x to y)
                    if (scannedPlant == null) continue
                    val def = scannedPlant.plant.cropDef

                    val isFreshTeleporterSpawn = plantBefore != null && def.teleportsWhileGrowing && state.lastScanTime != null &&
                            scannedPlant.plant.highestStage == 1 && (plantBefore.lowestStage ?: 0) > 1

                    if (plantBefore != null && plantBefore.cropTypeEquals(scannedPlant.plant) && !isFreshTeleporterSpawn) {
                        keepRecordedState(plantBefore, scannedPlant)
                    } else {
                        if (def.isMutation && state.lastScanTime != null) {
                            val placedNow = callbacks.placementConfirmed(def, scannedPlant.plant.slot, this)
                            if (scannedPlant.plant.placed || placedNow) {
                                callbacks.markAsPlaced(scannedPlant.plant)
                            } else if (plantBefore == null || isFreshTeleporterSpawn) {
                                callbacks.claimSpawnedMutation(scannedPlant.plant, layout)
                                capStageToTicksSinceScan(scannedPlant.plant)
                                spawnedMutations += scannedPlant.plant
                            }
                        }
                        if (def == DeadPlant.definition) scannedPlant.plant.appearedAt = System.currentTimeMillis()
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

        logLostCounts(plantBeforeBySlot.values, merged, scannedCropBySlot, region, shouldKeepUnmatchedPlants)

        scannedPlants.clear()
        scannedPlants.addAll(merged)

        layout.plants.clear()
        layout.plants.addAll(merged.map { it.plant })
        spawnedMutations.forEach { MutationCounting.countSpawn(layout, it, state.blindSpawns) }

        return true
    }


    private fun logLostCounts(
        plantsBefore: Collection<Plant>,
        merged: List<ScannedPlant>,
        scannedCropBySlot: Map<Pair<Int, Int>, String>,
        region: Set<Pair<Int, Int>>?,
        isUnsettled: Boolean
    ) {
        val keptSlots = merged.mapTo(HashSet()) { Triple(it.plant.slot.x, it.plant.slot.y, it.plant.elementId) }
        plantsBefore
            .filter { Triple(it.slot.x, it.slot.y, it.elementId) !in keptSlots && it.cropDef.minMutationsBeforeDecay != null }
            .filterNot { it.isGrowingTeleporter }
            .filter { it.lastDiagnosisReading != null || it.isMutationCountTracked }
            .forEach { lost ->
                val slot = lost.slot.x to lost.slot.y
                val count = (if (lost.mutationsSpawnedIsMinimum) "≥" else "") + lost.mutationsSpawned
                val scanKind = when {
                    region != null -> "region scan"
                    isUnsettled -> "unsettled full scan"
                    else -> "settled full scan"
                }
                Common.LOGGER.info(
                    "[scan] ${layout.displayName()}: ${lost.cropDef.name} at (${slot.first},${slot.second}) dropped with times mutated $count " +
                            "by a $scanKind, scan found ${scannedCropBySlot[slot] ?: "nothing"} there. Stands: ${describeStandsOn(lost)}"
                )
                getPosForSlot(lost.slot)?.let { droppedSoil += it to lost.cropDef.footprint }
            }
    }

    val droppedSoil: MutableList<Pair<BlockPos, Footprint>> = mutableListOf()

    val slotsRecheckedForMiss: MutableSet<Pair<Int, Int>> = mutableSetOf()

    val soilToRecheck: MutableList<BlockPos> = mutableListOf()

    private fun holdsOwnCropParts(plant: Plant): Boolean {
        val soil = getPosForSlot(plant.slot) ?: return false
        val level = Minecraft.getInstance().level ?: return false
        val stages = plant.cropDef.stages
        val skullHashes = stages.flatMap { it.armorStands.orEmpty() }.mapNotNullTo(HashSet()) { it.hashString }
        val blocks = stages.flatMap { it.blocks.orEmpty() }.mapTo(HashSet()) { it.blockState.block }
        val footprint = plant.cropDef.footprint

        val stands = currentStandsInFootprint(soil, footprint)
        if (stands.any { stand -> PlayerUtils.getSkullHash(stand)?.let { it in skullHashes } == true }) return true
        if (skullHashes.isEmpty() && stands.any { EntityUtils.carriesAnything(it) }) return true

        return (0 until footprint.width).any { dx ->
            (0 until footprint.height).any { dz ->
                (1..CROP_HEIGHT).any { dy -> level.getBlockState(soil.offset(dx, dy, dz)).block in blocks }
            }
        }
    }

    private fun noteMissedOnce(plant: Plant, slot: Pair<Int, Int>) {
        if (!slotsRecheckedForMiss.add(slot)) return
        val soil = getPosForSlot(plant.slot) ?: return
        soilToRecheck += soil
        Common.LOGGER.info(
            "[scan] ${layout.displayName()}: ${plant.cropDef.name} at (${slot.first},${slot.second}) not recognised, kept and rechecking. " +
                    "Stands: ${describeStandsOn(plant)}"
        )
    }

    private fun describeStandsOn(plant: Plant): String {
        val soil = getPosForSlot(plant.slot) ?: return "plot not loaded"
        val stands = currentStandsInFootprint(soil, plant.cropDef.footprint)
        if (stands.isEmpty()) return "none"

        return stands.joinToString("; ") { stand ->
            val offset = stand.position().subtract(Vec3.atBottomCenterOf(soil))
            val moving = if (GreenhouseData.isStandMoving(stand.id)) ", still moving" else ""
            "%s offset (%.5f, %.5f, %.5f)%s".format(PlayerUtils.getSkullHash(stand)?.take(8) ?: "no skull", offset.x, offset.y, offset.z, moving)
        }
    }

    private fun capStageToTicksSinceScan(plant: Plant) {
        val ticks = state.ticksSinceLastScan.coerceAtLeast(1)

        val stageRange = (plant.growthStage as? PlantStage.Estimated)?.range ?: return
        val highestStage = stageRange.last.coerceAtMost(ticks)
        if (highestStage < stageRange.first) return

        plant.growthStage =
            if (stageRange.first == highestStage) PlantStage.Known(highestStage) else PlantStage.Estimated(stageRange.first..highestStage)
    }

    private fun scanMatchesPlantBefore(previous: Plant, scanned: ScannedPlant?): Boolean {
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
        scannedPlant.mutationsSpawned = plantBefore.mutationsSpawned
        scannedPlant.mutationsSpawnedIsMinimum = plantBefore.mutationsSpawnedIsMinimum
        scannedPlant.seenSpawnsHelped = plantBefore.seenSpawnsHelped
        scannedPlant.isMutationCountTracked = plantBefore.isMutationCountTracked
        scannedPlant.isCountedFromStart = plantBefore.isCountedFromStart
        scannedPlant.hasUncertainCredit = plantBefore.hasUncertainCredit
        scannedPlant.lastDiagnosisReading = plantBefore.lastDiagnosisReading
        scannedPlant.decayAttemptAt = plantBefore.decayAttemptAt

        val readerKeys = scannedPlant.cropDef.stages.flatMapTo(mutableSetOf()) { stage -> stage.readers.map { it.key } } -
                StandReader.CHARGE - StandReader.ASLEEP
        plantBefore.readings.forEach { (key, value) ->
            if (key in readerKeys) scannedPlant.readings.putIfAbsent(key, value)
        }
        settleCharge(scannedPlant)

        scannedPlant.waterLevel = plantBefore.waterLevel
        scannedPlant.waterBestCase = plantBefore.waterBestCase
        scannedPlant.waterPredictedNegative = plantBefore.waterPredictedNegative
        scannedPlant.waterExact = plantBefore.waterExact

        val previousStage = plantBefore.growthStage
        val scannedStage = scannedPlant.growthStage

        if (previousStage is PlantStage.Known && scannedStage is PlantStage.Estimated &&
            previousStage.stage in scannedStage.range
        ) {
            scannedPlant.growthStage = previousStage
        }
        if (previousStage is PlantStage.Estimated && scannedStage is PlantStage.Estimated) {
            val lowest = maxOf(previousStage.range.first, scannedStage.range.first)
            val highest = minOf(previousStage.range.last, scannedStage.range.last)
            if (lowest <= highest) {
                scannedPlant.growthStage = if (lowest == highest) PlantStage.Known(lowest) else PlantStage.Estimated(lowest..highest)
            }
        }

        return plantAfter
    }

    fun simulateGreenhouse(ticks: Int) {
        state.glasscornsReset = simulateLayout(layout, ticks).glasscornsReset
    }

    fun predictedLayout(ticks: Int): PlotLayout {
        val layoutCopy = layout.freshCopy()
        layoutCopy.plants.filter { it.cropDef.resetPercentByStage.isNotEmpty() }.forEach { it.chanceToReachStage = 1.0 }

        val nextTickInMs = GreenhouseTickTime.remainingTickMs()
        val tickMs = GreenhouseTickTime.tickMs
        if (nextTickInMs == null || tickMs == null) {
            simulateLayout(layoutCopy, ticks)
            return layoutCopy
        }

        val decayOutlookBySlot = layout.plants.associate { (it.slot.x to it.slot.y) to layout.decayOutlookOf(it) }
        for (tick in 1..ticks) {
            decayPlantsWhoseTimerEndsBy(layoutCopy, nextTickInMs + (tick - 1) * tickMs, decayOutlookBySlot)
            simulateLayout(layoutCopy, 1)
        }
        return layoutCopy
    }

    private fun decayPlantsWhoseTimerEndsBy(layout: PlotLayout, msFromNow: Long, decayOutlookBySlot: Map<Pair<Int, Int>, DecayOutlook>) {
        val plantsOutOfTime = layout.plants.filter { plant -> (plant.decayRemainingMs ?: return@filter false) <= msFromNow }
        val decayed = plantsOutOfTime.filter { plant ->
            val outlook = decayOutlookBySlot[plant.slot.x to plant.slot.y] ?: return@filter false
            if (!outlook.isCertain && outlook.canDecay) plant.predictedDecayDoubt = outlook
            outlook.isCertain
        }
        layout.plants.removeAll(decayed)
        decayed.filter { it.cropDef != DeadPlant.definition }.flatMap { it.coveredCells }.forEach { (x, y) ->
            val slot = layout.getSlot(x, y) ?: return@forEach
            layout.plants += Plant(
                elementId = DeadPlant.definition.elementId,
                slot = slot,
                growthStage = PlantStage.Known(DeadPlant.definition.maxStage),
                cropDef = DeadPlant.definition
            )
        }
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

        class PredictedLostPlants(var glasscornsReset: Int = 0)

        private fun isHaltedEvenInBestCase(plant: Plant): Boolean {
            val haltLevel = PlotPrediction.WATER_HALT_LEVEL.toDouble()

            return plant.isHaltedByCharge || (plant.isHaltedByWater && (plant.waterBestCase ?: haltLevel) <= haltLevel)
        }

        fun simulateLayout(layout: PlotLayout, ticks: Int): PredictedLostPlants {
            val losses = PredictedLostPlants()
            val soggybuds = layout.plants.filter { it.cropDef.drainsNeighbours }

            repeat(ticks) {
                soggybudsDrainNeighbours(soggybuds.filter { !it.isFullyGrown }, layout)
                growPlants(layout, 1, losses)
            }
            return losses
        }


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
                    donor.waterBestCase = donor.waterBestCase?.minus(waterGiven)
                    waterTaken += waterGiven
                }

                soggybud.waterLevel = (soggybud.waterLevel ?: 0.0) + waterTaken * PlotPrediction.DRAIN_KEPT
            }
        }

        private fun growPlants(layout: PlotLayout, ticks: Int, losses: PredictedLostPlants) {
            val gardenTime = dayOrNightNow()

            layout.plants.forEach { plant ->
                val maxStage = plant.cropDef.maxStage

                val hunger = plant.hunger
                val ticksFed = if (hunger == null) ticks else ticks.coerceAtMost(ticksUntilStarving(hunger))
                if (hunger != null) plant.readings[StandReader.HUNGER] = (hunger - ticks * HUNGER_LOSS_PER_TICK).coerceAtLeast(0)

                val lowestStage = plant.lowestStage

                if (lowestStage != null && lowestStage >= maxStage && !plant.cropDef.resetsToFirstStage) {
                    return@forEach
                }
                if (isHaltedEvenInBestCase(plant)) return@forEach

                val stalledByTimeOfDay = plant.needsOtherTimeOfDay(gardenTime)
                val cravesTimeOfDay = plant.timeOfDayNeeded != null

                val isWaterNegative = (plant.waterLevel ?: 0.0) < 0
                if (isWaterNegative) plant.waterPredictedNegative = true

                fun consumeWaterForTicks(ticks: Int) {
                    if (!plant.consumesWater || plant.cropDef.drainsNeighbours) return

                    val waterBefore = plant.waterLevel
                    plant.waterLevel = waterBefore?.let {
                        PlotPrediction.waterLevelAfter(it, ticks, waterEffectAt(layout, plant.slot))
                    }
                    plant.waterBestCase = if (isWaterNegative) plant.waterBestCase ?: waterBefore else plant.waterLevel
                }

                if (plant.isAsleep || stalledByTimeOfDay || ticksFed == 0) {
                    consumeWaterForTicks(ticks)
                    return@forEach
                }

                val stagesGrown = if (cravesTimeOfDay) ticksFed.coerceAtMost(1) else ticksFed

                val stageToGrow = if (isWaterNegative) plant.highestStage else lowestStage
                val stagesLeft = stageToGrow?.let { (maxStage - it).coerceAtLeast(0) }
                val drinkingTicks = if (stagesLeft == null || ticksFed < stagesLeft) ticks else stagesLeft

                consumeWaterForTicks(drinkingTicks)
                if (isHaltedEvenInBestCase(plant)) return@forEach

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

                fun stageAfterReset(fromStage: Int, grownBy: Int): Int = when {
                    plant.cropDef.resetsToFirstStage -> (fromStage - 1 + grownBy) % maxStage + 1
                    else -> (fromStage + grownBy).coerceAtMost(nextSleepStage(fromStage))
                }

                val lowestStageAfter = if (isWaterNegative) stageRange.first else stageAfterReset(stageRange.first, stagesGrown)
                val highestStageAfter = stageAfterReset(stageRange.last, stagesGrown)

                plant.growthStage =
                    if (lowestStageAfter == highestStageAfter) PlantStage.Known(lowestStageAfter)
                    else PlantStage.Estimated(lowestStageAfter..highestStageAfter)

                if (plant.cropDef.resetsToFirstStage && lowestStageAfter < stageRange.first) losses.glasscornsReset++

                plant.chanceToReachStage = plant.chanceToReachStage?.let { it * plant.cropDef.chanceToGrow(stageRange.first, lowestStageAfter) }

                if (cravesTimeOfDay && lowestStageAfter > stageRange.first) {
                    plant.readings[StandReader.NEEDS_TIME] =
                        if (plant.timeOfDayNeeded == StandReader.NEEDS_DAY) StandReader.NEEDS_NIGHT else StandReader.NEEDS_DAY
                }

                // asleep only once it grows into a sleep stage
                if (lowestStageAfter in sleepStages && lowestStageAfter > stageRange.first) plant.readings[StandReader.ASLEEP] = 1

                plant.cropDef.chargeRule?.let { chargeRule ->
                    plant.charge += chargeRule.perStage * (lowestStageAfter - stageRange.first)
                }
            }
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
        var planTurns: Int = 0,
        var noRotateAssignedLayout: Boolean = false
    ) {
        /** read from disk, resolved once the presets load */
        var assignedLayoutId: String? = null

        var glasscornsReset: Int = 0

        var chorusLossChanceByTick: DoubleArray? = null

        val blindSpawns: MutableList<BlindSpawns> = mutableListOf()

        var slotsToDiagnose: Set<Pair<Int, Int>> = emptySet()

        var countedSignature: Int? = null

        var chorusRiskCalculation: ChorusCollision.Calculation? = null

        val isChorusRiskCalculating: Boolean
            get() = chorusLossChanceByTick == null && chorusRiskCalculation?.isRunning == true

        val chorusLossChanceNextTick: Double?
            get() = chorusLossChanceByTicksAhead(0)

        fun chorusLossChanceByTicksAhead(ticksAhead: Int): Double? = chorusLossChanceByTick?.let { chances ->
            chances.getOrNull(ticksSinceLastScan + maxOf(ticksAhead, 1) - 1) ?: chances.lastOrNull()
        }
    }

    override fun toString(): String = layout.displayName()
}
