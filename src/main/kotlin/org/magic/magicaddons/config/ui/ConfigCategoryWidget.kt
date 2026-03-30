package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.features.FeatureManager.features

class ConfigCategoryWidget(
    val categoryName: String,
    featureIds: Map<String, Any> // your featureMap
) : Drawable, Element {

    val categoryFeatures = mutableListOf<FeatureToggleWidget>()

    var x: Int = 0
    var y: Int = 0

    var width: Int = 200
    var height: Int = 0

    val featurePadding = 10

    init {
        featureIds.forEach { (featureId, _) ->
            val feature = features.find { it.id == featureId } ?: return@forEach
            categoryFeatures.add(FeatureToggleWidget(feature))
        }
    }

    fun layout(baseX: Int, baseY: Int) {
        x = baseX
        y = baseY

        val maxWidth = categoryFeatures.maxOfOrNull { it.getContentWidth() } ?: 200
        width = maxWidth

        var currentY = y

        categoryFeatures.forEach {
            it.x = x
            it.y = currentY
            it.width = maxWidth

            currentY += it.height + featurePadding
        }

        height = currentY - y
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        categoryFeatures.forEach {
            it.render(ctx, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        categoryFeatures.forEach {
            if (it.mouseClicked(click, doubled)) return true
        }
        return false
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}