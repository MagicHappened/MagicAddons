package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.plot.*
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

/** The greenhouse's own numbers, read off the menus that show them. */
object MenuReadings {

    fun updateCropGrowth(realItems: List<ItemStack>) {
        val sunflower = realItems.firstOrNull {
            it.item == Items.SUNFLOWER
        } ?: return

        val lore = sunflower.getLore()

        val cropGrowthLine = lore.firstOrNull { component ->
            component.siblings.any { it.string.contains("Crop Growth") }
        }

        val cropGrowth = cropGrowthLine
            ?.siblings
            ?.firstOrNull { it.string.any { c -> c.isDigit() } }
            ?.string
            ?.filter { it.isDigit() }
            ?.toIntOrNull()

        cropGrowth ?: return
        if (GreenhouseData.miscInfo.cropGrowthValue != cropGrowth) {
            ChatUtils.sendWithPrefix("Updated crop growth value to $cropGrowth")
            ChatUtils.sendWithPrefix("Make sure to open /desk again if you get more crop growth.")
            GreenhouseData.miscInfo.cropGrowthValue = cropGrowth
        }

    }

    fun updateUpgrades(realItems: List<ItemStack>) {
        val seedsStack = realItems.firstOrNull { it.item == Items.WHEAT_SEEDS }
        val plantPotStack = realItems.firstOrNull { it.item == Items.FLOWER_POT }
        val seedsLore = seedsStack?.getLore()
        val potLore = plantPotStack?.getLore()

        val speedTier = seedsLore?.firstOrNull { line ->
            line.siblings.any { it.string.contains("Current Tier") }
        }?.siblings?.firstOrNull { it.string.toIntOrNull() != null }
            ?.string
            ?.toIntOrNull()

        val yieldTier = potLore?.firstOrNull { line ->
            line.siblings.any { it.string.contains("Current Tier") }
        }?.siblings?.firstOrNull { it.string.toIntOrNull() != null }
            ?.string
            ?.toIntOrNull()

        speedTier ?: return
        yieldTier ?: return

        if (GreenhouseData.miscInfo.cropSpeedUpgradeValue != speedTier) {
            GreenhouseData.miscInfo.cropSpeedUpgradeValue = speedTier
            ChatUtils.sendWithPrefix("Updated Growth Speed Tier to $speedTier")
        }
        if (GreenhouseData.miscInfo.cropYieldUpgradeValue != yieldTier) {
            GreenhouseData.miscInfo.cropYieldUpgradeValue = yieldTier
            ChatUtils.sendWithPrefix("Updated Plant Yield Tier to $yieldTier")
        }
    }
}
