package org.magic.magicaddons.features.farming.fortune

import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

object ItemFortune {

    private val FARMING_FORTUNE_LINE: Regex = Regex("""^Farming Fortune: \+([\d.]+)""")
    private val CROP_FORTUNE_LINE: Regex = Regex("""^(.+?) Fortune: \+([\d.]+)""")
    private val TIERED_BONUS_LINE: Regex = Regex("""Tiered Bonus: .*\((\d)/4\)""")
    private val GRANTS_LINE: Regex = Regex("""Grants \+([\d.]+)""")

    private const val PER_TURBO_LEVEL: Double = 5.0
    private const val TURBO_ENCHANT_PREFIX: String = "turbo_"

    private val TURBO_CROPS: Map<String, CropDrop> = mapOf(
        "wheat" to CropDrop.Wheat,
        "carrot" to CropDrop.Carrot,
        "potato" to CropDrop.Potato,
        "pumpkin" to CropDrop.Pumpkin,
        "cane" to CropDrop.SugarCane,
        "melon" to CropDrop.Melon,
        "cactus" to CropDrop.Cactus,
        "coco" to CropDrop.CocoaBeans,
        "mushrooms" to CropDrop.RedMushroom,
        "warts" to CropDrop.NetherWart
    )

    fun rawId(item: ItemStack): String? =
        item.getSkyBlockId()?.id?.substringAfter(':', "")?.takeIf { it.isNotEmpty() }?.uppercase()

    fun isFarmingTool(item: ItemStack): Boolean =
        item.getLore().any { it.string.trim() == "Farming Tool" }

    fun statLines(item: ItemStack): List<FortuneSource> {
        val name = item.hoverName.string
        return item.getLore().flatMap { line ->
            val text = line.string.trim()
            FARMING_FORTUNE_LINE.find(text)?.let { return@flatMap listOf(FortuneSource(name, FortuneStat.General, it.groupValues[1].toDouble(), "stat line")) }
            val crop = CROP_FORTUNE_LINE.find(text) ?: return@flatMap emptyList()
            FortuneCrops.byStatName(crop.groupValues[1]).map { FortuneSource(name, FortuneStat.Crop(it), crop.groupValues[2].toDouble(), "stat line") }
        }
    }

    fun armorSetBonus(armor: List<ItemStack>): FortuneSource? {
        val piece = armor.firstOrNull { item -> item.getLore().any { TIERED_BONUS_LINE.containsMatchIn(it.string) } } ?: return null
        val lore = piece.getLore().map { it.string }
        val pieces = lore.firstNotNullOfOrNull { TIERED_BONUS_LINE.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (pieces < 2) return null
        val granted = lore.firstNotNullOfOrNull { GRANTS_LINE.find(it) }?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return FortuneSource("${piece.hoverName.string} set ($pieces/4)", FortuneStat.General, granted, "tiered bonus")
    }

    fun turboFortune(tool: ItemStack): List<FortuneSource> {
        val name = tool.hoverName.string
        val coveredByLine = statLines(tool).mapNotNull { (it.stat as? FortuneStat.Crop)?.crop }.toSet()
        return tool.getData(DataTypes.ENCHANTMENTS).orEmpty().mapNotNull { (enchant, level) ->
            if (!enchant.startsWith(TURBO_ENCHANT_PREFIX) || level <= 0) return@mapNotNull null
            val crop = TURBO_CROPS[enchant.removePrefix(TURBO_ENCHANT_PREFIX)] ?: return@mapNotNull null
            if (crop in coveredByLine) return@mapNotNull null
            FortuneSource(name, FortuneStat.Crop(crop), level * PER_TURBO_LEVEL, "Turbo $level, off-crop")
        }
    }
}
