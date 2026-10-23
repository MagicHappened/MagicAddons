package org.magic.magicaddons.features.farming.fortune

import java.time.Duration
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

object FortuneMenuReaders {

    private const val ANITA_TITLE: String = "Anita"
    private const val ANITA_ITEM: String = "Extra Farming Fortune"
    private val ANITA_LINE: Regex = Regex("""You have: \+(\d+)""")

    private const val CROP_UPGRADES_TITLE: String = "Crop Upgrades"
    private val CROP_TIER_LINE: Regex = Regex("""Current Tier: (\d+)""")

    private const val CHIPS_TITLE: String = "Manage Chips"
    private val CHIP_LEVEL_LINE: Regex = Regex("""Level (\d+)/(\d+)""")

    private const val PESTHUNTER_TITLE: String = "Pesthunter"
    private const val PESTHUNTER_BONUS_ITEM: String = "Pesthunter Bonus"
    private val TIME_REMAINING_LINE: Regex = Regex("""Time Remaining: (\d+):(\d+)""")
    private val PESTHUNTER_FORTUNE_LINE: Regex = Regex("""\+(\d+) Farming Fortune""")

    private const val CARROLYN_TITLE: String = "Carrolyn's Exportable Crops"
    private val EXPORT_PROGRESS_LINE: Regex = Regex("""Export Progress: (\d+)%""")
    private val EXPORT_REWARD_LINE: Regex = Regex("""\+\d+ (.+) Fortune""")
    private const val EXPORT_COMPLETE_PERCENT: Int = 100

    private const val STATS_BREAKDOWN_TITLE: String = "Your Stats Breakdown"
    private const val FARMING_STATS_ITEM: String = "Farming Stats"
    private val SUMMARY_LINE: Regex = Regex("""^\s*(.+? Fortune) ([\d.,]+)$""")

    private const val CROP_MILESTONES_TITLE: String = "Crop Milestones"
    private val MILESTONE_ITEM: Regex = Regex("""^(.+) (\d+)$""")

    private const val CONSUMABLES_TITLE: String = "Tasks ➜ Consumables"
    private val TASK_XP_LINE: Regex = Regex("""^\s*(\d+)/(\d+) XP$""")
    private const val XP_PER_CONSUMED: Int = 2
    private val CONSUMABLE_FIELDS: Map<String, FortuneField> = mapOf(
        "Refined Dark Cacao" to FortuneField.Truffles,
        "Filled Rosewater Flask" to FortuneField.RosewaterFlasks
    )

    private val BREAKDOWN_TITLE: Regex = Regex("""^(.+) ➜ Flat Bonuses$""")
    private val VALUE_LINE: Regex = Regex("""Value: \+([\d.,]+)""")
    private val SUB_LINE: Regex = Regex("""^\s*\+([\d.,]+) (.+)$""")
    private const val GROUPED_CATEGORY: String = "Grouped"
    private const val CATEGORY_PREFIX: String = "Category: "
    private const val EXPORTATION_CATEGORY: String = "Exportation"
    private const val PET_STATS_CATEGORY: String = "Pet Stats"
    private const val GARDEN_POWER: String = "Garden Power"
    private val CROP_UPGRADE_TITLE: Regex = Regex("""^(.+) Upgrades$""")
    private const val CROPSHOT_ENTRY: String = "Cropshot"
    private const val ROSE_DRAGON: String = "Rose Dragon"
    private const val PET_EGG: String = "Egg"

    private val ICON_GLYPHS: Regex = Regex("""[-]""")

    private val GENERAL_BREAKDOWN_FIELDS: Map<String, Pair<FortuneField, Double>> = mapOf(
        "Extra Farming Fortune (Anita)" to (FortuneField.Anita to FortuneTables.PER_ANITA_LEVEL),
        "DNA Analysis Milestones" to (FortuneField.DnaMilestones to FortuneTables.PER_DNA_MILESTONE),
        "Bestiary" to (FortuneField.PestBestiary to FortuneTables.PER_PEST_BRACKET),
        "Garden Farming Fortune" to (FortuneField.Community to FortuneTables.PER_COMMUNITY_LEVEL),
        "Aged Like Fine Wine" to (FortuneField.RosewaterFlasks to 1.0),
        "Fortunate Feasting" to (FortuneField.FortunateFeasting to FortuneTables.PER_FORTUNATE_FEASTING_LEVEL)
    )

