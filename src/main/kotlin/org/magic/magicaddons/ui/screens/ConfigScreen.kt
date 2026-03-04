package org.magic.magicaddons.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.ui.widgets.ButtonWidget
import org.magic.magicaddons.ui.widgets.ConfigButtonWidget

class ConfigScreen : Screen(Text.literal(Common.MOD_NAME + " Configuration")) {

    private val buttons = mutableListOf<ConfigButtonWidget>()
    private val buttonWidth = 150
    private val buttonHeight = 20
    private val padding = 5

    override fun init() {
        var yPos = 50

        // Iterate through all registered features
        FeatureManager.features.forEach { feature ->
            val button = ConfigButtonWidget(
                x = width / 2 - buttonWidth / 2,
                y = yPos,
                width = buttonWidth,
                height = buttonHeight,
                feature = feature
            )

            buttons.add(button)
            yPos += buttonHeight + padding
        }

        buttons.forEach { this.addDrawable(it) }
        buttons.forEach { this.addSelectableChild(it) }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            title,
            width / 2,
            10,
            0xFFFFFF
        ) //heh? not working
        super.render(context, mouseX, mouseY, delta) // for rendering button drawables

    }


}