package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem

class ElementWidget(val instance: GreenhouseElementInstance) : Renderable, GuiEventListener {
    var widgetX: Int = 0
    var widgetY: Int = 0
    var padding: Int = 0
    var width = 50
    var height = 50
    var sprite: TextureAtlasSprite? = ScreenUtil.getSpriteForState(Blocks.FIRE.defaultBlockState(),Direction.NORTH)
    var renderedStack: ItemStack = ItemStack.EMPTY
    var markingColor: Int? = null
    @JvmField
    var isFocused: Boolean = false

    enum class HoverInfo {
        GrowthStage,
        WaterLevel,
        DecayTime,

    }

    fun init(){
        markingColor = when (instance.slot.slotMark){
            LayoutSlot.Marking.Target -> {
                0xFF2dbcf6.toInt()
            }
            LayoutSlot.Marking.Ingredient -> {
                0xFF89F336.toInt()
            }
            LayoutSlot.Marking.UniqueCrop -> {
                0xFFbb00bb.toInt()
            }
            else -> {null}
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTick: Float) {
        markingColor?.let {
            graphics.drawBorder(
                widgetX + 1,
                widgetY + 1,
                widgetX + width - 1,
                widgetY + height - 1,
                1,
                it
            )
        }
        if (instance.elementId == "Fire") {
            renderFire(graphics, mouseX, mouseY, deltaTick)
            return
        }
        graphics.renderFakeItem(
            renderedStack,
            widgetX + padding,
            widgetY + padding,
            width - padding * 2,
            height - padding * 2
        )
    }

    fun renderFire(graphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTick: Float){
        val sprite = sprite
        sprite ?: return
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            widgetX,
            widgetY,
            width,
            height
        )
    }

    //todo add render info and such
    fun renderHoverButtonInfo(graphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTick: Float){

    }

    fun renderSideTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTick: Float){

    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            ChatUtils.sendWithPrefix("Clicked on ${instance.elementId} ")
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in widgetX..widgetX+width
                && mouseY.toInt() in widgetY..widgetY + height)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
    }
    fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font

        val lines = buildList {
            add(Component.literal(instance.cropDef.name))

            val growthText = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known ->
                    "Growth: ${stage.stage}"

                is GrowthStageInfo.Estimated ->
                    "Growth: ${stage.range.first}-${stage.range.last}"

                null -> null
            }

            growthText?.let {
                add(Component.literal(it))
            }
        }

        val components = lines.map { ClientTooltipComponent.create(it.visualOrderText) }

        graphics.renderTooltip(
            font,
            components,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        )
    }


    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}