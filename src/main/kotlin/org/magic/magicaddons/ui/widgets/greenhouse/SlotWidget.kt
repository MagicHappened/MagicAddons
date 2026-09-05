package org.magic.magicaddons.ui.widgets.greenhouse


import org.magic.magicaddons.util.ScreenUtil.drawCheckerboard
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class SlotWidget(
    val slot: LayoutSlot
) : Renderable, Focusable {

    var sprite: TextureAtlasSprite? = null



    var widgetX: Int = 0
    var widgetY: Int = 0

    var widgetWidth: Int = 25
    var widgetHeight: Int = 25


    override var focusedState: Boolean = false
    
    /** Whether the slot asks for air, drawn as the checkerboard; a slot with nothing set draws nothing. */
    private var air: Boolean = false

    fun init(){
        val block = slot.placedBlock
        air = block?.block == Blocks.AIR
        sprite = if (block == null || air) null else ScreenUtil.getSpriteForState(block, Direction.UP)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (air) {
            graphics.drawCheckerboard(widgetX, widgetY, widgetX + widgetWidth, widgetY + widgetHeight)
            return
        }
        val sprite = sprite ?: return
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            widgetX,
            widgetY,
            widgetWidth,
            widgetHeight
        )

    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in widgetX until widgetX + widgetWidth &&
                mouseY.toInt() in widgetY until widgetY + widgetHeight
    }



}