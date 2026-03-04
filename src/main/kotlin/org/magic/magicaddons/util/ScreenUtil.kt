package org.magic.magicaddons.util

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen

object ScreenUtil {


    private var newScreen: Screen? = null

    fun setScreen(screen: Screen) {
        newScreen = screen
    }

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            val target = newScreen ?: return@register

            if (MinecraftClient.getInstance().currentScreen !== target) {
                MinecraftClient.getInstance().setScreen(target)
            } else {
                newScreen = null // stop forcing
            }
        }
    }
}