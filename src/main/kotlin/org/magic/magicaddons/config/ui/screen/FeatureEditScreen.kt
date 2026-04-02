package org.magic.magicaddons.config.ui.screen

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.config.ui.feature.SettingWidget
import org.magic.magicaddons.config.ui.feature.SettingWidgetFactory
import org.magic.magicaddons.features.Feature

class FeatureEditScreen(
    val feature: Feature,
    val parent: Screen?
) : Screen(Text.literal(feature.displayName)) {

    val childrenSettings: List<SettingNode<*>> = feature.baseSetting.children
        ?: throw IllegalStateException("Cannot construct a feature edit screen for a feature with no nested settings")

    val baseChildrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    val screenDisplayTitle: String = "Editing ${feature.displayName}"

    override fun init() {
        super.init()
        childrenSettings.forEach {
            baseChildrenWidgets.add(SettingWidgetFactory.create(it))
        }
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(ctx, mouseX, mouseY, delta)

        val textWidth = textRenderer.getWidth(screenDisplayTitle)

        ctx.drawText(
            textRenderer,
            screenDisplayTitle,
            (width - textWidth) / 2,
            10,
            0xFFFFFFFF.toInt(),
            false
        )
    }

    // todo change this to react to different widgets
    override fun mouseClicked(click: Click?, doubled: Boolean): Boolean {
        return super.mouseClicked(click, doubled)
    }

    override fun close() {
        MinecraftClient.getInstance().setScreen(parent)
    }
}