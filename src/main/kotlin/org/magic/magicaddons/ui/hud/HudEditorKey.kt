package org.magic.magicaddons.ui.hud

import org.magic.magicaddons.util.ErrorReporter.guard
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseKey
import org.magic.magicaddons.ui.screens.HudEditorScreen
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.compat.McCompat

/** The key that opens the hud editor, unbound until set in the controls menu. */
object HudEditorKey {

    private val key = KeyMappingHelper.registerKeyMapping(
        KeyMapping("key.magicaddons.hud_editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, GreenhouseKey.category)
    )

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            guard("the hud editor key") {
                while (key.consumeClick()) {
                    if (McCompat.currentScreen() == null) ScreenUtil.setScreen(HudEditorScreen())
                }
            }
        }
    }
}
