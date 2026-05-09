package org.magic.magicaddons.data.greenhouse


data class GreenhouseElementInstance(
    val elementId: String, //just the skyblock id
    val slot: GreenhouseSlot,
    var waterLevel: Int? = null,
    var growthStage: GrowthStageInfo? = null){




}