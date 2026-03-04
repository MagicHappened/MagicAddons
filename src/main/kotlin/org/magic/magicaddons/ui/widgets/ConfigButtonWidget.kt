package org.magic.magicaddons.ui.widgets

import net.minecraft.text.Text
import org.magic.magicaddons.features.Feature

class ConfigButtonWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val feature: Feature)
    : ButtonWidget(x, y, width, height, Text.literal(feature.displayName)) {

}