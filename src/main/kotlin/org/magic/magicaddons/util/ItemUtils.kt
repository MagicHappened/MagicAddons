package org.magic.magicaddons.util

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack

object ItemUtils {

    fun ItemStack.getHypixelId(): String? {
        val customData = this.get(DataComponentTypes.CUSTOM_DATA) ?: return null
        return customData.copyNbt().getString("id").orElse(null)
    }

}