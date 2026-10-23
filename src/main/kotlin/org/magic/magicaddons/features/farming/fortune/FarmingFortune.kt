package org.magic.magicaddons.features.farming.fortune

import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.events.EventBus
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.area.hub.ElectionAPI
import tech.thatgravyboat.skyblockapi.api.datetime.DateTimeAPI
import tech.thatgravyboat.skyblockapi.api.datetime.SkyBlockSeason
import tech.thatgravyboat.skyblockapi.api.profile.PetsAPI
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.api.profile.items.accessory.AccessoryBagAPI
import tech.thatgravyboat.skyblockapi.api.profile.items.equipment.EquipmentAPI
import tech.thatgravyboat.skyblockapi.api.profile.skillxp.SkillExpAPI
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.HypixelSkillAPI
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockAttributesRepo

object FarmingFortune {

    private val ARMOR_SLOTS: IntRange = 36..39

    const val SKILL_CATEGORY: String = "Skill"
    const val ARMOR_CATEGORY: String = "Armor"
    const val ARMOR_PASSIVE_CATEGORY: String = "Armor (Passive)"
    const val EQUIPMENT_CATEGORY: String = "Equipment"
    const val HELD_ITEM_CATEGORY: String = "Held Item"
    const val ACCESSORY_CATEGORY: String = "Accessory Bag"
    const val ATTRIBUTE_CATEGORY: String = "Attribute"
    const val CARNIVAL_CATEGORY: String = "Carnival Perk"
    const val PLOTS_ENTRY: String = "Garden Plots"

    val LIVE_CATEGORIES: Set<String> = setOf(
        SKILL_CATEGORY, ARMOR_CATEGORY, ARMOR_PASSIVE_CATEGORY, EQUIPMENT_CATEGORY, HELD_ITEM_CATEGORY, ACCESSORY_CATEGORY, ATTRIBUTE_CATEGORY, CARNIVAL_CATEGORY, "Farming Tool", "Tool", "Weapon"
    )

    private val TEMPORARY_ENTRIES: Map<String, String> = mapOf(
        TemporaryFortune.PESTHUNTER to TemporaryFortune.PESTHUNTER,
        TemporaryFortune.CENTURY_CAKE to TemporaryFortune.CENTURY_CAKE
    )

    private const val MYTHIC_BEE_CROP_FORTUNE_PER_LEVEL: Double = 1.6
    private val MYTHIC_BEE_CROPS: List<CropDrop> = listOf(CropDrop.Sunflower, CropDrop.Moonflower, CropDrop.WildRose)
    private const val MOSQUITO_FORTUNE_PER_LEVEL_PER_VISITOR: Double = 0.02
    private const val MOSQUITO_MAX_FORTUNE: Double = 175.0

    fun init() {
        SkyBlockAPI.eventBus.register(FortuneMenuReaders)
        SkyBlockAPI.eventBus.register(TemporaryFortune)
        EventBus.register(TemporaryFortune)
        EventBus.register(GardenVisitors)
        EventBus.register(FortuneChatReaders)
        SkyBlockAPI.eventBus.register(ActivePet)
        EventBus.register(ActivePet)
    }

