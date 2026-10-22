package org.magic.magicaddons.features.farming.fortune

import org.magic.magicaddons.data.greenhouse.crops.CropDrop

sealed interface FortuneStat {
    data object General : FortuneStat
    data class Crop(val crop: CropDrop) : FortuneStat
}

data class FortuneSource(val name: String, val stat: FortuneStat, val value: Double, val detail: String? = null)

data class FortuneSnapshot(val sources: List<FortuneSource>, val unknown: List<String>) {
    val general: Double get() = sources.filter { it.stat == FortuneStat.General }.sumOf { it.value }

    fun cropOnly(crop: CropDrop): Double = sources.filter { it.stat == FortuneStat.Crop(crop) }.sumOf { it.value }

    fun forCrop(crop: CropDrop): Double = general + cropOnly(crop)
}

object FortuneCrops {

    private const val MUSHROOM_STAT: String = "Mushroom"

    val MUSHROOMS: List<CropDrop> = listOf(CropDrop.RedMushroom, CropDrop.BrownMushroom)

    fun byStatName(name: String): List<CropDrop> = when (name) {
        MUSHROOM_STAT -> MUSHROOMS
        else -> listOfNotNull(CropDrop.entries.firstOrNull { it.displayName == name })
    }

    fun statName(crop: CropDrop): String = if (crop in MUSHROOMS) MUSHROOM_STAT else crop.displayName

    fun tabStat(crop: CropDrop): String = "${statName(crop)} Fortune"

    val representatives: List<CropDrop> = CropDrop.entries.filter { it != CropDrop.BrownMushroom }
}
