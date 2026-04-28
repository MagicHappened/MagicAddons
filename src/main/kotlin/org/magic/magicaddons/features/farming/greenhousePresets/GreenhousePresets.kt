package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

object GreenhousePresets : Feature() {

    override val id = "GreenhousePresets"
    override val displayName = "Greenhouse Presets"
    override val tooltipMessage = "Enables Greenhouse Presets..."
    override val category = "farming"

    override val baseSetting = BooleanSetting(
        displayName = displayName,
        tooltip = tooltipMessage,
        value = true
    )


}