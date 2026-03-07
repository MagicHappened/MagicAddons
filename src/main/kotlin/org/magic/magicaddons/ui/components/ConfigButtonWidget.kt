package org.magic.magicaddons.ui.components

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import org.magic.magicaddons.features.Feature

class ConfigButtonWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val feature: Feature)
    : ButtonWidget(x, y, width, height, Text.literal(feature.displayName)) {

    init {
        setTooltip(Tooltip.of(Text.literal(feature.tooltipMessage)))
    }

    val disabledColor: Int = 0xFF828282.toInt()
    override val fillColor: Int
        get() = if (feature.enabled) super.fillColor else disabledColor

    override fun onClick(click: Click?, doubled: Boolean) {
        if (click?.button() == 0){
            feature.enabled = !feature.enabled
        }

    }

}