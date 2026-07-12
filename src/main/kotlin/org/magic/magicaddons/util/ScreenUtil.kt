package org.magic.magicaddons.util

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import org.joml.Matrix3x2f

object ScreenUtil {

    private data class TextBoxLayout(
        val lines: List<String>,
        val maxWidth: Int,
        val lineHeight: Int,
        val totalHeight: Int,
        val boxWidth: Int,
        val boxHeight: Int
    )

    private fun computeLayout(text: String): TextBoxLayout {
        val textRenderer = Minecraft.getInstance().font
        val padding = 4

        val lines = text.lines()
        val maxWidth = lines.maxOfOrNull { textRenderer.width(it) } ?: 0
        val lineHeight = textRenderer.lineHeight
        val totalHeight = lines.size * lineHeight

        val boxWidth = maxWidth + padding * 2
        val boxHeight = totalHeight + padding * 2

        return TextBoxLayout(
            lines,
            maxWidth,
            lineHeight,
            totalHeight,
            boxWidth,
            boxHeight
        )
    }



    private var newScreen: Screen? = null

    fun setScreen(screen: Screen) {
        newScreen = screen
    }

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            val target = newScreen ?: return@register

            if (Minecraft.getInstance().gui.screen() !== target) {
                Minecraft.getInstance().gui.setScreen(target)
            } else {
                newScreen = null
            }
        }
    }

    fun GuiGraphicsExtractor.drawBorder(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int) {
        drawBorder(
            x1.toFloat(),
            y1.toFloat(),
            x2.toFloat(),
            y2.toFloat(),
            thickness.toFloat(),
            color
        )
    }


    fun GuiGraphicsExtractor.drawBorder(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        thickness: Float,
        color: Int? = null
    ) {
        // top
        drawLine(x1, y1, x2, y1, thickness, color)

        // bottom
        drawLine(x1, y2, x2, y2, thickness, color)

        // left
        drawLine(x1, y1, x1, y2, thickness, color)

        // right
        drawLine(x2, y1, x2, y2, thickness, color)
    }

    fun GuiGraphicsExtractor.drawSquareBorder(
        x: Float,
        y: Float,
        size: Float,
        thickness: Float,
        color: Int
    ) {
        drawBorder(
            x,
            y,
            x + size,
            y + size,
            thickness,
            color
        )
    }

    fun GuiGraphicsExtractor.drawLine(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        thickness: Int,
        color: Int
    ) {
        drawLine(
            x1.toFloat(),
            y1.toFloat(),
            x2.toFloat(),
            y2.toFloat(),
            thickness.toFloat(),
            color
        )
    }

    fun GuiGraphicsExtractor.drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        color: Int? = null
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length == 0f) return

        val pose = Matrix3x2f(this.pose())

        pose.translate(x1, y1)
        pose.rotate(kotlin.math.atan2(dy, dx))

        val half = thickness / 2f
        val y0 = kotlin.math.floor(-half).toInt()
        val y1 = kotlin.math.ceil(half).toInt()

        val actualColor = color ?: 0xFFFFFFFF.toInt()

        this.guiRenderState.addGuiElement(
            ColoredRectangleRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                pose,
                0,
                y0,
                length.toInt(),
                y1,
                actualColor,
                actualColor,
                this.scissorStack.peek()
            )
        )
    }

    fun GuiGraphicsExtractor.drawSimpleTooltip(text: String, mouseX: Int, mouseY: Int) {
        val client = Minecraft.getInstance()

        val lines = text.split("\n").map {
            ClientTooltipComponent.create(
                Component.literal(it).visualOrderText
            )
        }

        this.tooltip(
            client.font,
            lines,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        )
    }

    fun GuiGraphicsExtractor.drawMultilineBoxCentered(
        text: String,
        centerX: Int,
        centerY: Int,
        color: Int? = null
    ) {
        val layout = computeLayout(text)

        val x = centerX - layout.boxWidth / 2
        val y = centerY - layout.boxHeight / 2

        drawMultilineBox(text, x, y, color)
    }


    fun GuiGraphicsExtractor.drawMultilineBox(
        text: String,
        x: Int,
        y: Int,
        color: Int? = null
    ){
        drawMultilineBox(
            text,
            x.toFloat(),
            y.toFloat(),
        )
    }


    fun GuiGraphicsExtractor.drawMultilineBox(
        text: String,
        x: Float,
        y: Float,
        color: Int? = null
    ) {
        val font = Minecraft.getInstance().font
        val padding = 4f

        val layout = computeLayout(text)

        val x1 = x
        val y1 = y
        val x2 = x + layout.boxWidth
        val y2 = y + layout.boxHeight

        fill(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), 0x88000000.toInt())

        drawBorder(x1, y1, x2, y2, 1f, color)

        // text
        var currentY = y + padding

        layout.lines.forEach { line ->
            val seq = Component.literal(line).visualOrderText
            val centeredX = x + (layout.boxWidth - font.width(line)) / 2f

            guiRenderState.addText(
                GuiTextRenderState(
                    font,
                    seq,
                    Matrix3x2f(pose()),
                    centeredX.toInt(),
                    currentY.toInt(),
                    0xFFFFFFFF.toInt(),
                    0,
                    false,
                    false,
                    scissorStack.peek()
                )
            )

            currentY += layout.lineHeight
        }
    }

    fun GuiGraphicsExtractor.blitStretched(
        pipeline: RenderPipeline,
        texture: Identifier,
        x: Int,
        y: Int,
        u: Float,
        v: Float,
        width: Int,
        height: Int,
        textureWidth: Int,
        textureHeight: Int
    ){

        val pose = this.pose()

        val scaleTextureX = width.toFloat() / textureWidth.toFloat()
        val scaleTextureY = height.toFloat() / textureHeight.toFloat()

        pose.pushMatrix()

        pose.scale(scaleTextureX , scaleTextureY )

        pose.translate(x.toFloat() / scaleTextureX, y.toFloat() / scaleTextureY)

        this.blit(
            pipeline,
            texture,
            0,
            0,
            u,
            v,
            textureWidth,
            textureHeight,
            textureWidth,
            textureHeight
        )

        pose.popMatrix()
    }

    fun GuiGraphicsExtractor.renderFakeItem(
        stack: ItemStack,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        renderDecorations: Boolean = false
    ) {
        if (stack.isEmpty) return

        val mc = Minecraft.getInstance()

        val pose = this.pose()

        pose.pushMatrix()

        try {
            pose.translate(x.toFloat(), y.toFloat())

            val scaleX = width / 16.0f
            val scaleY = height / 16.0f
            val scale = minOf(scaleX, scaleY)

            pose.scale(scale, scale)

            this.item(stack, 0, 0)

            if (renderDecorations) {
                this.itemDecorations(mc.font, stack, 0, 0)
            }

        } finally {
            pose.popMatrix()
        }
    }




    fun getSpriteForState(state: BlockState, direction: Direction): TextureAtlasSprite {

        val client = Minecraft.getInstance()
        val model = client.modelManager.blockStateModelSet.get(state)

        val random = RandomSource.create(0)

        val parts: MutableList<BlockStateModelPart> = mutableListOf()

        model.collectParts(random, parts)


        for (part in parts) {

            var directionalMaterial = part


            return part.particleMaterial().sprite
        }


        throw IllegalStateException("No sprite for state $state")
    }




}