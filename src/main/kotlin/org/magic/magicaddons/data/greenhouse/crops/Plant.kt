package org.magic.magicaddons.data.greenhouse.crops

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction

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
    var age: Long? = null,
    val cropDef: CropDefinition,
    val readings: MutableMap<String, Int> = mutableMapOf(),
    val alternatives: MutableList<CropDefinition> = mutableListOf(),
) {
    val hasAlternatives: Boolean get() = alternatives.isNotEmpty()

    fun acceptsCrop(crop: CropDefinition): Boolean = crop == cropDef || crop in alternatives

    val acceptedCrops: List<CropDefinition> get() = listOf(cropDef) + alternatives

    val isAsleep: Boolean get() = readings[StandReader.ASLEEP] == 1

    val timeOfDayNeeded: Int? get() = readings[StandReader.NEEDS_TIME]

    /** 0 to 100, null without a hunger bar */
    val hunger: Int? get() = readings[StandReader.HUNGER]

    /** if a tick has passed with negative water, then we don't know if it truly passed or not */
    var waterPredictedInDebt: Boolean = false

    var waterExact: Boolean = false

    var firstSeenStage: Int? = null

    var placed: Boolean = false

    /** electricity gained since the last look, for a crop with a [CropDefinition.chargeRule] */
    var charge: Int = 0

    /** read off its bar or set by a discharge; until then the charge is what the stage implies */
    var chargeKnown: Boolean = false

    var waterBestCase: Double? = null

    val isPlacedMutation: Boolean get() = placed && cropDef.isMutation

    val isCollectable: Boolean get() = isPlacedMutation && (age ?: 1L) <= 0L

    val readyToHarvest: Boolean
        get() = (cropDef.isMutation || cropDef.isBaseCrop) && !isPlacedMutation && (highestStage ?: 0) >= cropDef.maxStage

    val consumesWater: Boolean get() = cropDef.needsWater && !isPlacedMutation && !isFullyGrown

    fun copyForPrediction(slot: LayoutSlot): Plant =
        copy(slot = slot, readings = readings.toMutableMap(), alternatives = alternatives.toMutableList()).also {
            it.waterPredictedInDebt = waterPredictedInDebt
            it.waterExact = waterExact
            it.firstSeenStage = firstSeenStage
            it.placed = placed
            it.waterBestCase = waterBestCase
            it.charge = charge
            it.chargeKnown = chargeKnown
        }

    /** null when the stage is unknown or the crop has one stage */
    fun waterLastsUntilGrown(waterEffectPercent: Int): Boolean? {
        if (!consumesWater || cropDef.drainsNeighbours) return true

        val water = waterLevel ?: return null
        if (water <= PlotPrediction.WATER_DEATH_LEVEL) return false

        val ticksLeft = PlotPrediction.ticksUntilDeath(water, waterEffectPercent) ?: return true

        // in debt the highest stage is the one the water paid for
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

    val decayRemainingMs: Long?
        get() {
            val decayTime = cropDef.decayTimeMs
            if (decayTime == NEVER_DECAYS) return null
            val age = age ?: return null
            return (decayTime - age).coerceAtLeast(0L)
        }
}
