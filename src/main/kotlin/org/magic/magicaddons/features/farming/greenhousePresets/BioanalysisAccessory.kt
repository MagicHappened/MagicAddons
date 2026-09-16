package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.profile.items.accessory.AccessoryBagAPI
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

object BioanalysisAccessory {

    // the lore wraps mid-sentence, so the percent and the word it belongs to sit on separate lines
    private val MUTATE_THEN_PERCENT: Regex = Regex("""mutat\w*[^%]{0,40}?(\d+(?:\.\d+)?)\s*%""", RegexOption.IGNORE_CASE)
    private val PERCENT_THEN_MUTATE: Regex = Regex("""(\d+(?:\.\d+)?)\s*%[^%]{0,40}?mutat""", RegexOption.IGNORE_CASE)

    fun mutationWeightMultiplier(): Double {
        val inventoryItems = Minecraft.getInstance().player?.inventory?.let { inventory ->
            (0 until inventory.containerSize).map { inventory.getItem(it) }
        }.orEmpty()
        val accessoryBagItems = AccessoryBagAPI.getItems().map { it.item }

        val bestPercent = (accessoryBagItems + inventoryItems).maxOfOrNull { mutationChanceOf(it) } ?: 0.0
        return 1.0 + bestPercent / 100.0
    }

    private fun mutationChanceOf(item: ItemStack): Double {
        if (item.isEmpty) return 0.0
        val lore = item.getLore().joinToString(" ") { it.string }

        return (MUTATE_THEN_PERCENT.find(lore) ?: PERCENT_THEN_MUTATE.find(lore))
            ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 0.0
    }
}
