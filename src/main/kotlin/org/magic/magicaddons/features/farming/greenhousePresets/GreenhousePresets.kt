package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.data.config.ParentSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Chloronite
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Noctilume
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Snoozling
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.OtherProfiles
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.PlantBars
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.StatsWidget
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.BreakProtection
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhouseKey
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhousePlantDischarge
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhouseWatering
import org.magic.magicaddons.features.farming.greenhousePresets.render.LayoutRenderState
import org.magic.magicaddons.features.farming.greenhousePresets.render.PlannerMark
import org.magic.magicaddons.features.farming.greenhousePresets.render.WaterIndicator
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.ChorusCollision
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI

object GreenhousePresets : Feature() {

    private const val KEY_ANYWHERE = "GreenhouseKeyAnywhere"
    private const val TURN_GRID_KEY = "TurnGridWithPlayer"
    private const val SCREEN_KEY = "Screen"
    private const val PLANNER_OPTIONS_KEY = "PlannerOptions"
    private const val BREAK_PROTECTION_KEY = "BreakProtection"
    private const val PREDICTION_KEY = "Prediction"
    private const val PLANT_TRANSPARENCY_KEY = "PlantTransparency"
    private const val PLANT_HIGHLIGHTS_KEY = "PlantHighlights"
    private const val HARVEST_HIGHLIGHT_KEY = "HarvestHighlight"
    private const val PREVENT_BREAKING_INGREDIENTS_KEY = "PreventBreakingIngredients"
    private const val PREVENT_BREAKING_GROWING_KEY = "PreventBreakingGrowingMutations"
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

    @Suppress("UNUSED_EXPRESSION")
    private fun registerListeners() {
        EventBus.register(GreenhouseData)
        SkyBlockAPI.eventBus.register(GreenhouseData)
        EventBus.register(GreenhouseWatering)
        EventBus.register(GreenhousePlantDischarge)
        EventBus.register(BreakProtection)
        SkyBlockAPI.eventBus.register(BreakProtection)
        SkyBlockAPI.eventBus.register(StatsWidget)
        EventBus.register(PlantBars)
        EventBus.register(PlantWarnings)
        EventBus.register(GreenhouseKey)
        EventBus.register(GreenhouseHud)
        EventBus.register(LayoutRenderState)
        EventBus.register(OtherProfiles)
        EventBus.register(ChorusCollision)
        EventBus.register(PlannerNeeds)
        CropRegistry
        AttributeAPI
    }

    private val screenAnywhereSetting = BooleanSetting(
        key = KEY_ANYWHERE,
        displayName = "Open Anywhere",
        description = "Lets the greenhouse screen key (G unless rebound) open the screen " +
                "outside the garden too. Off, it only works while on the garden",
        value = false
    )

    private val turnGridSetting = BooleanSetting(
        key = TURN_GRID_KEY,
        displayName = "Turn Grid With Player",
        description = "Turns the greenhouse screen's grid so the way you are facing is up. " +
                "Only the picture turns: plans still go on the same tiles",
        value = false
    )

    fun keyWorksAnywhere(): Boolean = screenAnywhereSetting.value

    fun turnsGridWithPlayer(): Boolean = turnGridSetting.value

    private val plantTransparencySetting = IntSetting(
        key = PLANT_TRANSPARENCY_KEY,
        displayName = "Plant Transparency",
        description = "How much of the world shows through the planner's ghost blocks and stands",
        value = 25,
        range = 0..100,
        step = 5,
        scrollable = false
    )

    @JvmStatic
    fun plantAlpha(): Int = 255 * (100 - plantTransparencySetting.value) / 100

    private val harvestOnlyTargetsSetting = BooleanSetting(
        key = HARVEST_ONLY_TARGETS_KEY,
        displayName = "Only highlight based on the Target marking from the assigned preset",
        description = "Only the target slots of the assigned preset are watched: green on a target " +
                "mutation ready to harvest, and red on anything else growing in its slot",
        value = false
    )

    private val preventBreakingIngredientsSetting = BooleanSetting(
        key = PREVENT_BREAKING_INGREDIENTS_KEY,
        displayName = "Prevent Breaking Ingredients",
        description = "Prevents breaking crops marked as ingredients in the assigned layout for that greenhouse.\n\n" +
                "§7Enabling this option will also prevent ingredient crops showing up in harvest highlight.",
        value = false
    )

    fun preventBreakingIngredients(): Boolean = baseSetting.value && preventBreakingIngredientsSetting.value

    private val jellybeanHarvestStageSetting = IntSetting(
        key = "MagicJellybeanHarvestStage",
        displayName = "Magic Jellybean Harvest Stage",
        description = "Allows breaking a Magic Jellybean once it reaches this stage",
        value = 12,
        range = 12..120,
        step = 12,
        scrollable = false
    )

    private val aloeHarvestStageSetting = IntSetting(
        key = "AllInAloeHarvestStage",
        displayName = "All-in Aloe Harvest Stage",
        description = "Allows breaking an All-in Aloe once it reaches this stage",
        value = 12,
        range = 1..27,
        scrollable = false
    )

    private val preventBreakingGrowingSetting = BooleanSetting(
        key = PREVENT_BREAKING_GROWING_KEY,
        displayName = "Prevent Breaking Growing Mutations",
        description = "Prevents breaking a mutation that has not finished growing.",
        value = false,
        children = listOf(jellybeanHarvestStageSetting, aloeHarvestStageSetting)
    )

    fun preventBreakingGrowingMutations(): Boolean = baseSetting.value && preventBreakingGrowingSetting.value

