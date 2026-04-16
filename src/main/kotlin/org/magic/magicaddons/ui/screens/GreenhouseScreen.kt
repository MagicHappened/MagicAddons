package org.magic.magicaddons.ui.screens

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.magic.magicaddons.util.ScreenUtil

class GreenhouseScreen(title: Text) : Screen(title) {

    private val paddingY: Int = 50

    var startX: Int = paddingY
    var startY: Int = paddingY
    var containerSize: Int = 400

    override fun init() {
        super.init()
        containerSize = height-paddingY*2
        startX = (width-containerSize)/2
        startY = paddingY

    }

    /*
    todo to left of grid add menu buttons
    todo put preset on greenhouse with a mc dropdown? or own.

    todo when on a selected preset and press visualize, dropdown of which greenhouse (if applicable)
    todo prints out an error when not enough unlocked space in main greenhouse (detect if one gh and then conflict)

    todo grid?
    todo render the base soil on the greenhouse screen under the plants.
    */

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            Identifier.of("minecraft", "popup/background"),
            startX,
            startY,
            containerSize,
            containerSize
        )
    }
}