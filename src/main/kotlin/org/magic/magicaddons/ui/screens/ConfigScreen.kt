package org.magic.magicaddons.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.ui.widgets.ButtonWidget

class ConfigScreen(title: Text) : Screen(title) {

    private val buttons = mutableListOf<ButtonWidget>()
    private val buttonWidth = 150
    private val buttonHeight = 20
    private val padding = 5

    override fun init() {
        var yPos = 20

        // Iterate through all registered features
        FeatureManager.features.forEach { feature ->
            val button = ButtonWidget(
                x = width / 2 - buttonWidth / 2,
                y = yPos,
                width = buttonWidth,
                height = buttonHeight,
                message = Text.literal(feature.id)
            )

            // Set initial color based on feature.enabled
            button.fillColor = if (feature.enabled) 0xFF00AA00.toInt() else 0xFFAA0000.toInt()



            buttons.add(button)
            yPos += buttonHeight + padding
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            title,
            width / 2,
            10,
            0xFFFFFF
        )

        // Render all buttons
        buttons.forEach { it.render(context, mouseX, mouseY, delta) }

        super.render(context, mouseX, mouseY, delta)
    }


}