    private val CROP_BREAKDOWN_FIELDS: Map<String, Pair<FortuneField, Double>> = mapOf(
        "Anita's Personal Bests" to (FortuneField.PersonalBest to 1.0),
        "Garden Crop Upgrades" to (FortuneField.CropUpgrade to FortuneTables.PER_CROP_UPGRADE)
    )

    @Subscription
    fun onContainer(event: ContainerInitializedEvent) {
        when (event.title) {
            ANITA_TITLE -> readAnita(event.containerItems)
            CROP_UPGRADES_TITLE -> readCropUpgrades(event.containerItems)
            CHIPS_TITLE -> readChips(event.containerItems)
            PESTHUNTER_TITLE -> readPesthunter(event.containerItems)
            CARROLYN_TITLE -> readCarrolyn(event.containerItems)
            STATS_BREAKDOWN_TITLE -> readFarmingStats(event.containerItems)
            CROP_MILESTONES_TITLE -> readCropMilestones(event.containerItems)
            CONSUMABLES_TITLE -> readConsumables(event.containerItems)
            else -> {
                BREAKDOWN_TITLE.find(event.title)?.let { readBreakdown(it.groupValues[1], event.containerItems) }
                CROP_UPGRADE_TITLE.find(event.title)?.takeIf { FortuneCrops.byStatName(it.groupValues[1]).isNotEmpty() }?.let { readCropUpgrades(event.containerItems) }
            }
        }
    }

    private fun withoutIcons(text: String): String = ICON_GLYPHS.replace(text, "")

    private fun loreOf(item: ItemStack): List<String> = item.getLore().map { withoutIcons(it.string) }

    private fun entryName(text: String): String = withoutIcons(text).dropWhile { !it.isLetterOrDigit() }.trim()

    private fun nameOf(item: ItemStack): String = entryName(item.hoverName.string)

