package org.magic.magicaddons.features.misc

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.data.config.ActionSetting
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.compat.McCompat


object HighlightMarkers : Feature() {

    override val id: String = "HighlightMarkers"
    override val displayName: String = "Highlight Markers"
    override val description: String = "§fMarks a highlighted mob that is too far away to make out,\n" +
            "§fwith what it is and where to turn to see it."
    override val category: String = "misc"

    val iconSetting = BooleanSetting(
        key = "MarkerIcon",
        displayName = "Mob Icon",
        description = "§fDraws what a highlighted mob is over it once it is far enough away,\n" +
                "§fsince an outline that far off is only a dot.",
        value = false
    )

    val arrowsSetting = BooleanSetting(
        key = "MarkerArrows",
        displayName = "Screen Edge Arrows",
        description = "§fMarks a mob that is off screen on the edge nearest it,\n" +
                "§fpointing the way to turn.",
        value = false
    )

    val alwaysNameSetting = BooleanSetting(
        key = "MarkerAlwaysName",
        displayName = "Always Show Name",
        description = "§fWrites every marked mob's name beside it.\n" +
                "§fOff, a name is only written when two marked mobs look alike.",
        value = false
    )

    val tracerSetting = BooleanSetting(
        key = "MarkerTracer",
        displayName = "Tracer To Nearest",
        description = "§fDraws a line from the middle of the screen to the closest marked mob.",
        value = false
    )

    val distanceSetting = IntSetting(
        key = "MarkerDistance",
        displayName = "Icon Past Distance",
        description = "§fHow far away a mob has to be before it is marked.",
        value = 50,
        range = 20..200,
        step = 5
    )

    private val featuresSetting = ToggleListSetting(
        key = "MarkedFeatures",
        displayName = "Features to use markers with:",
        description = "",
        value = mutableListOf(),
        choices = { highlightFeatures().map { it.displayName } },
        searchable = false
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        needsExtensionPack = true,
        children = listOf(
            iconSetting,
            arrowsSetting,
            alwaysNameSetting,
            tracerSetting,
            distanceSetting,
            featuresSetting
        )
    )

    private fun highlightFeatures(): List<HighlightFeature> =
        FeatureManager.availableFeatures.filterIsInstance<HighlightFeature>()

    fun linkSetting(): ActionSetting = ActionSetting(
        key = "MarkerOptions",
        displayName = "Navigate to highlight marker configuration",
        description = "",
        buttonLabel = "Navigate To",
        onPressed = { navigateToFeature() }
    )

    private fun navigateToFeature() {
        (McCompat.currentScreen() as? ConfigScreen)?.showSetting(this, listOf(baseSetting))
    }


    fun markingEnabled(): Boolean = baseSetting.value && (iconSetting.value || arrowsSetting.value)

    @JvmStatic
    fun markingReplacesOutline(entity: Entity, source: EntityUtils.HighlightSource): Boolean {
        if (!markingEnabled() || !marks(source)) return false

        val player = Minecraft.getInstance().player ?: return false

        return entity.distanceToSqr(player) >= distanceSetting.value.toDouble() * distanceSetting.value
    }

    fun marks(source: EntityUtils.HighlightSource): Boolean {
        val feature = source as? Feature ?: return false

        return featuresSetting.value.any { entry: ListEntry -> entry.enabled && entry.value == feature.displayName }
    }
}
