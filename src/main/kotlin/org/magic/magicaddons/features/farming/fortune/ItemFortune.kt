package org.magic.magicaddons.features.farming.fortune

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.features.farming.fortune.FortuneTables.ArmorPiece
import org.magic.magicaddons.features.farming.fortune.FortuneTables.ArmorSet
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.defaults.Gemstone
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

object ItemFortune {

    private const val DUMMIES_TAG: String = "farming_for_dummies_count"

    private val PIECE_BONUS: Regex = Regex("""Piece Bonus:\s*\+(\d+(?:\.\d+)?)""")

    class Read(val sources: List<FortuneSource>, val unknown: List<String>)

    fun rawId(item: ItemStack): String? =
        item.getSkyBlockId()?.id?.substringAfter(':', "")?.takeIf { it.isNotEmpty() }?.uppercase()

    fun armorSetOf(id: String): Pair<ArmorSet, ArmorPiece>? {
        val piece = ArmorPiece.entries.firstOrNull { id.endsWith(it.idSuffix) } ?: return null
        val set = FortuneTables.ARMOR_SETS.firstOrNull { id == it.idPrefix + piece.idSuffix } ?: return null
        return set to piece
    }

    fun armor(item: ItemStack, farmingLevel: Int): Read {
        val id = rawId(item) ?: return Read(emptyList(), emptyList())
        val name = item.hoverName.string
        val sources = mutableListOf<FortuneSource>()

        armorSetOf(id)?.let { (set, piece) -> sources += FortuneSource(name, set.fortuneByPiece.getValue(piece), "base") }
        if (id in FortuneTables.FARMING_LEVEL_ARMOR) sources += FortuneSource(name, farmingLevel.toDouble(), "1 per farming level")

        sources += reforge(item, name)
        sources += peridot(item, name, half = false)
        enchantLevel(item, "pesterminator")?.let {
            sources += FortuneSource(name, it * FortuneTables.PER_PESTERMINATOR_LEVEL, "Pesterminator $it")
        }
        return Read(sources, emptyList())
    }

    fun equipment(item: ItemStack): Read {
        val id = rawId(item) ?: return Read(emptyList(), emptyList())
        val name = item.hoverName.string
        val sources = mutableListOf<FortuneSource>()
        val unknown = mutableListOf<String>()

        FortuneTables.EQUIPMENT_BASE[id]?.let { sources += FortuneSource(name, it, "base") }
        pieceBonus(item)?.let { sources += FortuneSource(name, it, "piece bonus") }
        sources += reforge(item, name)
        enchantLevel(item, "green_thumb")?.let { unknown += "$name: Green Thumb $it needs the unique visitor count" }
        return Read(sources, unknown)
    }

    fun isFarmingTool(item: ItemStack): Boolean {
        val id = rawId(item) ?: return false
        return FortuneTables.FARMING_TOOL_PREFIXES.any { id.startsWith(it) } || id in FortuneTables.VACUUM_BASE
    }

    fun tool(item: ItemStack): Read {
        val id = rawId(item) ?: return Read(emptyList(), emptyList())
        val name = item.hoverName.string
        val sources = mutableListOf<FortuneSource>()

        (FortuneTables.GARDENING_TOOL_BASE[id] ?: FortuneTables.VACUUM_BASE[id])?.let { sources += FortuneSource(name, it, "base") }
        sources += reforge(item, name)
        sources += peridot(item, name, half = false)
        enchantLevel(item, "harvesting")?.let { sources += FortuneSource(name, it * FortuneTables.PER_HARVESTING_LEVEL, "Harvesting $it") }
        enchantLevel(item, "cultivating")?.let { sources += FortuneSource(name, it * FortuneTables.PER_CULTIVATING_LEVEL, "Cultivating $it") }
        dummies(item).takeIf { it > 0 }?.let {
            sources += FortuneSource(name, it * FortuneTables.PER_FARMING_FOR_DUMMIES, "Farming for Dummies $it")
        }
        return Read(sources, emptyList())
    }

    fun accessory(item: ItemStack): Read {
        val id = rawId(item) ?: return Read(emptyList(), emptyList())
        val name = item.hoverName.string
        val sources = mutableListOf<FortuneSource>()

        if (id == "HELIANTHUS_RELIC") sources += FortuneSource(name, FortuneTables.HELIANTHUS_RELIC)
        sources += peridot(item, name, half = true)
        return Read(sources, emptyList())
    }

    private fun rarity(item: ItemStack): SkyBlockRarity = item.getData(DataTypes.RARITY) ?: SkyBlockRarity.COMMON

    private fun reforge(item: ItemStack, name: String): List<FortuneSource> {
        val modifier = item.getData(DataTypes.MODIFIER) ?: return emptyList()
        val value = FortuneTables.reforgeFortune(modifier, rarity(item)) ?: return emptyList()
        return listOf(FortuneSource(name, value, modifier.replace('_', ' ')))
    }

    private fun peridot(item: ItemStack, name: String, half: Boolean): List<FortuneSource> {
        val rarity = rarity(item)
        return item.getData(DataTypes.GEMSTONES).orEmpty()
            .filter { it.gemstone == Gemstone.PERIDOT }
            .map { gem ->
                val value = FortuneTables.peridotFortune(gem.quality, rarity)
                FortuneSource(name, if (half) value / 2 else value, "${gem.quality.name.lowercase()} peridot")
            }
    }

    private fun enchantLevel(item: ItemStack, enchant: String): Int? =
        item.getData(DataTypes.ENCHANTMENTS)?.get(enchant)?.takeIf { it > 0 }

    private fun dummies(item: ItemStack): Int {
        val tag = item.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return 0
        return tag.getInt(DUMMIES_TAG).orElse(0).coerceAtMost(FortuneTables.MAX_FARMING_FOR_DUMMIES)
    }

    private fun pieceBonus(item: ItemStack): Double? =
        item.getLore().asReversed().firstNotNullOfOrNull { PIECE_BONUS.find(it.string) }?.groupValues?.get(1)?.toDoubleOrNull()
}