    fun snapshot(): FortuneSnapshot {
        val sources = mutableListOf<FortuneSource>()
        val unknown = mutableListOf<String>()
        val farmingLevel = farmingLevel()

        farmingLevel?.let { sources += FortuneSource("Farming level $it", FortuneStat.General, it * FortuneTables.PER_FARMING_LEVEL, category = SKILL_CATEGORY) }
            ?: unknown.add("Farming level (not seen in tab yet)")

        val player = Minecraft.getInstance().player
        val armor = player?.inventory?.let { inventory -> ARMOR_SLOTS.map { inventory.getItem(it) } }.orEmpty().filter { !it.isEmpty }
        armor.forEach { sources += ItemFortune.statLines(it).map { source -> source.copy(category = ARMOR_CATEGORY) } }
        ItemFortune.armorSetBonus(armor)?.let { sources += it.copy(category = ARMOR_PASSIVE_CATEGORY) }

        EquipmentAPI.normalEquipment.values.filter { !it.isEmpty }.forEach { sources += ItemFortune.statLines(it).map { source -> source.copy(category = EQUIPMENT_CATEGORY) } }

        player?.mainHandItem?.takeIf { ItemFortune.isFarmingTool(it) }?.let { tool ->
            sources += ItemFortune.statLines(tool).map { source -> source.copy(category = HELD_ITEM_CATEGORY) }
            sources += ItemFortune.turboFortune(tool)
            sources += ItemFortune.dedicationFortune(tool)
        } ?: unknown.add("Tool (hold a farming tool)")

        AccessoryBagAPI.getItems().map { it.item }.filter { !it.isEmpty }.forEach { sources += ItemFortune.statLines(it).map { source -> source.copy(category = ACCESSORY_CATEGORY) } }

        sources += shards().map { it.copy(category = ATTRIBUTE_CATEGORY) }
        fortunateFeasting()?.let { sources += it }

        PlotAPI.plots.count { !it.isBarn && it.data?.locked == false }.takeIf { it > 0 }?.let {
            sources += FortuneSource("Unlocked plots ($it)", FortuneStat.General, it.coerceAtMost(FortuneTables.MAX_PLOTS) * FortuneTables.PER_PLOT, category = PLOTS_ENTRY)
        } ?: unknown.add("Unlocked plots (open the desk)")

        val generalBreakdown = FortuneReadings.breakdown(FortuneStat.General)
        if (generalBreakdown != null) {
            sources += breakdownSources(generalBreakdown, FortuneStat.General)
            FortuneMenuReaders.fillPetItem(generalBreakdown.entries)
            if (generalBreakdown.petName != ActivePet.current?.name.orEmpty()) unknown += "Pet changed since the breakdown was read (${generalBreakdown.petName.ifEmpty { "none" }} then)"
        } else {
            pet(farmingLevel)?.let { sources += it } ?: unknown.add("Pet (open the Pets menu or enable the Pet tab widget)")
            sources += readingSources(unknown, generalOnly = true)
            sources += temporarySources()
            unknown += "Stats breakdown not read (SkyBlock Menu, Your Stats, Farming Fortune, Flat Bonuses)"
        }

        sources += readingSources(unknown, generalOnly = false)
        CropDrop.entries.forEach { crop ->
            TemporaryFortune.overdriveFortune(crop).takeIf { it > 0 }?.let { sources += FortuneSource("Overdrive chip (contest)", FortuneStat.Crop(crop), it) }
            TemporaryFortune.anitaContestFortune(crop).takeIf { it > 0 }?.let { sources += FortuneSource("Anita accessory (contest)", FortuneStat.Crop(crop), it) }
        }
        sources += petCropFortune()
        FortuneReadings.temporary(TemporaryFortune.CACAO_TRUFFLE)?.let {
            sources += FortuneSource(TemporaryFortune.CACAO_TRUFFLE, FortuneStat.Crop(CropDrop.CocoaBeans), it.value, "temporary")
        }

        return FortuneSnapshot(sources, unknown)
    }

    private fun petCropFortune(): List<FortuneSource> {
        val pet = ActivePet.current ?: return emptyList()
        val name = pet.name
        val level = pet.level
        val label = "Pet: $name $level"
        return when {
            name.contains("Bee", ignoreCase = true) && pet.rarity == SkyBlockRarity.MYTHIC -> MYTHIC_BEE_CROPS.map {
                FortuneSource(label, FortuneStat.Crop(it), level * MYTHIC_BEE_CROP_FORTUNE_PER_LEVEL, "Powered by Pollen")
            }
            name.contains("Mosquito", ignoreCase = true) && pet.rarity?.let { it >= SkyBlockRarity.RARE } != false -> {
                val visitors = FortuneReadings.get(FortuneField.UniqueVisitors)?.level ?: return emptyList()
                val fortune = (level * MOSQUITO_FORTUNE_PER_LEVEL_PER_VISITOR * visitors).coerceAtMost(MOSQUITO_MAX_FORTUNE)
                listOf(FortuneSource(label, FortuneStat.Crop(CropDrop.SugarCane), fortune, "Buzzin' Barterer, ${visitors.toInt()} visitors"))
            }
            else -> emptyList()
        }
    }

    private val HARVEST_FEAST_SEASONS: Set<SkyBlockSeason> = setOf(SkyBlockSeason.EARLY_AUTUMN, SkyBlockSeason.AUTUMN, SkyBlockSeason.LATE_AUTUMN)

    fun isFeastActive(): Boolean {
        if (DateTimeAPI.season in HARVEST_FEAST_SEASONS) return true
        return listOfNotNull(ElectionAPI.mayor, ElectionAPI.minister).any { candidate -> candidate.activePerks.any { it.perkName.contains("Feast", ignoreCase = true) } }
    }

    private fun fortunateFeasting(): FortuneSource? {
        val level = FortuneReadings.get(FortuneField.FortunateFeasting)?.level ?: return null
        if (!isFeastActive()) return null
        return FortuneSource("Fortunate Feasting ${level.toInt()}", FortuneStat.General, level * FortuneTables.PER_FORTUNATE_FEASTING_LEVEL, "feast", CARNIVAL_CATEGORY)
    }

