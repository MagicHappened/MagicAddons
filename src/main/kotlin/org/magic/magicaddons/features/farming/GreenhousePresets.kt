package org.magic.magicaddons.features.farming

import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileData
import tech.thatgravyboat.skyblockapi.helpers.SkyBlockEntity

object GreenhousePresets : Feature() {
    override val id: String = "GreenhousePresets"
    override val displayName: String = "Greenhouse Presets"
    override val tooltipMessage: String = "Enables Greenhouse Presets, highlighting, prevents accidental breaks and more."
    override val category: String = "farming"

    override val baseSetting: BooleanSetting =
        BooleanSetting(
            key = "enabled",
            displayName = displayName,
            tooltip = tooltipMessage,
            value = true
        ) //todo change to false default, just for testing purposes


    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        val isGreenhouse = PlotAPI.getCurrentPlot()?.data?.isGreenhouse
        //todo here should be highlighting
    }

    @Subscription
    fun islandChanged(event: IslandChangeEvent) {
        val currentPlot = PlotAPI.getCurrentPlot() ?: return
        currentPlot.aabb
    }







}