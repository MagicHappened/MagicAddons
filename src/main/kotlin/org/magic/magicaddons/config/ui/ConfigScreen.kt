package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.features.FeatureManager.features
import org.magic.magicaddons.util.ChatUtils

class ConfigScreen(title: Text, val parent: Screen?) : Screen(title) {

    val categories = MagicAddonsConfigJsonHandler.configMap
    val categoryWidgets = mutableListOf<ConfigCategoryWidget>()

    val categoryPadding: Int = 20
    val featurePadding: Int = 10

    override fun init() {
        if (categories.isEmpty()) {
            ChatUtils.sendWithPrefix("Unexpected empty category map. Cannot initialize screen.")
            return
        }

        categoryWidgets.clear()

        categories.forEach { (categoryName, featureMap) ->
            categoryWidgets.add(ConfigCategoryWidget(categoryName, featureMap))
        }

        var currentX = 200
        val baseY = 100

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
        MagicAddonsConfigJsonHandler.save()
        client?.setScreen(parent)
    }
}