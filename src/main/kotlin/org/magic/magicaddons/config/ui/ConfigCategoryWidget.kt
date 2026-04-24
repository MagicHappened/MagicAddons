package org.magic.magicaddons.config.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.config.ui.feature.FeatureToggleWidget
import org.magic.magicaddons.features.Feature

class ConfigCategoryWidget(
    val categoryName: String,
    categoryFeatures: List<Feature> // your featureMap
) : Renderable, GuiEventListener, NarratableEntry {

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

        val font = Minecraft.getInstance().font
        val titleHeight = font.lineHeight + categoryTitlePadding * 2

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

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {

        val font = Minecraft.getInstance().font
        graphics.drawString(
            font,
            categoryName,
            x + (width - font.width(categoryName)) / 2,
            y + (font.lineHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        categoryFeatureWidgets.forEach {
            it.render(graphics, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        categoryFeatureWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }
        return false
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
    override fun narrationPriority(): NarratableEntry.NarrationPriority {
       return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {
    }
}