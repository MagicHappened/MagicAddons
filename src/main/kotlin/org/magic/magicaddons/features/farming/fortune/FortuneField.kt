package org.magic.magicaddons.features.farming.fortune

import org.magic.magicaddons.data.greenhouse.crops.CropDrop

enum class FieldScope { General, PerCrop, Cocoa }

enum class FortuneField(
    val displayName: String,
    val scope: FieldScope,
    val maxLevel: Int?,
    val unit: String,
    val where: String,
    val command: String?,
    val fromApi: Boolean
) {
    Anita("Anita's Extra Farming Fortune", FieldScope.General, FortuneTables.MAX_ANITA_LEVEL, "level", "Anita at the Barn, Extra Farming Fortune", null, true) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_ANITA_LEVEL
    },
    Community("Community Shop Farming Fortune", FieldScope.General, FortuneTables.MAX_COMMUNITY_LEVEL, "level", "Community Center, Community Shop", null, false) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_COMMUNITY_LEVEL
    },
    PestBestiary("Pest Bestiary", FieldScope.General, FortuneTables.MAX_PEST_BRACKETS, "brackets", "Bestiary, Garden pests", null, true) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_PEST_BRACKET
    },
    Cropshot("Cropshot chip", FieldScope.General, FortuneTables.MAX_CHIP_LEVEL, "level", "/chips", "chips", true) {
        override fun fortune(level: Double): Double = FortuneTables.cropshotFortune(level.toInt())
    },
    DnaMilestones("DNA analysis milestones", FieldScope.General, FortuneTables.MAX_DNA_MILESTONES, "milestones", "Crop Analyzer in the greenhouse", null, false) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_DNA_MILESTONE
    },
    RosewaterFlasks("Filled Rosewater Flasks", FieldScope.General, FortuneTables.MAX_ROSEWATER_FLASKS, "flasks", "SkyBlock Menu, SkyBlock XP, Consumables", null, false) {
        override fun fortune(level: Double): Double = level
    },
    PetItem("Pet item", FieldScope.General, null, "fortune", "Pets menu, the item held by your active pet", null, false) {
        override fun fortune(level: Double): Double = level
    },
    CropUpgrade("Garden Crop Upgrade", FieldScope.PerCrop, FortuneTables.MAX_CROP_UPGRADE, "level", "Desk, Crop Upgrades", null, true) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_CROP_UPGRADE
    },
    PersonalBest("Personal Best", FieldScope.PerCrop, FortuneTables.MAX_PERSONAL_BEST_FORTUNE.toInt(), "fortune", "Jacob's personal bests, needs Anita's Personal Bests perk", null, true) {
        override fun fortune(level: Double): Double = level
    },
    ExportableCrop("Exportable Crop", FieldScope.PerCrop, 1, "unlocked", "Carrolyn, one per exported crop", null, false) {
        override fun fortune(level: Double): Double = if (level >= 1) FortuneTables.EXPORTABLE_CROP else 0.0
    },
    CocoaUpgrades("Cocoa Fortune upgrades", FieldScope.Cocoa, FortuneTables.MAX_COCOA_UPGRADES, "level", "Chocolate Factory", null, true) {
        override fun fortune(level: Double): Double = level
    },
    Truffles("Refined Dark Cacao Truffles eaten", FieldScope.Cocoa, FortuneTables.MAX_TRUFFLES, "truffles", "Chocolate Factory", null, false) {
        override fun fortune(level: Double): Double = level
    };

    abstract fun fortune(level: Double): Double

    fun statFor(crop: CropDrop?): FortuneStat = when (scope) {
        FieldScope.General -> FortuneStat.General
        FieldScope.PerCrop -> FortuneStat.Crop(requireNotNull(crop) { "$displayName needs a crop" })
        FieldScope.Cocoa -> FortuneStat.Crop(CropDrop.CocoaBeans)
    }

    companion object {
        fun byName(name: String): FortuneField? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
