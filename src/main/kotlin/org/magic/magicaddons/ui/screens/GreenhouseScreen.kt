package org.magic.magicaddons.ui.screens

import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.texture.Sprite
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import org.magic.magicaddons.data.greenhouse.GreenhouseElement
import org.magic.magicaddons.data.greenhouse.elements.mutation.common.Ashwreath

class GreenhouseScreen(title: Text) : Screen(title) {

    private val paddingY: Int = 50

    var startX: Int = paddingY
    var startY: Int = paddingY
    var containerSize: Int = 400

    var sprite: Sprite? = null

    override fun init() {
        super.init()
        containerSize = height - paddingY * 2
        startX = (width - containerSize) / 2
        startY = paddingY

        sprite = getTopSprite(testList[0].requiredSoil)

    }

    val testList = mutableListOf<GreenhouseElement>(
        Ashwreath(),
        Ashwreath()
    )


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

        sprite ?: return
        context.drawSpriteStretched(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            startX,
            startY,
            100,
            100
        )

    }

    // todo possibly add state for moisture
    fun getTopSprite(block: Block): Sprite? {
        val client = MinecraftClient.getInstance()
        val state = block.defaultState
        val model = client.blockRenderManager.models.getModel(state)

        val parts = model.getParts(Random.create())

        for (part in parts) {
            val quads = part.getQuads(Direction.UP)
            if (quads.isNotEmpty()) {
                return quads[0].sprite
            }
        }

        for (part in parts) {
            val quads = part.getQuads(null)
            if (quads.isNotEmpty()) {
                return quads[0].sprite
            }
        }

        return null
    }
}