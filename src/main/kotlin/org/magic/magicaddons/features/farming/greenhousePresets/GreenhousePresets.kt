package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object GreenhousePresets : Feature() {

    init {
        SkyBlockAPI.eventBus.register(this)
    }
    override val id = "GreenhousePresets"
    override val displayName = "Greenhouse Presets"
    override val tooltipMessage = "Enables Greenhouse Presets..."
    override val category = "farming"

    override val baseSetting = BooleanSetting(
        displayName = displayName,
        tooltip = tooltipMessage,
        value = true,
        children = listOf(
            EnumSetting<Test1>(
                key = "test",
                displayName = "test",
                tooltip = "Just a test lol",
                value = Test1.EnumValue5forfun
            )
        )
    )

    enum class Test1 {
        EnumValue1,
        EnumValue2,
        EnumValue3,
        EnumValue4,
        EnumValue5forfun
    }

    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    private fun onIslandChange(event: IslandChangeEvent){
        GreenhouseData //for now for initialization
        CropRegistry

    }


}