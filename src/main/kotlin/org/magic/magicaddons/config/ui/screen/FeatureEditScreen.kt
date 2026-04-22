package org.magic.magicaddons.config.ui.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.config.ui.feature.SettingWidget
import org.magic.magicaddons.config.ui.feature.SettingWidgetFactory
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered

class FeatureEditScreen(
    val feature: Feature,
    val parent: Screen?
) : Screen(Component.literal(feature.displayName)) {

    val childrenSettings: List<SettingNode<*>> = feature.baseSetting.children
        ?: throw IllegalStateException("Cannot construct a feature edit screen for a feature with no nested settings")

    val baseChildrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    val screenDisplayTitle: String = "Editing ${feature.displayName}"

    val screenPaddingX: Int = 100
    val screenPaddingY: Int = 50

    val settingSpacingX: Int = 20 // setting childs CANNOT be larger than the base

    override fun init() {
        super.init()

        val count = childrenSettings.size
        if (count == 0) return

        val settingsTotalWidth = width - 2 * screenPaddingX
        val totalSpacing = (count - 1) * settingSpacingX
        val widgetWidth = (settingsTotalWidth - totalSpacing) / count

        childrenSettings.forEachIndexed { index, setting ->
            val widget = SettingWidgetFactory.create(setting)

            val xOffset = index * (widgetWidth + settingSpacingX)

            widget.width = widgetWidth
            widget.x = screenPaddingX + xOffset
            widget.y = screenPaddingY


            widget.init()
            baseChildrenWidgets.add(widget)
            addRenderableWidget(widget)
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(guiGraphics, mouseX, mouseY, delta)

        guiGraphics.drawMultilineBoxCentered(
            screenDisplayTitle,
            width / 2,
            20
        )
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        baseChildrenWidgets.forEach {
            it.charTyped(characterEvent)
        }
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        baseChildrenWidgets.forEach {
            it.keyPressed(keyEvent)
        }
        return super.keyPressed(keyEvent)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        baseChildrenWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
    }


    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        var handled = false
        baseChildrenWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled))
                handled = true
        }
        return handled
    }

    override fun onClose() {
        Minecraft.getInstance().setScreen(parent)
    }

    override fun removed() {
        MagicAddonsConfigJsonHandler.save()
    }
}