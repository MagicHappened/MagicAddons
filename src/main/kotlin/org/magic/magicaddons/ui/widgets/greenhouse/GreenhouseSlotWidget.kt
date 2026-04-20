package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.block.Blocks
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.texture.Sprite
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class GreenhouseSlotWidget(
    val slot: GreenhouseSlot
) : Drawable, Element{

    var sprite: Sprite? = null

    var widgetFocused
        get() = isFocused
        set(value) {isFocused = value}


    var widgetX: Int = 0
    var widgetY: Int = 0

    var widgetWidth: Int = 25
    var widgetHeight: Int = 25

    fun init(){
        if (slot.placedBlock == null){
            slot.placedBlock = Blocks.PODZOL.defaultState
            ChatUtils.sendWithPrefix("Block at ${slot.x} ${slot.y} failed to load, rendering as podzol.")
        }
        if (slot.placedBlock?.block == Blocks.AIR)
            sprite = null
        sprite = ScreenUtil.getTopSpriteForState(slot.placedBlock)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        sprite ?: return
        context.drawSpriteStretched(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            widgetX,
            widgetY,
            widgetWidth,
            widgetHeight
        )
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (isMouseOver(click.x, click.y)){
            ChatUtils.sendWithPrefix("Click at slot ${slot.x} , ${slot.y} on ${slot.placedBlock?.block}")
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in widgetX..widgetX + widgetWidth && mouseY.toInt() in widgetY .. widgetY + widgetHeight)
    }


    override fun isFocused(): Boolean = widgetFocused

    override fun setFocused(focused: Boolean) {
        this.widgetFocused = focused
    }
}