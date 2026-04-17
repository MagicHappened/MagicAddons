package org.magic.magicaddons.ui.screens

import net.minecraft.block.BlockState
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
import org.magic.magicaddons.features.farming.GreenhousePresets
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class GreenhouseScreen(title: Text) : Screen(title) {

    private val paddingY: Int = 50
    // todo change this to top padding and bottom padding
    var startX: Int = paddingY
    var startY: Int = paddingY
    var containerSize: Int = 400

    val gridSize = 10
    val slotSize = containerSize / gridSize

    val spriteMap: MutableMap<Sprite, MutableList<Int>> = mutableMapOf()

    override fun init() {
        super.init()
        containerSize = height - paddingY * 2
        startX = (width - containerSize) / 2
        startY = paddingY

        if (GreenhousePresets.initializedGreenhouseIds.isEmpty()) {
            ChatUtils.sendWithPrefix("No initialized greenhouse ids, please enter your greenhouse.")
            return
        }

        val grid = GreenhousePresets.greenhouseList.firstOrNull() ?: return

        val stateMap = mutableMapOf<BlockState, MutableList<Int>>()
        spriteMap.clear()

        for (x in 0 until 10) {
            for (y in 0 until 10) {

                val slot = grid.getSlot(x, y)
                val state = slot?.placedBlock ?: continue

                val index = y * 10 + x

                stateMap
                    .getOrPut(state) { mutableListOf() }
                    .add(index)
            }
        }

        stateMap.forEach { (state, indices) ->

            val sprite = getTopSpriteForState(state) ?: return@forEach

            spriteMap[sprite] = indices
        }

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

        ScreenUtil.drawMultilineBoxCentered(
            context,
            "Greenhouse 1",
            width/2,
            25
        )

        if (spriteMap.isEmpty()) return

        spriteMap.forEach { (sprite, ints) ->
            ints.forEach {
                context.drawSpriteStretched(
                    RenderPipelines.GUI_TEXTURED,
                    sprite,
                    startX + it % gridSize,
                    startY + it / gridSize,
                    slotSize,
                    slotSize
                )
            }
        }



    }

    // todo possibly add state for moisture
    fun getTopSpriteForState(state: BlockState): Sprite? {
        val client = MinecraftClient.getInstance()

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