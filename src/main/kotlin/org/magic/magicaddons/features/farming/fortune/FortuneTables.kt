package org.magic.magicaddons.features.farming.fortune

import org.magic.magicaddons.data.greenhouse.crops.CropDrop

// Values follow Elite Farmers' farming-weight package (MIT): github.com/EliteFarmers/Website
object FortuneTables {

    const val PER_FARMING_LEVEL: Double = 4.0
    const val MAX_FARMING_LEVEL: Int = 60
    const val PER_PLOT: Double = 3.0
    const val MAX_PLOTS: Int = 24

    const val PER_ANITA_LEVEL: Double = 4.0
    const val MAX_ANITA_LEVEL: Int = 15
    const val PER_COMMUNITY_LEVEL: Double = 4.0
    const val MAX_COMMUNITY_LEVEL: Int = 10
    const val PER_PEST_BRACKET: Double = 0.4
    const val MAX_PEST_BRACKETS: Int = 250
    const val PER_DNA_MILESTONE: Double = 5.0
    const val MAX_DNA_MILESTONES: Int = 6
    const val MAX_ROSEWATER_FLASKS: Int = 5
    const val MAX_CHIP_LEVEL: Int = 20
    const val PER_CROP_UPGRADE: Double = 5.0
    const val MAX_CROP_UPGRADE: Int = 9
    const val MAX_PERSONAL_BEST_FORTUNE: Double = 100.0
    const val PERSONAL_BEST_PER_TIER: Double = 0.01
    const val EXPORTABLE_CROP: Double = 12.0
    const val MAX_COCOA_UPGRADES: Int = 25
    const val MAX_TRUFFLES: Int = 5

    const val PER_FLY_LEVEL: Double = 2.5
    const val PER_FIREFLY_LEVEL: Double = 5.0
    const val PER_LUNAR_MOTH_LEVEL: Double = 5.0
    const val PER_EARTHWORM_LEVEL: Double = 3.0
    const val PER_GALAXY_FISH_LEVEL: Double = 1.0

    private const val CHIP_RARE_MAX_LEVEL: Int = 10
    private const val CHIP_EPIC_MAX_LEVEL: Int = 15

    fun cropshotFortune(level: Int): Double = level * when {
        level <= CHIP_RARE_MAX_LEVEL -> 3.0
        level <= CHIP_EPIC_MAX_LEVEL -> 4.0
        else -> 5.0
    }

    private val PEST_BRACKETS: List<Int> = listOf(1, 2, 3, 5, 7, 9, 14, 17, 21, 25, 50, 80, 125, 175, 250)
    private val SLOW_PEST_BRACKETS: List<Int> = listOf(1, 2, 3, 5, 7, 9, 11, 14, 17, 20, 30, 40, 55, 75, 100)
    private val CLONE_BRACKETS: List<Int> = listOf(1, 2, 3, 5, 7, 9, 11, 14, 17, 20)

    val PEST_BESTIARY_BRACKETS: Map<String, List<Int>> = buildMap {
        listOf("beetle", "cricket", "worm", "fly", "locust", "mite", "mosquito", "moth", "rat", "slug", "dragonfly", "firefly", "praying_mantis")
            .forEach { put("pest_${it}_1", PEST_BRACKETS) }
        put("pest_mouse_1", SLOW_PEST_BRACKETS)
        put("pest_lunar_moth_1", SLOW_PEST_BRACKETS)
        put("zombuddy_1", SLOW_PEST_BRACKETS)
        put("timestalk_clone_100", CLONE_BRACKETS)
    }

    fun pestBrackets(kills: Map<String, Long>): Int = PEST_BESTIARY_BRACKETS.entries.sumOf { (key, brackets) ->
        val killed = kills[key] ?: 0L
        brackets.count { killed >= it }
    }

    val PERSONAL_BEST_CROPS_PER_TIER: Map<CropDrop, Int> = mapOf(
        CropDrop.Wheat to 100,
        CropDrop.Carrot to 300,
        CropDrop.Potato to 300,
        CropDrop.Pumpkin to 100,
        CropDrop.Melon to 500,
        CropDrop.RedMushroom to 100,
        CropDrop.BrownMushroom to 100,
        CropDrop.Cactus to 200,
        CropDrop.SugarCane to 200,
        CropDrop.NetherWart to 300,
        CropDrop.CocoaBeans to 300,
        CropDrop.Sunflower to 200,
        CropDrop.Moonflower to 200,
        CropDrop.WildRose to 200
    )

    fun personalBestFortune(crop: CropDrop, best: Long): Double =
        (best / PERSONAL_BEST_CROPS_PER_TIER.getValue(crop) * PERSONAL_BEST_PER_TIER).coerceAtMost(MAX_PERSONAL_BEST_FORTUNE)

    val HYPIXEL_CROP_IDS: Map<String, List<CropDrop>> = mapOf(
        "WHEAT" to listOf(CropDrop.Wheat),
        "CARROT_ITEM" to listOf(CropDrop.Carrot),
        "POTATO_ITEM" to listOf(CropDrop.Potato),
        "PUMPKIN" to listOf(CropDrop.Pumpkin),
        "MELON" to listOf(CropDrop.Melon),
        "MUSHROOM_COLLECTION" to FortuneCrops.MUSHROOMS,
        "CACTUS" to listOf(CropDrop.Cactus),
        "SUGAR_CANE" to listOf(CropDrop.SugarCane),
        "NETHER_STALK" to listOf(CropDrop.NetherWart),
        "INK_SACK:3" to listOf(CropDrop.CocoaBeans),
        "SUNFLOWER" to listOf(CropDrop.Sunflower),
        "MOONFLOWER" to listOf(CropDrop.Moonflower),
        "WILD_ROSE" to listOf(CropDrop.WildRose)
    )
}
