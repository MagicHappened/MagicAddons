package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
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
            BooleanSetting(
                key = "ReadyToHarvestWarning",
                displayName = "Ready To Harvest Warning",
                tooltip = "Tells you when a mutation you grew has nothing left to grow, on the " +
                        "tick it finishes and again on the way to the next one",
                value = false
            ),
            BooleanSetting(
                key = "DecayWarning",
                displayName = "Decay Warning",
                tooltip = "Warns six hours, one hour, twenty, five and one minute before a plant " +
                        "rots away. Needs a plant diagnostic to have been used on the plant, " +
                        "since nothing else says how old it is",
                value = false
            ),
            BooleanSetting(
                key = "SnoozlingAsleepWarning",
                displayName = "Snoozling Asleep Warning",
                tooltip = "Warns when a snoozling has dropped asleep, which it does on reaching " +
                        "stage 5, 10 and 15, and grows no further until it is woken",
                value = false
            ),
            BooleanSetting(
                key = "NoctilumeTimeWarning",
                displayName = "Noctilume Time Warning",
                tooltip = "Warns while a noctilume craves a time of day the garden is not on, " +
                        "since it stalls every tick until the garden time is changed",
                value = false
            ),
            BooleanSetting(
                key = "ChorusCollisionWarning",
                displayName = "Chorus Collision Warning",
                tooltip = "Warns before a chorus fruit runs out of tiles to teleport into and " +
                        "starts destroying the plot around it",
                value = false,
                children = listOf(
                    IntSetting(
                        key = "ChorusAbsenceTicks",
                        displayName = "Ticks Away",
                        tooltip = "How many growth ticks you expect to be away for. The line " +
                                "underneath says what that is in real time, counted from the tick " +
                                "already running",
                        value = 5,
                        range = 1..48,
                        detail = { GreenhouseData.absenceDetail() }
                    )
                )
            )
        )
    )




    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    private fun onIslandChange(event: IslandChangeEvent){
        // an object only registers on the event bus once something touches it, and these three
        // are only ever reached from their own handlers, so nothing else would wake them
        GreenhouseData
        GreenhouseWatering
        PlantWarnings
        CropRegistry

        // the attribute api only registers its listeners once something references it, so it is
        // referenced here rather than the first time a value is asked of it
        AttributeAPI


    }
    //todo dont render on top of other blocks just render a red outline and then when breaking said block
    // will render what to place


}