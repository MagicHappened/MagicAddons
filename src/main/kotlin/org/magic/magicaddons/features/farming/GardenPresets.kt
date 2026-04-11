package org.magic.magicaddons.features.farming

import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.features.Feature

object GardenPresets : Feature() {
    override val id: String = "GardenPresets"
    override val displayName: String = "Garden Presets"
    override val tooltipMessage: String = "Enables Garden Presets, highlighting, prevents accidental breaks and more."
    override val category: String = "farming"
    override val baseSetting: BooleanSetting =
        BooleanSetting(
            key = "enabled",
            displayName = displayName,
            tooltip = tooltipMessage,
            value = false
        )
}