package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.features.Feature

class FeatureEditScreen(
    feature: Feature,
    parent: Screen?
) : Screen(Text.literal(feature.displayName)) {

}