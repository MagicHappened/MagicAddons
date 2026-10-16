package org.magic.magicaddons.features.farming.fortune

import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datatype.defaults.GemstoneQuality

// Values follow Elite Farmers' farming-weight package (MIT): github.com/EliteFarmers/Website
object FortuneTables {

    const val PER_FARMING_LEVEL: Double = 4.0
    const val MAX_FARMING_LEVEL: Int = 60
    const val PER_PLOT: Double = 3.0
    const val MAX_PLOTS: Int = 24
    const val PER_HARVESTING_LEVEL: Double = 12.5
    const val PER_CULTIVATING_LEVEL: Double = 2.0
    const val PER_PESTERMINATOR_LEVEL: Double = 2.0
    const val PER_FARMING_FOR_DUMMIES: Double = 1.0
    const val MAX_FARMING_FOR_DUMMIES: Int = 5
    const val HELIANTHUS_RELIC: Double = 40.0
    const val YELLOW_BANDANA: Double = 30.0

    const val PER_FLY_LEVEL: Double = 2.5
    const val PER_FIREFLY_LEVEL: Double = 5.0
    const val PER_LUNAR_MOTH_LEVEL: Double = 5.0
    const val PER_EARTHWORM_LEVEL: Double = 3.0
    const val PER_GALAXY_FISH_LEVEL: Double = 1.0

    private val RARITY_COLUMNS = listOf(
        SkyBlockRarity.COMMON, SkyBlockRarity.UNCOMMON, SkyBlockRarity.RARE,
        SkyBlockRarity.EPIC, SkyBlockRarity.LEGENDARY, SkyBlockRarity.MYTHIC
    )

    private fun columnOf(rarity: SkyBlockRarity): Int = RARITY_COLUMNS.indexOf(rarity).coerceIn(0, RARITY_COLUMNS.lastIndex)

    private val REFORGES: Map<String, List<Double>> = mapOf(
        "mossy" to listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0),
        "bustling" to listOf(1.0, 2.0, 4.0, 6.0, 8.0, 10.0),
        "mantid" to listOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
        "rooted" to listOf(6.0, 9.0, 12.0, 15.0, 18.0, 21.0),
        "blooming" to listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
        "squeaky" to listOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
        "thorny" to listOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
        "blessed" to listOf(5.0, 7.0, 10.0, 13.0, 16.0, 20.0),
        "bountiful" to listOf(1.0, 2.0, 3.0, 5.0, 7.0, 10.0),
        "robust" to listOf(2.0, 3.0, 4.0, 6.0, 8.0, 10.0),
        "earthy" to listOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
        "green_thumb" to listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
        "deep_fried" to listOf(5.0, 9.0, 13.0, 18.0, 23.0, 28.0),
        "overpriced" to listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0),
        "buzzing" to listOf(2.0, 3.0, 5.0, 7.0, 9.0, 11.0)
    )

    fun reforgeFortune(modifier: String, rarity: SkyBlockRarity): Double? =
        REFORGES[modifier.lowercase()]?.get(columnOf(rarity))

    private val PERIDOT: Map<GemstoneQuality, List<Double>> = mapOf(
        GemstoneQuality.ROUGH to listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0),
        GemstoneQuality.FLAWED to listOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0),
        GemstoneQuality.FINE to listOf(1.5, 2.0, 3.0, 4.0, 5.0, 6.0),
        GemstoneQuality.FLAWLESS to listOf(2.0, 3.0, 4.0, 5.0, 6.0, 8.0),
        GemstoneQuality.PERFECT to listOf(3.0, 4.0, 5.0, 6.0, 8.0, 10.0)
    )

    fun peridotFortune(quality: GemstoneQuality, rarity: SkyBlockRarity): Double =
        PERIDOT.getValue(quality)[columnOf(rarity)]

    enum class ArmorPiece(val idSuffix: String) {
        HELMET("_HELMET"), CHESTPLATE("_CHESTPLATE"), LEGGINGS("_LEGGINGS"), BOOTS("_BOOTS")
    }

    class ArmorSet(val idPrefix: String, val displayName: String, pieces: List<Double>, val setBonus: List<Double>?) {
        val fortuneByPiece: Map<ArmorPiece, Double> = ArmorPiece.entries.zip(pieces).toMap()
    }

    val ARMOR_SETS: List<ArmorSet> = listOf(
        ArmorSet("FARM_SUIT", "Farm Suit", listOf(5.0, 5.0, 5.0, 5.0), null),
        ArmorSet("FARM_ARMOR", "Farm Armor", listOf(10.0, 10.0, 10.0, 10.0), null),
        ArmorSet("PUMPKIN", "Pumpkin", listOf(15.0, 15.0, 15.0, 15.0), null),
        ArmorSet("MELON", "Melon", listOf(15.0, 20.0, 20.0, 15.0), listOf(10.0, 20.0, 30.0)),
        ArmorSet("CROPIE", "Cropie", listOf(20.0, 25.0, 25.0, 20.0), listOf(15.0, 30.0, 45.0)),
        ArmorSet("SQUASH", "Squash", listOf(25.0, 30.0, 30.0, 25.0), listOf(20.0, 40.0, 60.0)),
        ArmorSet("FERMENTO", "Fermento", listOf(30.0, 35.0, 35.0, 30.0), listOf(25.0, 50.0, 75.0)),
        ArmorSet("HELIANTHUS", "Helianthus", listOf(35.0, 40.0, 40.0, 35.0), listOf(25.0, 50.0, 75.0))
    )

    val FARMING_LEVEL_ARMOR: Set<String> = setOf("FARMER_BOOTS", "RANCHERS_BOOTS", "ENCHANTED_JACK_O_LANTERN")

    val EQUIPMENT_BASE: Map<String, Double> = buildMap {
        listOf("NECKLACE", "CLOAK", "BELT", "BRACELET").forEach { slot ->
            put("LOTUS_$slot", 5.0)
            put("BLOSSOM_$slot", 7.0)
        }
        put("ZORROS_CAPE", 10.0)
    }

    val GARDENING_TOOL_BASE: Map<String, Double> = mapOf(
        "ROOKIE_HOE" to 5.0,
        "ROOKIE_FARMING_AXE" to 5.0,
        "BASIC_GARDENING_HOE" to 10.0,
        "BASIC_GARDENING_AXE" to 10.0,
        "ADVANCED_GARDENING_HOE" to 15.0,
        "ADVANCED_GARDENING_AXE" to 15.0
    )

    val FARMING_TOOL_PREFIXES: List<String> = listOf(
        "THEORETICAL_HOE", "MELON_DICER", "PUMPKIN_DICER", "CACTUS_KNIFE", "COCO_CHOPPER", "FUNGI_CUTTER",
        "ECLIPSE_SICKLE"
    ) + GARDENING_TOOL_BASE.keys

    val VACUUM_BASE: Map<String, Double> = mapOf(
        "SKYMART_VACUUM" to 5.0,
        "SKYMART_TURBO_VACUUM" to 10.0,
        "SKYMART_HYPER_VACUUM" to 15.0,
        "INFINI_VACUUM" to 20.0,
        "INFINI_VACUUM_HOOVERIUS" to 25.0
    )
}
