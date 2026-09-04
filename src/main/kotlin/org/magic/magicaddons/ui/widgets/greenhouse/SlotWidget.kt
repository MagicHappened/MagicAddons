package org.magic.magicaddons.ui.widgets.greenhouse


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
    
    fun init(){
        if (slot.placedBlock == null){
            slot.placedBlock = Blocks.PODZOL.defaultBlockState()
            Common.LOGGER.warn("Encountered a null slot block. replacing with podzol")
        }
        if (slot.placedBlock?.block == Blocks.AIR){
            sprite = null
            return
        }
        sprite = ScreenUtil.getSpriteForState(slot.placedBlock!!, Direction.UP)


    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
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