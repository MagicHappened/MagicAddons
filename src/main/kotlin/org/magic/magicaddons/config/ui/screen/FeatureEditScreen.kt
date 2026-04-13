package org.magic.magicaddons.config.ui.screen

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.config.ui.feature.SettingWidget
import org.magic.magicaddons.config.ui.feature.SettingWidgetFactory
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ScreenUtil

class FeatureEditScreen(
    val feature: Feature,
    val parent: Screen?
) : Screen(Text.literal(feature.displayName)) {

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
            addDrawable(widget)
        }
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(ctx, mouseX, mouseY, delta)

        ScreenUtil.drawMultilineBoxCentered(
            ctx,
            screenDisplayTitle,
            width / 2,
            20
        )
    }

    override fun charTyped(input: CharInput): Boolean {
        baseChildrenWidgets.forEach {
            it.charTyped(input)
        }
        return super.charTyped(input)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        baseChildrenWidgets.forEach {
            it.keyPressed(input)
        }
        return super.keyPressed(input)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        baseChildrenWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
    }


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        var handled = false
        baseChildrenWidgets.forEach {
            if (it.mouseClicked(click, doubled))
                handled = true
        }
        return handled
    }

    override fun close() {
        client.setScreen(parent)
    }

    override fun removed() {
        MagicAddonsConfigJsonHandler.save()
    }
}