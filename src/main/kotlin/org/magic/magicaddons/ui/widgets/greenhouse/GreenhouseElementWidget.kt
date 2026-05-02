package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ScreenUtil.blitStretched
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem

class GreenhouseElementWidget(val state: ElementRuntimeState) : Renderable, GuiEventListener {
    var widgetX: Int = 0
    var widgetY: Int = 0
    var padding: Int = 0
    var width = 50
    var height = 50

    var renderedStack: ItemStack = ItemStack.EMPTY


    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        if (state.renderOverride != null) {
            state.renderOverride!!.invoke(
                guiGraphics,
                widgetX + padding,
                widgetY + padding,
                width - padding * 2,
                height - padding * 2
                )
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
            add(Component.literal(state.cropDef?.name ?: state.nameOverride ?: "Unknown"))

            val growthText = when (val stage = state.growthStage) {
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