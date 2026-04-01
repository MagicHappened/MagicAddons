package org.magic.magicaddons.config.ui.screen

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.config.ui.ConfigCategoryWidget
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils

class ConfigScreen(title: Text, val parent: Screen?) : Screen(title) {

    val categoryWidgets = mutableListOf<ConfigCategoryWidget>()
    lateinit var categories: MutableMap<String, MutableList<Feature>>

    val categoryPadding: Int = 20

    override fun init() {
        MagicAddonsConfigJsonHandler.load()
        FeatureManager.syncFromConfig()
        categories = FeatureManager.features
            .groupBy { it.category }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()

        if (categories.isEmpty()) {
            ChatUtils.sendWithPrefix("Unexpected empty category map. Cannot initialize screen.")
            return
        }

        categoryWidgets.clear()

        categories.forEach { (categoryName, featureList) ->
            categoryWidgets.add(ConfigCategoryWidget(categoryName, featureList))
        }

        var currentX = 50
        val baseY = 25

        categoryWidgets.forEach { category ->
            category.layout(currentX, baseY)

            currentX += category.width + categoryPadding

            addDrawable(category)
        }
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(ctx, mouseX, mouseY, delta)

        val textWidth = textRenderer.getWidth(title)
        ctx.drawText(
            textRenderer,
            title,
            (width - textWidth) / 2,
            10,
            0xFFFFFF,
            false
        )
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        categoryWidgets.forEach {
            it.mouseClicked(click, doubled)
        }
        return super.mouseClicked(click, doubled)
    }

    override fun close() {
        FeatureManager.syncToConfig()
        MagicAddonsConfigJsonHandler.save()
        client?.setScreen(parent)
    }
}