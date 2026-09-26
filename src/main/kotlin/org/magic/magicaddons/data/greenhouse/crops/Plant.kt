package org.magic.magicaddons.data.greenhouse.crops

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime

sealed interface PlantStage {

    data class Known(val stage: Int) : PlantStage

    data class Estimated(val range: IntRange) : PlantStage

}

data class ScannedPlant(
    val plant: Plant,
    val stands: List<Entity>?,
    val blocks: Map<BlockPos,BlockState>?
)

data class Plant(
    val elementId: String,
    val slot: LayoutSlot,
    var waterLevel: Double? = null,
    var growthStage: PlantStage? = null,
    var appearedAt: Long? = null,
    val cropDef: CropDefinition,
    val readings: MutableMap<String, Int> = mutableMapOf(),
    val presetAlternatives: MutableList<CropDefinition> = mutableListOf(),
) {
    val hasAlternatives: Boolean get() = presetAlternatives.isNotEmpty()

    fun acceptsCrop(crop: CropDefinition): Boolean = crop == cropDef || crop in presetAlternatives

    val acceptedCrops: List<CropDefinition> get() = listOf(cropDef) + presetAlternatives

    val requiredSoils: Set<Block>
        get() = acceptedCrops.flatMapTo(mutableSetOf()) { it.requiredSoil }

    val coveredCells: List<Pair<Int, Int>> get() = cropDef.footprint.cellsFrom(slot.x, slot.y)

    fun covers(slot: LayoutSlot): Boolean =
        slot.x in this.slot.x until this.slot.x + cropDef.footprint.width &&
                slot.y in this.slot.y until this.slot.y + cropDef.footprint.height

    fun cropTypeEquals(other: Plant): Boolean = elementId == other.elementId

    fun placementEquals(other: Plant): Boolean =
        slot.x == other.slot.x && slot.y == other.slot.y &&
                acceptedCrops.toSet() == other.acceptedCrops.toSet()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    val isAsleep: Boolean get() = readings[StandReader.ASLEEP] == 1

    val timeOfDayNeeded: Int? get() = readings[StandReader.NEEDS_TIME]

    val hunger: Int? get() = readings[StandReader.HUNGER]

    var waterPredictedInDebt: Boolean = false

    var waterExact: Boolean = false

    var firstSeenStage: Int? = null

    var placed: Boolean = false

    var charge: Int = 0

    var chargeKnown: Boolean = false

    var waterBestCase: Double? = null

    val isPlacedMutation: Boolean get() = placed && cropDef.isMutation

    val isCollectable: Boolean
        get() = isPlacedMutation && appearedAt?.let { GreenhouseTickTime.hasGrowthTickPassedSince(it) } != true

    val readyToHarvest: Boolean
        get() = (cropDef.isMutation || cropDef.isBaseCrop) && !isPlacedMutation && (highestStage ?: 0) >= cropDef.maxStage

    val consumesWater: Boolean get() = cropDef.needsWater && !isPlacedMutation && !isFullyGrown

    fun copyForPrediction(slot: LayoutSlot): Plant =
        copy(slot = slot, readings = readings.toMutableMap(), presetAlternatives = presetAlternatives.toMutableList()).also {
            it.waterPredictedInDebt = waterPredictedInDebt
            it.waterExact = waterExact
            it.firstSeenStage = firstSeenStage
            it.placed = placed
            it.waterBestCase = waterBestCase
            it.charge = charge
            it.chargeKnown = chargeKnown
        }

    fun waterLastsUntilGrown(waterEffectPercent: Int): Boolean? {
        if (!consumesWater || cropDef.drainsNeighbours) return true

        val water = waterLevel ?: return null
        if (water <= PlotPrediction.WATER_DEATH_LEVEL) return false

        val ticksLeft = PlotPrediction.ticksUntilDeath(water, waterEffectPercent) ?: return true

        val stage = (if (waterPredictedInDebt) highestStage else lowestStage) ?: return null
        if (cropDef.maxStage <= 1) return null

        return ticksLeft > cropDef.maxStage - stage
    }

    val isFullyGrown: Boolean get() = (lowestStage ?: 0) >= cropDef.maxStage

    val lowestStage: Int?
        get() = when (val stage = growthStage) {
            is PlantStage.Known -> stage.stage
            is PlantStage.Estimated -> stage.range.first
            null -> null
        }

    val highestStage: Int?
        get() = when (val stage = growthStage) {
            is PlantStage.Known -> stage.stage
            is PlantStage.Estimated -> stage.range.last
            null -> null
        }

    val grewInPlace: Boolean
        get() {
            if (placed) return false
            val firstStage = firstSeenStage ?: return false
            val currentStage = lowestStage ?: return false

            return currentStage > firstStage
        }

    fun needsOtherTimeOfDay(dayOrNight: Int): Boolean {
        val needed = timeOfDayNeeded ?: return false
        val stage = lowestStage
        return needed != dayOrNight && (stage == null || stage < cropDef.maxStage)
    }

    val age: Long?
        get() = appearedAt?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) }

    val decayRemainingMs: Long?
        get() {
            val decayTime = cropDef.decayTimeMs
            if (decayTime == NEVER_DECAYS) return null
            val age = age ?: return null
            return (decayTime - age).coerceAtLeast(0L)
        }
}
