package org.magic.magicaddons.features.farming.fortune

import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datetime.DateTimeAPI
import tech.thatgravyboat.skyblockapi.api.profile.PetsAPI
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.api.profile.items.accessory.AccessoryBagAPI
import tech.thatgravyboat.skyblockapi.api.profile.items.equipment.EquipmentAPI
import tech.thatgravyboat.skyblockapi.api.profile.skillxp.SkillExpAPI
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.HypixelSkillAPI
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockAttributesRepo

object FarmingFortune : Feature() {

    override val id: String = "FarmingFortune"
    override val displayName: String = "Farming Fortune"
    override val description: String = "Works out your Farming Fortune per crop from your gear, pet, skill, shards, garden and the menus you open."
    override val category: String = "farming"

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false
    )

    private val ARMOR_SLOTS: IntRange = 36..39

    init {
        SkyBlockAPI.eventBus.register(FortuneMenuReaders)
    }

    fun snapshot(): FortuneSnapshot {
        val sources = mutableListOf<FortuneSource>()
        val unknown = mutableListOf<String>()
        val farmingLevel = farmingLevel()

        farmingLevel?.let { sources += FortuneSource("Farming level $it", FortuneStat.General, it * FortuneTables.PER_FARMING_LEVEL) }
            ?: unknown.add("Farming level (not seen in tab yet)")

        val player = Minecraft.getInstance().player
        val armor = player?.inventory?.let { inventory -> ARMOR_SLOTS.map { inventory.getItem(it) } }.orEmpty().filter { !it.isEmpty }
        armor.forEach { sources += ItemFortune.statLines(it) }
        ItemFortune.armorSetBonus(armor)?.let { sources += it }

        EquipmentAPI.normalEquipment.values.filter { !it.isEmpty }.forEach { sources += ItemFortune.statLines(it) }

        player?.mainHandItem?.takeIf { ItemFortune.isFarmingTool(it) }?.let { tool ->
            sources += ItemFortune.statLines(tool)
            sources += ItemFortune.turboFortune(tool)
        } ?: unknown.add("Tool (hold a farming tool)")

        AccessoryBagAPI.getItems().map { it.item }.filter { !it.isEmpty }.forEach { sources += ItemFortune.statLines(it) }

        pet(farmingLevel)?.let { sources += it } ?: unknown.add("Pet ${PetsAPI.pet ?: "(none seen in tab)"}")

        sources += shards()

        PlotAPI.plots.count { !it.isBarn && it.data?.locked == false }.takeIf { it > 0 }?.let {
            sources += FortuneSource("Unlocked plots ($it)", FortuneStat.General, it.coerceAtMost(FortuneTables.MAX_PLOTS) * FortuneTables.PER_PLOT)
        } ?: unknown.add("Unlocked plots (open the desk)")

        sources += readingSources(unknown)

        return FortuneSnapshot(sources, unknown)
    }

    private fun readingSources(unknown: MutableList<String>): List<FortuneSource> = buildList {
        FortuneField.entries.forEach { field ->
            when (field.scope) {
                FieldScope.PerCrop -> {
                    val missing = FortuneCrops.representatives.filter { crop ->
                        val reading = FortuneReadings.get(field, crop) ?: return@filter true
                        FortuneCrops.byStatName(FortuneCrops.statName(crop)).forEach { statCrop ->
                            add(FortuneSource(field.displayName, FortuneStat.Crop(statCrop), field.fortune(reading.level), "${reading.level.toInt()} ${field.unit}"))
                        }
                        false
                    }
                    if (missing.isNotEmpty()) unknown += "${field.displayName}: ${missing.size} crops not read (${field.where})"
                }
                else -> {
                    val reading = FortuneReadings.get(field)
                    if (reading == null) unknown += "${field.displayName} (${field.where})"
                    else add(FortuneSource(field.displayName, field.statFor(null), field.fortune(reading.level), "${reading.level.toInt()} ${field.unit}"))
                }
            }
        }
    }

    private fun farmingLevel(): Int? =
        SkillExpAPI.skills[HypixelSkillAPI.Skill.FARMING]?.toInt()?.coerceAtMost(FortuneTables.MAX_FARMING_LEVEL)

    private fun pet(farmingLevel: Int?): FortuneSource? {
        val name = PetsAPI.pet ?: return null
        val level = PetsAPI.level
        val rarity = PetsAPI.rarity
        val label = "Pet: $name $level"

        return when {
            name.contains("Elephant", ignoreCase = true) -> when (rarity) {
                SkyBlockRarity.MYTHIC -> FortuneSource(label, FortuneStat.General, level * 2.0, "Trunk Efficiency 1.5 + 0.5 per level")
                SkyBlockRarity.LEGENDARY -> FortuneSource(label, FortuneStat.General, level * 1.5, "Trunk Efficiency")
                else -> FortuneSource(label, FortuneStat.General, 0.0)
            }
            name.contains("Mooshroom", ignoreCase = true) -> FortuneSource(label, FortuneStat.General, level * 1.0, "1 per level, strength bonus not counted")
            name.contains("Chicken", ignoreCase = true) -> FortuneSource(label, FortuneStat.General, level * 0.5, "0.5 per level")
            name.contains("Bee", ignoreCase = true) -> {
                val perLevel = when (rarity) {
                    SkyBlockRarity.RARE -> 0.2
                    SkyBlockRarity.EPIC, SkyBlockRarity.LEGENDARY -> 0.3
                    SkyBlockRarity.MYTHIC -> 0.4
                    else -> 0.0
                }
                FortuneSource(label, FortuneStat.General, level * perLevel, "Busy Buzz Buzz")
            }
            name.contains("Rose Dragon", ignoreCase = true) -> {
                val base = if (level >= 101) level * 0.2 else 0.0
                val gardenPower = (farmingLevel ?: 0) * 3.0 * level / 200
                FortuneSource(label, FortuneStat.General, base + gardenPower, "Garden Power, Rosy Scales not counted")
            }
            else -> FortuneSource(label, FortuneStat.General, 0.0)
        }
    }

    private fun shards(): List<FortuneSource> {
        val hasPest = (PlotAPI.getCurrentPlot()?.data?.pest?.pest ?: 0) > 0
        val sources = mutableListOf<FortuneSource>()

        AttributeAPI.attributeMap.forEach { (id, data) ->
            val level = data.level.takeIf { it > 0 } ?: return@forEach
            val shard = SkyBlockAttributesRepo.get(id.id)
                ?: SkyBlockAttributesRepo.get(id.id.substringAfter(':'))
                ?: return@forEach
            val label = "${shard.shardName()} shard $level"

            when (shard.shardName().lowercase()) {
                "fly" -> sources += FortuneSource(label, FortuneStat.General, level * FortuneTables.PER_FLY_LEVEL)
                "firefly" -> if (DateTimeAPI.isDay) sources += FortuneSource(label, FortuneStat.General, level * FortuneTables.PER_FIREFLY_LEVEL, "day")
                "lunar moth" -> if (DateTimeAPI.isNight) sources += FortuneSource(label, FortuneStat.General, level * FortuneTables.PER_LUNAR_MOTH_LEVEL, "night")
                "earthworm" -> if (hasPest) sources += FortuneSource(label, FortuneStat.General, level * FortuneTables.PER_EARTHWORM_LEVEL, "pest in plot")
                "galaxy fish" -> sources += FortuneSource(label, FortuneStat.General, level * FortuneTables.PER_GALAXY_FISH_LEVEL)
            }
        }
        return sources
    }

    fun cropsToAudit(): List<CropDrop> = FortuneCrops.representatives
}
