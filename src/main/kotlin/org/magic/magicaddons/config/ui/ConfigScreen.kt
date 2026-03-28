package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.features.FeatureManager.features

class ConfigScreen(title: Text, val parent: Screen?) : Screen(title) {

    private val widgets = mutableListOf<ExpandableFeatureWidget>()

    override fun init() {
        widgets.clear()

        val categories = MagicAddonsConfigJsonHandler.configMap
        if (categories.isEmpty()) return

        val categoryWidth = 200
        val categorySpacing = 20

        var categoryIndex = 0

        categories.forEach { (categoryName, featureMap) ->

            val baseX = 20 + categoryIndex * (categoryWidth + categorySpacing)
            var currentY = 40

            featureMap.forEach { (featureId, data) ->

                val feature = features.find { it.id == featureId } ?: return@forEach

                val base = BooleanSettingWidget(feature, baseX, currentY)

                val widget = ExpandableFeatureWidget(
                    feature,
                    base,
                    baseX,
                    currentY,
                    categoryWidth,
                    20
                )

                widgets.add(widget)

                currentY += widget.getTotalHeight() + 4
            }

            categoryIndex++
        }
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(ctx, mouseX, mouseY, delta)

        // 🔹 Title
        val textWidth = textRenderer.getWidth(title)
        ctx.drawText(
            textRenderer,
            title,
            (width - textWidth) / 2,
            10,
            0xFFFFFF,
            false
        )

        val categories = MagicAddonsConfigJsonHandler.configMap
        val categoryWidth = 200
        val categorySpacing = 20

        var categoryIndex = 0

        categories.keys.forEach { categoryName: String ->

            val baseX = 20 + categoryIndex * (categoryWidth + categorySpacing)

            // 🔹 Category title
            ctx.drawText(
                textRenderer,
                Text.literal(categoryName),
                baseX,
                25,
                0xAAAAAA,
                false
            )

            categoryIndex++
        }

        // 🔹 Reflow layout EVERY FRAME (IMPORTANT)
        val grouped = widgets.groupBy { it.feature.category }

        var categoryIndex2 = 0
        grouped.forEach { (_, list) ->

            val baseX = 20 + categoryIndex2 * (categoryWidth + categorySpacing)
            var currentY = 40

            list.forEach { widget ->
                widget.setY(currentY)
                widget.render(ctx, mouseX, mouseY, delta)
                currentY += widget.getTotalHeight() + 4
            }

            categoryIndex2++
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        widgets.forEach {
            if (it.mouseClicked(click, doubled)) return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun close() {
        MagicAddonsConfigJsonHandler.save()
        client?.setScreen(parent)
    }
}