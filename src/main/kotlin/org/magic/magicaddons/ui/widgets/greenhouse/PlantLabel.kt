package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.util.toCoarseDuration

enum class PlantLabel(val color: Int, val tabName: String) {
    GrowthStage(0xFF3FBF3F.toInt(), "Growth stage"),
    WaterLevel(Common.UI.WATER_FULL_COLOR, "Water level"),
    DecayTime(Common.UI.DECAY_TIME_COLOR, "Decay time");

    fun valueFor(plant: Plant): String? = when (this) {
        GrowthStage -> when {
            plant.isPlacedMutation -> "Placed"
            plant.cropDef.maxStage <= 1 && !plant.readyToHarvest -> null
            else -> when (val stage = plant.growthStage) {
                is PlantStage.Known -> "${stage.stage}/${plant.cropDef.maxStage}"
                is PlantStage.Estimated -> "~${stage.range.first}-${stage.range.last}"
                null -> null
            }
        }
        WaterLevel -> if (!plant.cropDef.needsWater || plant.isPlacedMutation) null else waterText(plant)
        DecayTime -> plant.decayRemainingMs?.toCoarseDuration()
    }

    fun colorFor(plant: Plant): Int = when {
        this != GrowthStage -> Common.UI.OVERLAY_TEXT_COLOR
        plant.readyToHarvest -> Common.UI.SUCCESS_COLOR
        plant.isPlacedMutation && !plant.isCollectable -> Common.UI.DANGER_COLOR
        else -> Common.UI.OVERLAY_TEXT_COLOR
    }
}

internal fun waterText(plant: Plant): String? = plant.waterLevel?.let { worst ->
    val best = plant.waterBestCase
    if (best == null || best == worst) "${PlotPrediction.formatWaterLevel(worst)}%"
    else "${PlotPrediction.formatWaterLevel(worst)}% to ${PlotPrediction.formatWaterLevel(best)}%"
}
