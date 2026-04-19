package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.features.Feature

class ConfigCategoryWidget(
    val categoryName: String,
    categoryFeatures: List<Feature> // your featureMap
) : Drawable, Element {

    val categoryFeatureWidgets = mutableListOf<FeatureToggleWidget>()

    var x: Int = 0
    var y: Int = 0

    var width: Int = 200
    var height: Int = 0

    val categoryTitlePadding: Int = 5
    val featurePadding = 10

    init {
        categoryFeatures.forEach { feature ->
            categoryFeatureWidgets.add(FeatureToggleWidget(feature))
        }
    }

    fun init(baseX: Int, baseY: Int) {
        x = baseX
        y = baseY

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val titleHeight = textRenderer.fontHeight + categoryTitlePadding * 2

        val maxWidth = categoryFeatureWidgets.maxOfOrNull { it.getContentWidth() } ?: 200
        width = maxWidth

        // start below the title
        var currentY = y + titleHeight

        categoryFeatureWidgets.forEach {
            it.init()
            it.x = x
            it.y = currentY
            it.width = maxWidth
            it.checkbox.x = x
            it.checkbox.y = currentY
            currentY += it.height + featurePadding
        }

        // total height includes title
        height = currentY - y
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        val textRenderer = MinecraftClient.getInstance().textRenderer
        ctx.drawText(
            textRenderer,
            categoryName,
            x + (width - textRenderer.getWidth(categoryName)) / 2,
            y + (textRenderer.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        categoryFeatureWidgets.forEach {
            it.render(ctx, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        categoryFeatureWidgets.forEach {
            if (it.mouseClicked(click, doubled)) return true
        }
        return false
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}