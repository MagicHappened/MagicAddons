package org.magic.magicaddons.ui.screens

import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.Click
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
import org.magic.magicaddons.util.ScreenUtil.drawLine
import tech.thatgravyboat.skyblockapi.api.profile.items.wardrobe.WardrobeAPI.slots

class GreenhouseScreen(title: Text) : Screen(title) {

    private val gridSize = 10
    private val paddingY: Int = 50
    // todo change this to top padding and bottom padding
    private var startX: Int = paddingY
    private var startY: Int = paddingY
    private var containerSize: Int = 400
    private var slotSize: Int = 40

    val textureSize = 16f
    val borderPx = 4f

    var scale: Float = 1f
    var borderPadding: Int = 4

    val forward = Identifier.of("minecraft", "widget/page_forward")
    val backward = Identifier.of("minecraft", "widget/page_backward")
    //todo make highlighted arrows as well

    val spriteMap: MutableMap<Sprite, MutableList<Int>> = mutableMapOf()

    override fun init() {
        super.init()

        containerSize = height - paddingY * 2
        scale = containerSize / textureSize
        borderPadding = (borderPx * scale).toInt()

        slotSize = (containerSize - borderPadding*2) / (gridSize)
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


        //todo nice and all but turn into widgets cuz need handlers :/
        spriteMap.forEach { (sprite, ints) ->
            ints.forEach {
                context.drawSpriteStretched(
                    RenderPipelines.GUI_TEXTURED,
                    sprite,
                    startX + borderPadding + (it % gridSize)*slotSize,
                    startY + borderPadding + (it / gridSize)*slotSize,
                    slotSize,
                    slotSize
                )
            }
        }

        //todo these dont work
        for (x in 1..9){
            context.state.drawLine(
                startX + borderPadding + x * slotSize,
                startY + borderPadding,
                startX + borderPadding + x * slotSize,
                startY + containerSize - borderPadding,
                1,
                0xFFFFFFFF.toInt()
            )
        }
        for (y in 1..9){
            context.state.drawLine(
                startX + borderPadding,
                startY + borderPadding + y * slotSize,
                startX + containerSize - borderPadding,
                startY + containerSize + y * slotSize,
                1,
                0xFFFFFFFF.toInt()
            )

        }

        //todo also need these to function as actual clickers
        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            Identifier.of("minecraft", "widget/page_forward"),
            (width-20)/2,
            startY + containerSize + 20,
            16,
            16
        )

        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            Identifier.of("minecraft", "widget/page_backward"),
            (width+20)/2,
            startY + containerSize + 20,
            16,
            16
        )


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

    override fun mouseClicked(click: Click?, doubled: Boolean): Boolean {
        return super.mouseClicked(click, doubled)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }
}