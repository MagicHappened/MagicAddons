package org.magic.magicaddons.features.farming.greenhousePresets.render

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.features.farming.greenhousePresets.PlannerNeeds

object PlannerSlotHighlight {

    private const val SLOT_SIZE: Int = 16

    private const val NEEDED_ITEM_BACKGROUND: Int = 0x803F7FDF.toInt()

    @JvmStatic
    fun fillBehind(graphics: GuiGraphicsExtractor, stack: ItemStack, x: Int, y: Int) {
        if (!PlannerNeeds.isNeededByPlanner(stack)) return

        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, NEEDED_ITEM_BACKGROUND)
    }
}