    private fun readAnita(items: List<ItemStack>) {
        val item = items.firstOrNull { nameOf(it).contains(ANITA_ITEM) } ?: return
        val fortune = loreOf(item).firstNotNullOfOrNull { ANITA_LINE.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return
        if (FortuneReadings.set(FortuneField.Anita, null, fortune / FortuneTables.PER_ANITA_LEVEL)) ChatUtils.sendWithPrefix("Anita's Extra Farming Fortune is now +$fortune.")
    }

    private fun readCropUpgrades(items: List<ItemStack>) {
        var read = 0
        items.forEach { item ->
            val crops = FortuneCrops.byStatName(nameOf(item))
            if (crops.isEmpty()) return@forEach
            val tier = loreOf(item).firstNotNullOfOrNull { CROP_TIER_LINE.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            if (crops.map { FortuneReadings.set(FortuneField.CropUpgrade, it, tier.toDouble()) }.any { it }) read++
        }
        if (read > 0) ChatUtils.sendWithPrefix("$read Garden Crop Upgrade levels changed.")
    }

    private fun readChips(items: List<ItemStack>) {
        var read = 0
        items.forEach { item ->
            val field = FortuneField.CHIPS[nameOf(item)] ?: return@forEach
            val level = loreOf(item).firstNotNullOfOrNull { CHIP_LEVEL_LINE.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            if (FortuneReadings.set(field, null, level.toDouble())) read++
        }
        if (read > 0) ChatUtils.sendWithPrefix("$read chip levels changed.")
    }

    private fun readPesthunter(items: List<ItemStack>) {
        val bonus = items.firstOrNull { nameOf(it) == PESTHUNTER_BONUS_ITEM } ?: return
        val lore = loreOf(bonus)
        val remaining = lore.firstNotNullOfOrNull { TIME_REMAINING_LINE.find(it) }
        val fortune = lore.firstNotNullOfOrNull { PESTHUNTER_FORTUNE_LINE.find(it) }?.groupValues?.get(1)?.toDoubleOrNull()
        if (remaining == null || fortune == null) {
            TemporaryFortune.clearPesthunter()
            return
        }
        val duration = Duration.ofMinutes(remaining.groupValues[1].toLong()).plusSeconds(remaining.groupValues[2].toLong())
        val wasActive = FortuneReadings.temporary(TemporaryFortune.PESTHUNTER) != null
        TemporaryFortune.notePesthunter(fortune, duration)
        if (!wasActive) ChatUtils.sendWithPrefix("Pesthunter bonus: +${fortune.toInt()} for ${remaining.groupValues[1]}m more.")
    }

    private fun readFarmingStats(items: List<ItemStack>) {
        val summary = items.firstOrNull { nameOf(it) == FARMING_STATS_ITEM } ?: return
        val totals = loreOf(summary).mapNotNull { line ->
            val match = SUMMARY_LINE.find(line) ?: return@mapNotNull null
            val value = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            entryName(match.groupValues[1]) to value
        }.toMap()
        if (totals.isEmpty()) return
        if (FortuneReadings.setStatTotals(totals, heldToolName())) ChatUtils.sendWithPrefix("Your fortune totals changed since the last reading.")
    }

    private fun readCropMilestones(items: List<ItemStack>) {
        var changed = 0
        items.forEach { item ->
            val match = MILESTONE_ITEM.find(nameOf(item)) ?: return@forEach
            val crops = FortuneCrops.byStatName(match.groupValues[1])
            val tier = match.groupValues[2].toDouble()
            if (crops.map { FortuneReadings.set(FortuneField.CropMilestone, it, tier) }.any { it }) changed++
        }
        if (changed > 0) ChatUtils.sendWithPrefix("$changed crop milestones changed.")
    }

    private fun readConsumables(items: List<ItemStack>) {
        var changed = 0
        items.forEach { item ->
            val field = CONSUMABLE_FIELDS[nameOf(item)] ?: return@forEach
            val xp = loreOf(item).firstNotNullOfOrNull { TASK_XP_LINE.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            if (FortuneReadings.set(field, null, (xp / XP_PER_CONSUMED).toDouble())) changed++
        }
        if (changed > 0) ChatUtils.sendWithPrefix("$changed consumed fortune items changed.")
    }

    fun heldToolName(): String =
        Minecraft.getInstance().player?.mainHandItem?.takeIf { ItemFortune.isFarmingTool(it) }?.hoverName?.string.orEmpty()

    private fun readCarrolyn(items: List<ItemStack>) {
        var read = 0
        items.forEach { item ->
            val lore = loreOf(item)
            val progress = lore.firstNotNullOfOrNull { EXPORT_PROGRESS_LINE.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            val cropName = lore.firstNotNullOfOrNull { EXPORT_REWARD_LINE.find(it) }?.groupValues?.get(1)?.let(::entryName) ?: return@forEach
            val crops = FortuneCrops.byStatName(cropName)
            if (crops.isEmpty()) return@forEach
            val exported = if (progress >= EXPORT_COMPLETE_PERCENT) 1.0 else 0.0
            if (crops.map { FortuneReadings.set(FortuneField.ExportableCrop, it, exported) }.any { it }) read++
        }
        Common.LOGGER.info("Carrolyn's Exportable Crops: {} items, {} exports read", items.size, read)
        if (read > 0) ChatUtils.sendWithPrefix("$read exportable crops changed.")
    }

    private fun readBreakdown(statName: String, items: List<ItemStack>) {
        val crops = if (statName == "Farming Fortune") emptyList() else FortuneCrops.byStatName(statName.removeSuffix(" Fortune"))
        if (statName != "Farming Fortune" && crops.isEmpty()) return

        val entries = items.flatMap { item ->
            val name = nameOf(item)
            val lore = loreOf(item)
            val category = lore.firstOrNull()?.trim() ?: return@flatMap emptyList()
            val value = lore.firstNotNullOfOrNull { VALUE_LINE.find(it) }?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return@flatMap emptyList()
            if (category == GROUPED_CATEGORY) {
                val groupName = name.removePrefix(CATEGORY_PREFIX)
                lore.mapNotNull { line ->
                    SUB_LINE.find(line)?.let { FortuneReadings.BreakdownEntry(groupName, entryName(it.groupValues[2]), it.groupValues[1].replace(",", "").toDouble()) }
                }
            } else {
                listOf(FortuneReadings.BreakdownEntry(category, name, value))
            }
        }
        if (entries.isEmpty()) return

        val breakdown = FortuneReadings.Breakdown(entries, System.currentTimeMillis(), ActivePet.current?.name.orEmpty(), heldToolName())
        val changed = if (crops.isEmpty()) {
            FortuneReadings.setBreakdown(FortuneStat.General, breakdown).also { fillGeneralFields(entries) }
        } else {
            crops.map { FortuneReadings.setBreakdown(FortuneStat.Crop(it), breakdown) }.any { it }.also { fillCropFields(entries, crops) }
        }
        if (changed) ChatUtils.sendWithPrefix("The $statName breakdown changed: ${entries.size} sources, +${FortuneAudit.format(breakdown.total)} in total.")
    }

    private fun fillGeneralFields(entries: List<FortuneReadings.BreakdownEntry>) {
        fillPetItem(entries)
        entries.forEach { entry ->
            when {
                entry.name == CROPSHOT_ENTRY -> (0..FortuneTables.MAX_CHIP_LEVEL).firstOrNull { FortuneTables.cropshotFortune(it) == entry.value }
                    ?.let { FortuneReadings.set(FortuneField.Cropshot, null, it.toDouble()) }
                else -> GENERAL_BREAKDOWN_FIELDS[entry.name]?.let { (field, perLevel) -> FortuneReadings.set(field, null, entry.value / perLevel) }
            }
        }
    }

    fun fillPetItem(entries: List<FortuneReadings.BreakdownEntry>) {
        val entry = entries.firstOrNull { it.category == PET_STATS_CATEGORY && it.name.contains(ROSE_DRAGON) && !it.name.contains(PET_EGG) } ?: return
        val level = roseDragonLevel(entries) ?: return
        val item = (entry.value - FortuneTables.roseDragonBaseFortune(level)).coerceAtLeast(0.0)
        if (FortuneReadings.get(FortuneField.PetItem)?.level != item) FortuneReadings.set(FortuneField.PetItem, null, item)
    }

    private fun roseDragonLevel(entries: List<FortuneReadings.BreakdownEntry>): Int? {
        ActivePet.current?.takeIf { it.name.contains(ROSE_DRAGON) && it.level > 0 }?.let { return it.level }
        val gardenPower = entries.firstOrNull { it.name == GARDEN_POWER }?.value ?: return null
        val farmingLevel = FarmingFortune.farmingLevel()?.takeIf { it > 0 } ?: return null
        return FortuneTables.roseDragonLevelFromGardenPower(gardenPower, farmingLevel)
    }

    private fun fillCropFields(entries: List<FortuneReadings.BreakdownEntry>, crops: List<CropDrop>) {
        val exported = entries.any { it.category == EXPORTATION_CATEGORY && it.value >= FortuneTables.EXPORTABLE_CROP }
        crops.forEach { crop ->
            if (crop in FortuneField.ExportableCrop.crops || crop in FortuneCrops.MUSHROOMS) {
                FortuneReadings.set(FortuneField.ExportableCrop, crop, if (exported) 1.0 else 0.0)
            }
            entries.forEach { entry ->
                val (field, perLevel) = CROP_BREAKDOWN_FIELDS[entry.name] ?: return@forEach
                FortuneReadings.set(field, crop, entry.value / perLevel)
            }
        }
    }
}
