package org.magic.magicaddons.features.farming

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.features.Feature

object GreenhousePresets : Feature() {
    override val id: String = "GreenhousePresets"
    override val displayName: String = "Greenhouse Presets"
    override val tooltipMessage: String = "Enables Greenhouse Presets, highlighting, prevents accidental breaks and more."
    override val category: String = "farming"

    override val baseSetting: BooleanSetting =
        BooleanSetting(
            displayName = displayName,
            tooltip = tooltipMessage,
            value = false
        )
}