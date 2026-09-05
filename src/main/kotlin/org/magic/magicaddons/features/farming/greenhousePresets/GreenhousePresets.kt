package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration
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

    private const val KEY_ANYWHERE = "GreenhouseKeyAnywhere"
    private const val TURN_GRID_KEY = "TurnGridWithPlayer"

    private const val WARNINGS_KEY = "Warnings"
    private const val TYPES_KEY = "Types"
    private const val REMINDERS_KEY = "Reminders"
    const val CHORUS_KEY = "ChorusCollisionWarning"
    private const val CHORUS_TICKS_KEY = "ChorusAbsenceTicks"
    const val AT_TICK_KEY = "AtTheTick"
    private const val TEN_MINUTES_KEY = "TenMinutesBefore"
    private const val FIVE_MINUTES_KEY = "FiveMinutesBefore"
    private const val ONE_MINUTE_KEY = "OneMinuteBefore"

    init {
        SkyBlockAPI.eventBus.register(this)
    }

    fun keyWorksAnywhere(): Boolean = baseSetting.getChild<BooleanSetting>(KEY_ANYWHERE)?.value == true

    fun turnsGridWithPlayer(): Boolean = baseSetting.getChild<BooleanSetting>(TURN_GRID_KEY)?.value == true

    private fun warnings(): BooleanSetting? = baseSetting.getChild<BooleanSetting>(WARNINGS_KEY)?.takeIf { it.value }
    private fun types(): BooleanSetting? = warnings()?.getChild<BooleanSetting>(TYPES_KEY)?.takeIf { it.value }
    private fun reminders(): BooleanSetting? = warnings()?.getChild<BooleanSetting>(REMINDERS_KEY)?.takeIf { it.value }

    /** Whether one kind of warning is on, with the headings above it on too. */
    fun warningType(key: String): Boolean = types()?.getChild<BooleanSetting>(key)?.value == true

    /** Whether one of the reminder moments is on, with the headings above it on too. */
    fun reminder(key: String): Boolean = reminders()?.getChild<BooleanSetting>(key)?.value == true

    /** How far ahead of the next tick the warnings are sent, from the reminders that are on. */
    fun reminderThresholds(): List<Duration> = listOfNotNull(
        Duration.ofMinutes(10).takeIf { reminder(TEN_MINUTES_KEY) },
        Duration.ofMinutes(5).takeIf { reminder(FIVE_MINUTES_KEY) },
        Duration.ofMinutes(1).takeIf { reminder(ONE_MINUTE_KEY) }
    )

    /** How many growth ticks the player says they will be away for, under the chorus warning. */
    fun chorusAbsenceTicks(): Int? = baseSetting.getChild<BooleanSetting>(WARNINGS_KEY)
        ?.getChild<BooleanSetting>(TYPES_KEY)
        ?.getChild<BooleanSetting>(CHORUS_KEY)
        ?.getChild<IntSetting>(CHORUS_TICKS_KEY)
        ?.value
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
                key = WARNINGS_KEY,
                displayName = "Warnings",
                tooltip = "Chat warnings about the greenhouses: which ones, and how far ahead",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = TYPES_KEY,
                        displayName = "Types",
                        tooltip = "Which warnings are sent. Off, none are",
                        value = true,
                        children = listOf(
                            BooleanSetting(
                                key = "ReadyToHarvestWarning",
                                displayName = "Ready To Harvest",
                                tooltip = "Tells you when a mutation you grew has nothing left to grow",
                                value = false
                            ),
                            BooleanSetting(
                                key = "DecayWarning",
                                displayName = "Decay",
                                tooltip = "Warns six hours, one hour, twenty, five and one minute before a plant " +
                                        "rots away. Needs a plant diagnostic to have been used on the plant, " +
                                        "since nothing else says how old it is",
                                value = false
                            ),
                            BooleanSetting(
                                key = "SnoozlingAsleepWarning",
                                displayName = "Snoozling Asleep",
                                tooltip = "Warns when a snoozling has dropped asleep, which it does on reaching " +
                                        "stage 5, 10 and 15, and grows no further until it is woken",
                                value = false
                            ),
                            BooleanSetting(
                                key = "NoctilumeTimeWarning",
                                displayName = "Noctilume Time",
                                tooltip = "Warns while a noctilume craves a time of day the garden is not on, " +
                                        "since it stalls every tick until the garden time is changed",
                                value = false
                            ),
                            BooleanSetting(
                                key = CHORUS_KEY,
                                displayName = "Chorus Collision",
                                tooltip = "Warns before a chorus fruit runs out of tiles to teleport into and " +
                                        "starts destroying the plot around it",
                                value = false,
                                children = listOf(
                                    IntSetting(
                                        key = CHORUS_TICKS_KEY,
                                        displayName = "Ticks Away",
                                        tooltip = "How many growth ticks you expect to be away for. The line " +
                                                "underneath says what that is in real time, counted from the tick " +
                                                "already running",
                                        value = 5,
                                        range = 1..48,
                                        detail = { GreenhouseData.absenceDetail() }
                                    )
                                )
                            ),
                            BooleanSetting(
                                key = PlantWarnings.OTHER_PROFILES_KEY,
                                displayName = "Other Profiles",
                                tooltip = "Lets the greenhouses of your other profiles warn too, moved on by their " +
                                        "own clocks as if you were away. Each warning says which profile it is about",
                                value = false
                            )
                        )
                    ),
                    BooleanSetting(
                        key = REMINDERS_KEY,
                        displayName = "Reminders",
                        tooltip = "When a warning about the next tick is sent. Off, none are",
                        value = true,
                        children = listOf(
                            BooleanSetting(
                                key = AT_TICK_KEY,
                                displayName = "At The Tick",
                                tooltip = "The moment a growth tick lands",
                                value = false
                            ),
                            BooleanSetting(
                                key = TEN_MINUTES_KEY,
                                displayName = "10 Minutes Before",
                                tooltip = "Ten minutes before the next growth tick",
                                value = false
                            ),
                            BooleanSetting(
                                key = FIVE_MINUTES_KEY,
                                displayName = "5 Minutes Before",
                                tooltip = "Five minutes before the next growth tick",
                                value = false
                            ),
                            BooleanSetting(
                                key = ONE_MINUTE_KEY,
                                displayName = "1 Minute Before",
                                tooltip = "One minute before the next growth tick",
                                value = false
                            )
                        )
                    )
                )
            ),
            BooleanSetting(
                key = GreenhouseHud.KEY,
                displayName = "Greenhouse HUD",
                tooltip = "A small panel on screen while standing in a greenhouse: the next tick, " +
                        "and how many plants are ready, dry, asleep or about to rot",
                value = false
            ),
            BooleanSetting(
                key = KEY_ANYWHERE,
                displayName = "Greenhouse Key Anywhere",
                tooltip = "Lets the greenhouse screen key (G unless rebound) open the screen " +
                        "outside a greenhouse too. Off, it only works while standing in one",
                value = false
            ),
            BooleanSetting(
                key = TURN_GRID_KEY,
                displayName = "Turn Grid With Player",
                tooltip = "Turns the greenhouse screen's grid so the way you are facing is up. " +
                        "Only the picture turns: plans still go on the same tiles",
                value = false
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
        GreenhouseHud
        CropRegistry

        // the attribute api only registers its listeners once something references it, so it is
        // referenced here rather than the first time a value is asked of it
        AttributeAPI


    }
    //todo dont render on top of other blocks just render a red outline and then when breaking said block
    // will render what to place


}