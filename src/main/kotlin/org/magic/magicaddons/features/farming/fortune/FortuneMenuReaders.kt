package org.magic.magicaddons.features.farming.fortune

import net.minecraft.world.item.ItemStack
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

    @Subscription
    fun onContainer(event: ContainerInitializedEvent) {
        when (event.title) {
            ANITA_TITLE -> readAnita(event.containerItems)
            CROP_UPGRADES_TITLE -> readCropUpgrades(event.containerItems)
        }
    }

    private fun readAnita(items: List<ItemStack>) {
        val item = items.firstOrNull { it.hoverName.string.contains(ANITA_ITEM) } ?: return
        val fortune = item.getLore().firstNotNullOfOrNull { ANITA_LINE.find(it.string) }?.groupValues?.get(1)?.toIntOrNull() ?: return
        val level = fortune / FortuneTables.PER_ANITA_LEVEL
        FortuneReadings.set(FortuneField.Anita, null, level, FortuneReadings.ORIGIN_MENU)
        ChatUtils.sendWithPrefix("Read Anita's Extra Farming Fortune: +$fortune.")
    }

    private fun readCropUpgrades(items: List<ItemStack>) {
        var read = 0
        items.forEach { item ->
            val name = item.hoverName.string.trim()
            val crops = FortuneCrops.representatives.filter { crop -> name == FortuneCrops.statName(crop) }
            if (crops.isEmpty()) return@forEach
            val tier = item.getLore().firstNotNullOfOrNull { CROP_TIER_LINE.find(it.string) }?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            crops.forEach { FortuneReadings.set(FortuneField.CropUpgrade, it, tier.toDouble(), FortuneReadings.ORIGIN_MENU) }
            read++
        }
        if (read > 0) ChatUtils.sendWithPrefix("Read $read Garden Crop Upgrade levels.")
    }
}
