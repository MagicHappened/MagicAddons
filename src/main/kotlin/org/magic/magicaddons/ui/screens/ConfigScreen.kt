package org.magic.magicaddons.ui.screens

import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.ui.components.ConfigButtonWidget

class ConfigScreen() : MoulConfigEditor<Feature>() {

    private val buttons = mutableListOf<ConfigButtonWidget>()
    private val buttonWidth = 150
    private val buttonHeight = 20
    private val padding = 5

    override fun init() {
        var yPos = 50

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
        super.init()
    }



}