    fun harvestStageFor(cropName: String): Int? = when (cropName) {
        "Magic Jellybean" -> jellybeanHarvestStageSetting.value
        "All-in Aloe" -> aloeHarvestStageSetting.value
        else -> null
    }

    private val farmingFortuneThresholdSetting = IntSetting(
        key = "FarmingFortuneThreshold",
        displayName = "Farming Fortune Threshold",
        description = "The farming fortune under which a mutation is prevented from being broken",
        value = 1000,
        range = 0..5000,
        step = 50,
        scrollable = false
    )

    private val preventBreakingUnderFarmingFortuneSetting = BooleanSetting(
        key = "PreventBreakingUnderFarmingFortune",
        displayName = "Prevent Breaking Under Farming Fortune",
        description = "Prevents breaking any mutation while your farming fortune is under the threshold.\n\n" +
                "§7Your farming fortune is read off the tab list's Stats widget, which has to be on and have Farming Fortune enabled.",
        value = false,
        children = listOf(farmingFortuneThresholdSetting)
    )

    fun preventBreakingUnderFarmingFortune(): Boolean = baseSetting.value && preventBreakingUnderFarmingFortuneSetting.value
    val farmingFortuneThreshold: Int get() = farmingFortuneThresholdSetting.value
    val farmingFortuneSettingName: String get() = preventBreakingUnderFarmingFortuneSetting.displayName

    private val miningFortuneThresholdSetting = IntSetting(
        key = "MiningFortuneThreshold",
        displayName = "Mining Fortune Threshold",
        description = "The mining fortune under which a Chloronite is prevented from being broken",
        value = 1000,
        range = 0..5000,
        step = 50,
        scrollable = false
    )

    private val preventBreakingChloroniteSetting = BooleanSetting(
        key = "PreventBreakingChloroniteUnderMiningFortune",
        displayName = "Prevent Breaking Chloronite Under Mining Fortune",
        description = "Prevents breaking a Chloronite while your mining fortune is under the threshold.\n\n" +
                "§7Your mining fortune is read off the tab list's Stats widget, which has to be on and have Mining Fortune enabled.",
        value = false,
        children = listOf(miningFortuneThresholdSetting)
    )

    fun preventBreakingChloroniteUnderMiningFortune(): Boolean = baseSetting.value && preventBreakingChloroniteSetting.value
    val miningFortuneThreshold: Int get() = miningFortuneThresholdSetting.value
    val miningFortuneSettingName: String get() = preventBreakingChloroniteSetting.displayName

    private val preventBreakingDuringPestDebuffSetting = BooleanSetting(
        key = "PreventBreakingDuringPestDebuff",
        displayName = "Prevent Breaking While Pest Debuff Is Active",
        description = "Prevents breaking any mutation but Chloronite while pests are lowering your farming fortune.",
        value = false
    )

    fun preventBreakingDuringPestDebuff(): Boolean = baseSetting.value && preventBreakingDuringPestDebuffSetting.value

    private val harvestHighlightSetting = BooleanSetting(
        key = HARVEST_HIGHLIGHT_KEY,
        displayName = "Harvest Highlight",
        description = "Pulses green on every mutation ready to harvest in the greenhouse you stand in",
        value = true,
        children = listOf(harvestOnlyTargetsSetting)
    )

    fun harvestHighlightOn(): Boolean = baseSetting.value && harvestHighlightSetting.value

    fun harvestHighlightOnlyTargets(): Boolean = harvestOnlyTargetsSetting.value

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
        description = "Assumes water retain and drain are bugged for prediction logic",
        value = false
    )

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


    fun warningTypeEnabled(key: String): Boolean = types()?.getChild<BooleanSetting>(key)?.value == true


    fun reminderTimeEnabled(key: String): Boolean = reminders()?.getChild<BooleanSetting>(key)?.value == true

    fun reminderThresholds(): List<Duration> = listOfNotNull(
        Duration.ofMinutes(10).takeIf { reminderTimeEnabled(TEN_MINUTES_KEY) },
        Duration.ofMinutes(5).takeIf { reminderTimeEnabled(FIVE_MINUTES_KEY) },
        Duration.ofMinutes(1).takeIf { reminderTimeEnabled(ONE_MINUTE_KEY) }
    )

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
                key = SCREEN_KEY,
                displayName = "Screen",
                description = "The greenhouse screen itself",
                children = listOf(screenAnywhereSetting, turnGridSetting)
            ),
            ParentSetting(
                key = PLANNER_OPTIONS_KEY,
                displayName = "Planner",
                description = "How a plan running on a greenhouse is shown",
                children = listOf(plantTransparencySetting, plannerColorsGroup)
            ),
            ParentSetting(
                key = PLANT_HIGHLIGHTS_KEY,
                displayName = "Highlights",
                description = "What is marked on the plants of the greenhouse you stand in",
                children = listOf(harvestHighlightSetting, waterIndicatorSetting)
            ),
            ParentSetting(
                key = BREAK_PROTECTION_KEY,
                displayName = "Break Protection",
                description = "Prevents breaking plants under several conditions, configure below.",
                children = listOf(
                    preventBreakingIngredientsSetting,
                    preventBreakingGrowingSetting,
                    preventBreakingUnderFarmingFortuneSetting,
                    preventBreakingChloroniteSetting,
                    preventBreakingDuringPestDebuffSetting
                )
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
                                key = PlantWarnings.THIRST_KEY,
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
            ParentSetting(
                key = PREDICTION_KEY,
                displayName = "Prediction",
                description = "What the growth and water model is told to assume",
                children = listOf(assumeFlatWaterSetting)
            )
        )
    )
}
