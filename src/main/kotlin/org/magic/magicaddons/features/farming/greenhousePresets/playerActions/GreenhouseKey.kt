package org.magic.magicaddons.features.farming.greenhousePresets.playerActions

import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.events.EventHandler
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.compat.McCompat
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData

object GreenhouseKey {

    private val greenhouseScreenKey = KeyMappingHelper.registerKeyMapping(
        KeyMapping("key.magicaddons.greenhouse", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, Common.KEY_CATEGORY)
    )

    @EventHandler
    fun onTick(event: WorldTickEvent) {
        while (greenhouseScreenKey.consumeClick()) openScreen()
    }

    private fun openScreen() {
        if (McCompat.currentScreen() != null) return
        if (!GreenhousePresets.baseSetting.value) return
        if (!GreenhousePresets.keyWorksAnywhere() && !GreenhouseData.inOwnGarden()) return

        ScreenUtil.setScreen(GreenhouseScreen())
    }
}
