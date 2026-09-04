package org.magic.magicaddons.features.farming.greenhousePresets

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.compat.McCompat
import org.magic.magicaddons.util.ScreenUtil

/** The key that opens the greenhouse screen, G unless rebound in the controls menu. */
object GreenhouseKey {

    private val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Common.MOD_ID, Common.MOD_ID))

    private val key = KeyMappingHelper.registerKeyMapping(
        KeyMapping("key.magicaddons.greenhouse", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category)
    )

    init {
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            while (key.consumeClick()) open(mc)
        }
    }

    private fun open(mc: Minecraft) {
        if (McCompat.currentScreen() != null) return
        if (!GreenhousePresets.baseSetting.value) return
        if (!GreenhousePresets.keyWorksAnywhere() && !GreenhouseData.inGreenhouse()) return

        ScreenUtil.setScreen(GreenhouseScreen(Component.literal("GreenhouseScreen")))
    }
}
