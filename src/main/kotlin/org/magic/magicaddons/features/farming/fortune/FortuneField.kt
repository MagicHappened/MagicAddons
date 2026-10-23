package org.magic.magicaddons.features.farming.fortune

import org.magic.magicaddons.data.greenhouse.crops.CropDrop

enum class FieldScope { General, PerCrop, Cocoa, Info }

enum class FortuneField(
    val displayName: String,
    val scope: FieldScope,
    val maxLevel: Int?,
    val unit: String,
    val where: String,
    val command: String?,
    val fromApi: Boolean
) {
    Anita("Anita's Extra Farming Fortune", FieldScope.General, FortuneTables.MAX_ANITA_LEVEL, "level", "Anita at the Barn, Extra Farming Fortune, or the stats breakdown", null, true) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_ANITA_LEVEL
    },
    Community("Community Shop Farming Fortune", FieldScope.General, FortuneTables.MAX_COMMUNITY_LEVEL, "level", "Community Center, Community Shop, or the stats breakdown", null, false) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_COMMUNITY_LEVEL
    },
    PestBestiary("Pest Bestiary", FieldScope.General, FortuneTables.MAX_PEST_BRACKETS, "brackets", "Bestiary, Garden pests, or the stats breakdown", null, true) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_PEST_BRACKET
    },
    Cropshot("Cropshot chip", FieldScope.General, FortuneTables.MAX_CHIP_LEVEL, "level", "/chips", "chips", true) {
        override fun fortune(level: Double): Double = FortuneTables.cropshotFortune(level.toInt())
    },
    DnaMilestones("DNA analysis milestones", FieldScope.General, FortuneTables.MAX_DNA_MILESTONES, "milestones", "Jake, Mutation Analysis Rewards, or the stats breakdown", "call jake", false) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_DNA_MILESTONE
    },
    RosewaterFlasks("Filled Rosewater Flasks", FieldScope.General, FortuneTables.MAX_ROSEWATER_FLASKS, "flasks", "SkyBlock XP, Tasks, Consumables, or the stats breakdown", null, false) {
        override fun fortune(level: Double): Double = level
    },
    PetItem("Pet item", FieldScope.General, null, "fortune", "Pets menu, the item held by your active pet, or the stats breakdown", null, false) {
        override fun fortune(level: Double): Double = level
    },
    CropUpgrade("Garden Crop Upgrade", FieldScope.PerCrop, FortuneTables.MAX_CROP_UPGRADE, "level", "Desk, Crop Upgrades", null, true) {
        override fun fortune(level: Double): Double = level * FortuneTables.PER_CROP_UPGRADE
    },
    PersonalBest("Personal Best", FieldScope.PerCrop, FortuneTables.MAX_PERSONAL_BEST_FORTUNE.toInt(), "fortune", "Jacob's personal bests, needs Anita's Personal Bests perk", null, true) {
        override fun fortune(level: Double): Double = level
    },
    ExportableCrop("Exportable Crop", FieldScope.PerCrop, 1, "exported", "Carrolyn's Exportable Crops, only completed exports count", "call carrolyn", false) {
        override fun fortune(level: Double): Double = if (level >= 1) FortuneTables.EXPORTABLE_CROP else 0.0

        override val crops: List<CropDrop> = listOf(
            CropDrop.Wheat, CropDrop.Carrot, CropDrop.Pumpkin, CropDrop.Cactus, CropDrop.CocoaBeans,
            CropDrop.RedMushroom, CropDrop.NetherWart, CropDrop.WildRose
        )
    },
    CropMilestone("Crop Milestone", FieldScope.PerCrop, FortuneTables.MAX_CROP_MILESTONE, "tier", "Desk, Crop Milestones, feeds Dedication", null, false) {
        override fun fortune(level: Double): Double = 0.0
    },
    CocoaUpgrades("Cocoa Fortune upgrades", FieldScope.Cocoa, FortuneTables.MAX_COCOA_UPGRADES, "level", "Chocolate Factory", null, true) {
        override fun fortune(level: Double): Double = level
    },
    Truffles("Refined Dark Cacao Truffles eaten", FieldScope.Cocoa, FortuneTables.MAX_TRUFFLES, "truffles", "SkyBlock XP, Tasks, Consumables", null, false) {
        override fun fortune(level: Double): Double = level
    },
    Hypercharge("Hypercharge chip", FieldScope.Info, FortuneTables.MAX_CHIP_LEVEL, "level", "/chips, strengthens temporary fortune", "chips", true) {
        override fun fortune(level: Double): Double = 0.0
    },
    Overdrive("Overdrive chip", FieldScope.Info, FortuneTables.MAX_CHIP_LEVEL, "level", "/chips, crop fortune on contest crops", "chips", true) {
        override fun fortune(level: Double): Double = 0.0
    },
    UniqueVisitors("Unique visitors served", FieldScope.Info, null, "visitors", "Fetch from API, then counted from accepted visitor offers", null, true) {
        override fun fortune(level: Double): Double = 0.0
    },
    FortunateFeasting("Fortunate Feasting", FieldScope.Info, FortuneTables.MAX_FORTUNATE_FEASTING, "level", "Carnival perk, counts during a Harvest Feast (autumn) or a mayor's feast perk", null, true) {
        override fun fortune(level: Double): Double = 0.0
    },
    AnitaAccessory("Anita accessory contest bonus", FieldScope.Info, 25, "fortune", "Jacob's contest start message, or your accessory bag", null, false) {
        override fun fortune(level: Double): Double = 0.0
    },
    Evergreen("Evergreen chip", FieldScope.Info, FortuneTables.MAX_CHIP_LEVEL, "level", "/chips, more base crops when harvesting", "chips", true) {
        override fun fortune(level: Double): Double = 0.0
    };

    abstract fun fortune(level: Double): Double

    open val crops: List<CropDrop> get() = FortuneCrops.representatives

    fun statFor(crop: CropDrop?): FortuneStat = when (scope) {
        FieldScope.General, FieldScope.Info -> FortuneStat.General
        FieldScope.PerCrop -> FortuneStat.Crop(requireNotNull(crop) { "$displayName needs a crop" })
        FieldScope.Cocoa -> FortuneStat.Crop(CropDrop.CocoaBeans)
    }

    companion object {
        fun byName(name: String): FortuneField? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }

        val CHIPS: Map<String, FortuneField> = mapOf(
            "Cropshot Chip" to Cropshot,
            "Hypercharge Chip" to Hypercharge,
            "Overdrive Chip" to Overdrive,
            "Evergreen Chip" to Evergreen
        )
    }
}
