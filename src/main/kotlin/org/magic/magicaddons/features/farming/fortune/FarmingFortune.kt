package org.magic.magicaddons.features.farming.fortune

import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datetime.DateTimeAPI
import tech.thatgravyboat.skyblockapi.api.profile.PetsAPI
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.api.profile.items.accessory.AccessoryBagAPI
import tech.thatgravyboat.skyblockapi.api.profile.items.equipment.EquipmentAPI
import tech.thatgravyboat.skyblockapi.api.profile.skillxp.SkillExpAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.RepoAttributeAPI
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.HypixelSkillAPI

object FarmingFortune : Feature() {

    override val id: String = "FarmingFortune"
    override val displayName: String = "Farming Fortune"
    override val description: String = "Works out your Farming Fortune from your gear, pet, skill, shards and garden."
    override val category: String = "farming"

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false
    )

    private val MENU_SOURCES: List<String> = listOf(
        "Anita's Extra Farming Fortune",
        "Pest bestiary",
        "DNA analysis milestones",
        "Community Center farming fortune",
        "Cropshot chip",
        "Rosewater flasks",
        "Pet item",
        "Active effects (cake, potions, contest)"
    )

    fun snapshot(): FortuneSnapshot {
        val sources = mutableListOf<FortuneSource>()
        val unknown = MENU_SOURCES.toMutableList()
        val farmingLevel = farmingLevel()

        farmingLevel?.let { sources += FortuneSource("Farming level $it", it * FortuneTables.PER_FARMING_LEVEL) }
            ?: unknown.add("Farming level (not seen in tab yet)")

        val player = Minecraft.getInstance().player
        val armor = player?.inventory?.let { inventory -> (36..39).map { inventory.getItem(it) } }.orEmpty().filter { !it.isEmpty }
        armor.forEach { piece ->
            val read = ItemFortune.armor(piece, farmingLevel ?: 0)
            sources += read.sources
            unknown += read.unknown
        }
        sources += armorSetBonuses(armor)

        EquipmentAPI.normalEquipment.values.filter { !it.isEmpty }.forEach { piece ->
            val read = ItemFortune.equipment(piece)
            sources += read.sources
            unknown += read.unknown
        }

        player?.mainHandItem?.takeIf { ItemFortune.isFarmingTool(it) }?.let { tool ->
            sources += ItemFortune.tool(tool).sources
        } ?: unknown.add("Tool (hold a farming tool)")

        AccessoryBagAPI.getItems().map { it.item }.filter { !it.isEmpty }.forEach { sources += ItemFortune.accessory(it).sources }

        pet(farmingLevel)?.let { sources += it } ?: unknown.add("Pet ${PetsAPI.pet ?: "(none seen in tab)"}")

        sources += shards()

        PlotAPI.plots.count { !it.isBarn && it.data?.locked == false }.takeIf { it > 0 }?.let {
            sources += FortuneSource("Unlocked plots ($it)", it.coerceAtMost(FortuneTables.MAX_PLOTS) * FortuneTables.PER_PLOT)
        } ?: unknown.add("Unlocked plots (open the desk)")

        return FortuneSnapshot(sources, unknown)
    }

    private fun farmingLevel(): Int? =
        SkillExpAPI.skills[HypixelSkillAPI.Skill.FARMING]?.toInt()?.coerceAtMost(FortuneTables.MAX_FARMING_LEVEL)

    private fun armorSetBonuses(armor: List<ItemStack>): List<FortuneSource> =
        armor.mapNotNull { ItemFortune.rawId(it)?.let(ItemFortune::armorSetOf)?.first }
            .groupingBy { it }.eachCount()
            .mapNotNull { (set, pieces) ->
                val bonus = set.setBonus ?: return@mapNotNull null
                if (pieces < 2) return@mapNotNull null
                FortuneSource("${set.displayName} set ($pieces pieces)", bonus[pieces - 2])
            }

    private fun pet(farmingLevel: Int?): FortuneSource? {
        val name = PetsAPI.pet ?: return null
        val level = PetsAPI.level
        val rarity = PetsAPI.rarity
        val label = "Pet: $name $level"

        return when {
            name.contains("Elephant", ignoreCase = true) -> when (rarity) {
                SkyBlockRarity.MYTHIC -> FortuneSource(label, level * 2.0, "Trunk Efficiency 1.5 + 0.5 per level")
                SkyBlockRarity.LEGENDARY -> FortuneSource(label, level * 1.5, "Trunk Efficiency")
                else -> FortuneSource(label, 0.0)
            }
            name.contains("Mooshroom", ignoreCase = true) -> FortuneSource(label, level * 1.0, "1 per level, strength bonus not counted")
            name.contains("Chicken", ignoreCase = true) -> FortuneSource(label, level * 0.5, "0.5 per level")
            name.contains("Bee", ignoreCase = true) -> {
                val perLevel = when (rarity) {
                    SkyBlockRarity.RARE -> 0.2
                    SkyBlockRarity.EPIC, SkyBlockRarity.LEGENDARY -> 0.3
                    SkyBlockRarity.MYTHIC -> 0.4
                    else -> 0.0
                }
                FortuneSource(label, level * perLevel, "Busy Buzz Buzz")
            }
            name.contains("Rose Dragon", ignoreCase = true) -> {
                val base = if (level >= 101) level * 0.2 else 0.0
                val gardenPower = (farmingLevel ?: 0) * 3.0 * level / 200
                FortuneSource(label, base + gardenPower, "Garden Power, Rosy Scales not counted")
            }
            else -> FortuneSource(label, 0.0)
        }
    }

    private fun shards(): List<FortuneSource> {
        val hasPest = (PlotAPI.getCurrentPlot()?.data?.pest?.pest ?: 0) > 0
        val sources = mutableListOf<FortuneSource>()

        AttributeAPI.attributeMap.forEach { (id, data) ->
            val level = data.level.takeIf { it > 0 } ?: return@forEach
            val shard = RepoAttributeAPI.getAttributeDataById(id.id)
                ?: RepoAttributeAPI.getAttributeDataById(id.id.substringAfter(':'))
                ?: return@forEach
            val label = "${shard.shardName()} shard $level"

            when (shard.shardName().lowercase()) {
                "fly" -> sources += FortuneSource(label, level * FortuneTables.PER_FLY_LEVEL)
                "firefly" -> if (DateTimeAPI.isDay) sources += FortuneSource(label, level * FortuneTables.PER_FIREFLY_LEVEL, "day")
                "lunar moth" -> if (DateTimeAPI.isNight) sources += FortuneSource(label, level * FortuneTables.PER_LUNAR_MOTH_LEVEL, "night")
                "earthworm" -> if (hasPest) sources += FortuneSource(label, level * FortuneTables.PER_EARTHWORM_LEVEL, "pest in plot")
                "galaxy fish" -> sources += FortuneSource(label, level * FortuneTables.PER_GALAXY_FISH_LEVEL)
            }
        }
        return sources
    }
}
