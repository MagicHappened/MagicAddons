package org.magic.magicaddons.ui.components

import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import net.minecraft.client.MinecraftClient

class MAGuiComponent : GuiComponent() {

    override fun getWidth(): Int {
        val screenWidth: Int = MinecraftClient.getInstance().window.width
        return screenWidth - screenWidth / 6
    }

    override fun getHeight(): Int {
        val screenHeight: Int = MinecraftClient.getInstance().window.height
        return screenHeight - screenHeight / 8
    }

    override fun render(context: GuiImmediateContext) {

    }
}