package org.magic.magicaddons.util

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack

object ItemUtils {

    fun ItemStack.getHypixelId(): String? {
        val customData = this.get(DataComponents.CUSTOM_DATA) ?: return null
        return customData.copyTag().getString("id").orElse(null)
    }

}