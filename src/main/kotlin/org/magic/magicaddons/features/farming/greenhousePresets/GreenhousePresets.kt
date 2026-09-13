package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.data.config.ParentSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI

object GreenhousePresets : Feature() {

    private const val KEY_ANYWHERE = "GreenhouseKeyAnywhere"
    private const val TURN_GRID_KEY = "TurnGridWithPlayer"
    private const val PLANNER_OPTIONS_KEY = "PlannerOptions"
    private const val PLANT_TRANSPARENCY_KEY = "PlantTransparency"
    private const val PLANT_HIGHLIGHTS_KEY = "PlantHighlights"
    private const val HARVEST_HIGHLIGHT_KEY = "HarvestHighlight"
    private const val HARVEST_ONLY_TARGETS_KEY = "OnlyPresetTargets"
    private const val PLANNER_COLORS_KEY = "PlannerColors"
    private const val WATER_INDICATOR_KEY = "WaterIndicator"
    private const val WATER_ONLY_WITHOUT_PLANNER_KEY = "OnlyWithoutPlanner"
    private const val WATER_IGNORE_GROWN_KEY = "IgnoreWillFullyGrow"
    private const val FLAT_WATER_KEY = "AssumeFlatWaterLoss"
    private const val HUD_ANYWHERE_KEY = "HudAnywhere"

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
        registerListeners()
    }

    /** Registers every object of this feature on the bus, so none is left waiting to be referenced before it listens. */
    @Suppress("UNUSED_EXPRESSION")
    private fun registerListeners() {
        EventBus.register(GreenhouseData)
        SkyBlockAPI.eventBus.register(GreenhouseData)
        EventBus.register(GreenhouseWatering)
        EventBus.register(PlantWarnings)
        EventBus.register(GreenhouseKey)
        EventBus.register(GreenhouseHud)
        EventBus.register(LayoutRenderState)
        EventBus.register(OtherProfiles)
        EventBus.register(ChorusCollision)
        EventBus.register(GreenhouseWarnings)
        EventBus.register(PlannerNeeds)
        CropRegistry

        // the attribute api only registers its listeners once something references it, so it is
        // referenced here rather than the first time a value is asked of it
        AttributeAPI
    }

    fun keyWorksAnywhere(): Boolean = baseSetting.getChild<BooleanSetting>(KEY_ANYWHERE)?.value == true

    fun turnsGridWithPlayer(): Boolean = baseSetting.getChild<BooleanSetting>(TURN_GRID_KEY)?.value == true

    private val plantTransparencySetting = IntSetting(
        key = PLANT_TRANSPARENCY_KEY,
        displayName = "Plant Transparency",
        description = "How much of the world shows through the planner's ghost blocks and stands",
        value = 25,
        range = 0..100,
        step = 5,
        scrollable = false
    )

    /** how solid a planned plant is drawn, 0 to 255 */
    @JvmStatic
    fun plantAlpha(): Int = 255 * (100 - plantTransparencySetting.value) / 100

    private val harvestOnlyTargetsSetting = BooleanSetting(
        key = HARVEST_ONLY_TARGETS_KEY,
        displayName = "Only highlight based on the Target marking from the assigned preset",
        description = "Only the target slots of the assigned preset are watched: green on a target " +
                "mutation ready to harvest, and red on anything else growing in its slot",
        value = false
    )

    private val harvestHighlightSetting = BooleanSetting(
        key = HARVEST_HIGHLIGHT_KEY,
        displayName = "Harvest Highlight",
        description = "Pulses green on every mutation ready to harvest in the greenhouse you stand in",
        value = true,
        children = listOf(harvestOnlyTargetsSetting)
    )

    fun harvestHighlightOn(): Boolean = baseSetting.value && harvestHighlightSetting.value

    fun harvestHighlightOnlyTargets(): Boolean = harvestOnlyTargetsSetting.value

    /** One colour of the planner, written as hex. Blank or unreadable leaves the mark its own colour. */
    private val plannerColorSettings: Map<PlannerMark, TextSetting> = PlannerMark.entries.associateWith { mark ->
        TextSetting(
            key = "Color${mark.name}",
            displayName = mark.displayName,
            description = "The colour this is marked in, as hex such as FF3333. Left blank, the " +
                    "default is used",
            value = ""
        )
    }

    private val plannerColorsGroup = ParentSetting(
        key = PLANNER_COLORS_KEY,
        displayName = "Planner Colours",
        description = "The colours the planner marks a greenhouse in",
        children = plannerColorSettings.values.toList()
    )

    /** The colour [mark] is drawn in, which is the player's when they typed a readable one. */
    fun plannerColor(mark: PlannerMark): Int {
        val typed = plannerColorSettings[mark]?.value?.trim()?.removePrefix("#")?.removePrefix("0x")
        val rgb = typed?.takeIf { it.length == 6 }?.toIntOrNull(16) ?: return mark.defaultColor

        return rgb or 0xFF000000.toInt()
    }

    private val waterOnlyWithoutPlannerSetting = BooleanSetting(
        key = WATER_ONLY_WITHOUT_PLANNER_KEY,
        displayName = "Only Without Planner",
        description = "Only render the water indicator when the planner has nothing to show",
        value = true
    )

    private val waterIgnoreGrownSetting = BooleanSetting(
        key = WATER_IGNORE_GROWN_KEY,
        displayName = "Ignore crops that will fully grow",
        description = "Ignore crops that have enough water to reach full growth with no negative " +
                "water values, so your plant doesnt skip a tick.",
        value = false
    )

    private val waterIndicatorSetting = BooleanSetting(
        key = WATER_INDICATOR_KEY,
        displayName = "Water Highlight",
        description = "Marks the soil of every plant below full water in the greenhouse you stand in. " +
                "A plant whose water is unknown is left alone",
        value = true,
        children = listOf(waterOnlyWithoutPlannerSetting, waterIgnoreGrownSetting)
    )

    fun waterIndicatorOn(): Boolean = baseSetting.value && waterIndicatorSetting.value

    fun waterIndicatorOnlyWithoutPlanner(): Boolean = waterOnlyWithoutPlannerSetting.value

    fun waterIndicatorIgnoresGrown(): Boolean = waterIgnoreGrownSetting.value

    private val assumeFlatWaterSetting = BooleanSetting(
        key = FLAT_WATER_KEY,
        displayName = "Assume No Water Retain Or Drain",
        description = "Assumes water retain and drain are bugged for prediction.\n\n" +
                "§7§oSkyBlock is a great, consistent game, where water retain has been observed to " +
                "work on multiple occasions, showing the base water drain of -20 rectified to -15 and " +
                "-10 with 50% and 100% water retain respectively; and even with water drain, it " +
                "consumes 23, which is perfect. But in another case, no matter the water retain or " +
                "drain, it is a flat -20 with absolutely no consistency. If you have further findings " +
                "about this, please let me know. This is exactly the kind of consistency tied to " +
                "SkyBlock: no consistency.",
        value = false
    )

    /** Whether every prediction takes the plain loss, retain and drain set aside until they are trusted. */
    fun assumeFlatWater(): Boolean = baseSetting.value && assumeFlatWaterSetting.value

    private val hudAnywhereSetting = BooleanSetting(
        key = HUD_ANYWHERE_KEY,
        displayName = "Anywhere In SkyBlock",
        description = "Shows the panel anywhere in SkyBlock rather than only in your own garden. " +
                "Outside a greenhouse it shows the next tick only",
        value = false
    )

    fun hudAnywhere(): Boolean = hudAnywhereSetting.value

    private fun warningsSetting(): BooleanSetting? = baseSetting.getChild<BooleanSetting>(WARNINGS_KEY)
    private fun typesSetting(): BooleanSetting? = warningsSetting()?.getChild<BooleanSetting>(TYPES_KEY)

    private fun warnings(): BooleanSetting? = warningsSetting()?.takeIf { it.value }
    private fun types(): BooleanSetting? = warnings()?.let { typesSetting() }?.takeIf { it.value }
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

    /** How many growth ticks the player says they will be away for, whether or not the warning is on. */
    fun chorusAbsenceTicks(): Int? = typesSetting()
        ?.getChild<BooleanSetting>(CHORUS_KEY)
        ?.getChild<IntSetting>(CHORUS_TICKS_KEY)
        ?.value
    override val id = "GreenhousePresets"
    override val displayName = "Greenhouse Presets"
    override val description = "Enables Greenhouse Presets..."
    override val category = "farming"

    override val baseSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(
            ParentSetting(
                key = PLANNER_OPTIONS_KEY,
                displayName = "Planner Options",
                description = "How a plan running on a greenhouse is shown",
                children = listOf(plantTransparencySetting, plannerColorsGroup)
            ),
            ParentSetting(
                key = PLANT_HIGHLIGHTS_KEY,
                displayName = "Plant Highlights",
                description = "What is marked on the plants of the greenhouse you stand in",
                children = listOf(harvestHighlightSetting, waterIndicatorSetting)
            ),
            BooleanSetting(
                key = WARNINGS_KEY,
                displayName = "Warnings",
                description = "Chat warnings about the greenhouses: which ones, and how far ahead",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = TYPES_KEY,
                        displayName = "Types",
                        description = "Which warnings are sent. Off, none are",
                        value = false,
                        children = listOf(
                            BooleanSetting(
                                key = PlantWarnings.HARVEST_KEY,
                                displayName = "Ready To Harvest",
                                description = "Tells you when a mutation you grew has nothing left to grow",
                                value = false
                            ),
                            BooleanSetting(
                                key = GreenhouseData.THIRST_KEY,
                                displayName = "Dying Of Thirst",
                                description = "Warns before a growth tick kills a plant that has run out of water",
                                value = false
                            ),
                            BooleanSetting(
                                key = PlantWarnings.DECAY_KEY,
                                displayName = "Decay",
                                description = "Warns six hours, one hour, twenty, five and one minute before a plant " +
                                        "rots away. Needs a plant diagnostic to have been used on the plant, " +
                                        "since nothing else says how old it is",
                                value = false
                            ),
                            BooleanSetting(
                                key = PlantWarnings.SNOOZLING_KEY,
                                displayName = "Snoozling Asleep",
                                description = "Warns when a snoozling has dropped asleep, which it does on reaching " +
                                        "stage 5, 10 and 15, and grows no further until it is woken",
                                value = false
                            ),
                            BooleanSetting(
                                key = PlantWarnings.NOCTILUME_KEY,
                                displayName = "Noctilume Time",
                                description = "Warns while a noctilume craves a time of day the garden is not on, " +
                                        "since it stalls every tick until the garden time is changed",
                                value = false
                            ),
                            BooleanSetting(
                                key = CHORUS_KEY,
                                displayName = "Chorus Collision",
                                description = "Warns before a chorus fruit runs out of tiles to teleport into and " +
                                        "starts destroying the plot around it",
                                value = false,
                                children = listOf(
                                    IntSetting(
                                        key = CHORUS_TICKS_KEY,
                                        displayName = "Ticks Away",
                                        description = "How many growth ticks you expect to be away for. The line " +
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
                                description = "Lets the greenhouses of your other profiles warn too, moved on by their " +
                                        "own clocks as if you were away. Each warning says which profile it is about",
                                value = false
                            )
                        )
                    ),
                    BooleanSetting(
                        key = REMINDERS_KEY,
                        displayName = "Reminders",
                        description = "When a warning about the next tick is sent. Off, none are",
                        value = false,
                        children = listOf(
                            BooleanSetting(
                                key = AT_TICK_KEY,
                                displayName = "At The Tick",
                                description = "The moment a growth tick lands",
                                value = false
                            ),
                            BooleanSetting(
                                key = TEN_MINUTES_KEY,
                                displayName = "10 Minutes Before",
                                description = "Ten minutes before the next growth tick",
                                value = false
                            ),
                            BooleanSetting(
                                key = FIVE_MINUTES_KEY,
                                displayName = "5 Minutes Before",
                                description = "Five minutes before the next growth tick",
                                value = false
                            ),
                            BooleanSetting(
                                key = ONE_MINUTE_KEY,
                                displayName = "1 Minute Before",
                                description = "One minute before the next growth tick",
                                value = false
                            )
                        )
                    )
                )
            ),
            BooleanSetting(
                key = GreenhouseHud.KEY,
                displayName = "Greenhouse HUD",
                description = "A small panel on screen in your garden: the next tick, and in a greenhouse " +
                        "how many plants are ready, dry, asleep or about to rot",
                value = false,
                children = listOf(hudAnywhereSetting)
            ),
            assumeFlatWaterSetting,
            BooleanSetting(
                key = KEY_ANYWHERE,
                displayName = "Greenhouse Screen Anywhere",
                description = "Lets the greenhouse screen key (G unless rebound) open the screen " +
                        "outside the garden too. Off, it only works while on the garden",
                value = false
            ),
            BooleanSetting(
                key = TURN_GRID_KEY,
                displayName = "Turn Grid With Player",
                description = "Turns the greenhouse screen's grid so the way you are facing is up. " +
                        "Only the picture turns: plans still go on the same tiles",
                value = false
            )
        )
    )
}
