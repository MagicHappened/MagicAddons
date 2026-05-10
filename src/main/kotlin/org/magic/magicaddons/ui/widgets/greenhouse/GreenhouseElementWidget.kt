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
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem

class GreenhouseElementWidget(val instance: GreenhouseElementInstance,val definition: CropDefinition) : Renderable, GuiEventListener {
    var widgetX: Int = 0
    var widgetY: Int = 0
    var padding: Int = 0
    var width = 50
    var height = 50
    var sprite: TextureAtlasSprite? = null
    var renderedStack: ItemStack = ItemStack.EMPTY
    fun init(){
        sprite = ScreenUtil.getSpriteForState(Blocks.FIRE.defaultBlockState(),Direction.NORTH)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTick: Float) {
        if (instance.elementId == "Fire") {
            renderFire(guiGraphics, mouseX, mouseY, deltaTick)
            return
        }
        guiGraphics.renderFakeItem(
            renderedStack,
            widgetX + padding,
            widgetY + padding,
            width - padding * 2,
            height - padding * 2
        )
    }

    fun renderFire(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTick: Float){
        val sprite = sprite
        sprite ?: return
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            widgetX,
            widgetY,
            width,
            height
        )
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
        if (isMouseOver(mouseX, mouseY)) {
            val screen = Minecraft.getInstance().screen
            if (screen is GreenhouseScreen){
                screen.hoveredWidget = this
            }
        }
    }
    fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font

        val lines = buildList {
            add(Component.literal(definition.name))

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