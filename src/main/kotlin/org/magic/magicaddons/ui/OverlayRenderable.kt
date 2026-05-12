package org.magic.magicaddons.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent

interface OverlayRenderable {
    val renderPriority: Int

    fun renderOverlay(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    )
    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return false
    }

}