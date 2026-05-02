package org.magic.magicaddons.ui.widgets.greenhouse


import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class GreenhouseSlotWidget(
    val slot: GreenhouseSlot
) : Renderable, GuiEventListener {

    var sprite: TextureAtlasSprite? = null

    var widgetFocused
        get() = isFocused
        set(value) {isFocused = value}


    var widgetX: Int = 0
    var widgetY: Int = 0

    var widgetWidth: Int = 25
    var widgetHeight: Int = 25

    fun init(){
        if (slot.placedBlock == null){
            slot.placedBlock = Blocks.PODZOL.defaultBlockState()
            ChatUtils.sendWithPrefix("Block at ${slot.x} ${slot.y} failed to load, rendering as podzol.")
        }
        if (slot.placedBlock?.block == Blocks.AIR)
            sprite = null
        sprite = ScreenUtil.getSpriteForState(slot.placedBlock, Direction.UP)
    }

    override fun render(graphics: GuiGraphics, mouseY: Int, j: Int, deltaTicks: Float) {
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
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)){
            ChatUtils.sendWithPrefix("Click at slot ${slot.x} , ${slot.y} on ${slot.placedBlock}")
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