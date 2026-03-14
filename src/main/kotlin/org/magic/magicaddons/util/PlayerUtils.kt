package org.magic.magicaddons.util

import net.minecraft.client.MinecraftClient

object PlayerUtils {

    var wearingSpecialArmor = false
        private set

    fun updateArmorState() {
        val player = MinecraftClient.getInstance().player ?: return


    }
}