package org.magic.magicaddons.ui.hud

import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.EventBus
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.ui.screens.HudEditorScreen
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.compat.McCompat
import org.magic.magicaddons.Common

object HudEditorKey {

    private val hudEditorKeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("key.magicaddons.hud_editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, Common.KEY_CATEGORY)
    )

    init {
        EventBus.register(this)
    }

    @EventHandler
    fun openEditorOnKeyPress(event: WorldTickEvent) {
        while (hudEditorKeyMapping.consumeClick()) {
            if (McCompat.currentScreen() == null) ScreenUtil.setScreen(HudEditorScreen())
        }
    }
}
