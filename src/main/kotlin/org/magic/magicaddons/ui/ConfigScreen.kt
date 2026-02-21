package org.magic.magicaddons.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.awt.Color

class ConfigScreen(title: Text) : Screen(title) {
    override fun init() {}
    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        context?.drawText(client?.textRenderer,"Test text" , width / 2, (height / 2) , Color.red.red, false)
    }
}