    private fun breakdownSources(breakdown: FortuneReadings.Breakdown, stat: FortuneStat): List<FortuneSource> =
        breakdown.entries.filter { it.category !in LIVE_CATEGORIES && it.name != PLOTS_ENTRY }.mapNotNull { entry ->
            TEMPORARY_ENTRIES[entry.name]?.let { if (FortuneReadings.isKnownInactive(it)) return@mapNotNull null }
            FortuneSource(entry.name, stat, entry.value, "${entry.category}, breakdown")
        }

    private fun temporarySources(): List<FortuneSource> =
        FortuneReadings.activeTemporaries().filterKeys { it != TemporaryFortune.CACAO_TRUFFLE }.map { (name, temporary) -> FortuneSource(name, FortuneStat.General, temporary.value, "temporary") }

    private fun readingSources(unknown: MutableList<String>, generalOnly: Boolean): List<FortuneSource> = buildList {
        FortuneField.entries.forEach { field ->
            when (field.scope) {
                FieldScope.Info -> Unit
                FieldScope.General -> if (generalOnly) {
                    val reading = FortuneReadings.get(field)
                    if (reading == null) unknown += "${field.displayName} (${field.where})"
                    else add(FortuneSource(field.displayName, FortuneStat.General, field.fortune(reading.level), "${reading.level.toInt()} ${field.unit}"))
                }
                FieldScope.Cocoa -> if (!generalOnly) {
                    val reading = FortuneReadings.get(field)
                    if (reading == null) unknown += "${field.displayName} (${field.where})"
                    else add(FortuneSource(field.displayName, field.statFor(null), field.fortune(reading.level), "${reading.level.toInt()} ${field.unit}"))
                }
                FieldScope.PerCrop -> if (!generalOnly) {
                    val missing = field.crops.filter { crop ->
                        val reading = FortuneReadings.get(field, crop) ?: return@filter true
                        FortuneCrops.byStatName(FortuneCrops.statName(crop)).forEach { statCrop ->
                            add(FortuneSource(field.displayName, FortuneStat.Crop(statCrop), field.fortune(reading.level), "${reading.level.toInt()} ${field.unit}"))
                        }
                        false
                    }
                    if (missing.isNotEmpty()) unknown += "${field.displayName}: ${missing.size} crops not read (${field.where})"
                }
            }
        }
    }

    fun farmingLevel(): Int? =
        SkillExpAPI.skills[HypixelSkillAPI.Skill.FARMING]?.toInt()?.coerceAtMost(FortuneTables.MAX_FARMING_LEVEL)

    private fun pet(farmingLevel: Int?): FortuneSource? {
        val pet = ActivePet.current ?: return null
        val name = pet.name
        val level = pet.level
        val rarity = pet.rarity
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
                if (name.contains("Egg", ignoreCase = true)) return FortuneSource(label, FortuneStat.General, 0.0)
                val gardenPower = (farmingLevel ?: 0) * 3.0 * level / 200
                FortuneSource(label, FortuneStat.General, FortuneTables.roseDragonBaseFortune(level) + gardenPower, "base and Garden Power, Rosy Scales not counted")
            }
            else -> FortuneSource(label, FortuneStat.General, 0.0)
        }
    }

    private fun shards(): List<FortuneSource> {
        val hasPest = (PlotAPI.getCurrentPlot()?.data?.pest?.pest ?: 0) > 0
        val sources = mutableListOf<FortuneSource>()

        val level = Minecraft.getInstance().level

        AttributeAPI.attributeMap.forEach { (id, data) ->
            val attributeLevel = data.level.takeIf { it > 0 } ?: return@forEach
            val attribute = SkyBlockAttributesRepo.get(id.id)
                ?: SkyBlockAttributesRepo.get(id.id.substringAfter(':'))
                ?: return@forEach
            val name = attribute.name()
            val label = "$name $attributeLevel"

            when (name) {
                "Fortunate Farmer" -> sources += FortuneSource(label, FortuneStat.General, attributeLevel * FortuneTables.PER_FLY_LEVEL)
                "Solar Power" -> if (level?.isBrightOutside == true) sources += FortuneSource(label, FortuneStat.General, attributeLevel * FortuneTables.PER_FIREFLY_LEVEL, "day")
                "Lunar Power" -> if (level?.isDarkOutside == true) sources += FortuneSource(label, FortuneStat.General, attributeLevel * FortuneTables.PER_LUNAR_MOTH_LEVEL, "night")
                "Infiltration" -> if (hasPest) sources += FortuneSource(label, FortuneStat.General, attributeLevel * FortuneTables.PER_EARTHWORM_LEVEL, "pest in plot")
                "Ultimate DNA" -> sources += FortuneSource(label, FortuneStat.General, attributeLevel * FortuneTables.PER_GALAXY_FISH_LEVEL)
            }
        }
        return sources
    }
}
