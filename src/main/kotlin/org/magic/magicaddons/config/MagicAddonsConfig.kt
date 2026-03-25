package org.magic.magicaddons.config

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = "MagicAddonsConfig")
class MagicAddonsConfig : ConfigData {

    @ConfigEntry.Gui.Tooltip
    var highlightLittleFoot: Boolean = true

    @ConfigEntry.Gui.Tooltip
    var hidePowderCoating: Boolean = true
}