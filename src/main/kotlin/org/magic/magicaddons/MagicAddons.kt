package org.magic.magicaddons

import net.fabricmc.api.ModInitializer
import net.minecraft.client.MinecraftClient
import org.magic.magicaddons.util.LocationUtils

object MagicAddons : ModInitializer {
    val client : MinecraftClient = MinecraftClient.getInstance()

    override fun onInitialize() {

        LocationUtils.register()

    }
}
