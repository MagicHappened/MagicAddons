package org.magic.magicaddons.features.customization

import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.ActionSetting
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.ChoiceSetting
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.data.config.ParentSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.data.config.PresetLibrarySetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.ui.ColorPalette
import org.magic.magicaddons.ui.PaletteColors
import org.magic.magicaddons.ui.background.BackgroundFit
import org.magic.magicaddons.ui.background.BackgroundSource
import org.magic.magicaddons.ui.fonts.SystemFonts
import net.minecraft.resources.Identifier
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.magic.magicaddons.ui.background.ConfigBackground

object Customization : Feature() {

    override val id: String = "Customization"
    override val displayName: String = "Appearance"
    override val description: String = "The colours the mod's screens are drawn in"
    override val category: String = CATEGORY

    private class CustomColorField(key: String, displayName: String, val slot: (PaletteColors) -> Int) {
        val setting = TextSetting(
            key = key,
            displayName = displayName,
            description = "A colour as hex, such as 1F3330.",
            value = ""
        )

        fun colorOr(fallback: PaletteColors): Int {
            val typed = setting.value.trim().removePrefix("#").removePrefix("0x")
            val rgb = typed.takeIf { it.length == 6 }?.toIntOrNull(16) ?: return slot(fallback)

            return rgb or 0xFF000000.toInt()
        }
    }

    private val customColorFields = listOf(
        CustomColorField("CustomPanel", "Panel") { it.background },
        CustomColorField("CustomFrame", "Frame") { it.border },
        CustomColorField("CustomField", "Field") { it.field },
        CustomColorField("CustomText", "Text") { it.text },
        CustomColorField("CustomDimText", "Dim Text") { it.textDim },
        CustomColorField("CustomDisabledText", "Disabled Text") { it.disabledText },
        CustomColorField("CustomAccent", "Accent") { it.accent }
    )

    private var prefilledFromPalette: ColorPalette = ColorPalette.PineAmber

    private val paletteSetting = EnumSetting(
        key = "ColorPalette",
        displayName = "Colour Palette",
        description = "The colours of the mod's panels, frames, text and highlights",
        value = ColorPalette.PineAmber,
        childrenProvider = { picked ->
            if (picked == ColorPalette.Custom) {
                prefillCustomValues()
                customColorFields.map { it.setting }
            } else {
                prefilledFromPalette = picked
                emptyList()
            }
        }
    )

    private fun prefillCustomValues() {
        val from = prefilledFromPalette.colors

        customColorFields.forEach { field ->
            if (field.setting.value.isBlank()) {
                field.setting.value = "%06X".format(field.slot(from) and 0xFFFFFF)
            }
        }
    }

    val palette: PaletteColors
        get() {
            if (!baseSetting.value) return ColorPalette.PineAmber.colors
            if (paletteSetting.value != ColorPalette.Custom) return paletteSetting.value.colors

            val fallback = prefilledFromPalette.colors

            return PaletteColors(
                background = customColorFields[0].colorOr(fallback),
                border = customColorFields[1].colorOr(fallback),
                field = customColorFields[2].colorOr(fallback),
                text = customColorFields[3].colorOr(fallback),
                textDim = customColorFields[4].colorOr(fallback),
                disabledText = customColorFields[5].colorOr(fallback),
                accent = customColorFields[6].colorOr(fallback)
            )
        }

    private val borderThicknessSetting = IntSetting(
        key = "BorderThickness",
        displayName = "Border Thickness",
        description = "How thick the frame around every panel is drawn",
        value = 2,
        range = 1..4,
        mouseScrollEnabled = false
    )

    val borderSize: Int get() = if (baseSetting.value) borderThicknessSetting.value else 2

    private val hoverStrengthSetting = IntSetting(
        key = "HoverStrength",
        displayName = "Hover Strength",
        description = "How brightly a control lights up under the mouse",
        value = 16,
        range = 0..60,
        step = 2,
        mouseScrollEnabled = false
    )

    val hoverWash: Int
        get() {
            val strength = if (baseSetting.value) hoverStrengthSetting.value else 16
            val alpha = (strength * 0xFF / 100).coerceIn(0, 0xFF)

            return (alpha shl 24) or 0xFFFFFF
        }

    private val textTransparencySetting = IntSetting(
        key = "TextTransparency",
        displayName = "Text Transparency",
        description = "How much of what is behind it shows through the mod's text",
        value = 0,
        range = 0..100,
        step = 5,
        mouseScrollEnabled = false
    )

    private val panelTransparencySetting = IntSetting(
        key = "PanelTransparency",
        displayName = "Panel Transparency",
        description = "How much of what is behind them shows through the mod's panels",
        value = 0,
        range = 0..100,
        step = 5,
        mouseScrollEnabled = false
    )

    private val borderTransparencySetting = IntSetting(
        key = "BorderTransparency",
        displayName = "Border Transparency",
        description = "How much of what is behind them shows through the mod's frames and dividers",
        value = 0,
        range = 0..100,
        step = 5,
        mouseScrollEnabled = false
    )

    fun fadedText(color: Int): Int = faded(color, textTransparencySetting.value)

    fun fadedPanel(color: Int): Int = faded(color, panelTransparencySetting.value)

    fun fadedBorder(color: Int): Int = faded(color, borderTransparencySetting.value)

    private fun faded(color: Int, transparency: Int): Int {
        if (!baseSetting.value || transparency <= 0) return color
        if (transparency >= 100) return color and 0xFFFFFF

        // a linear step would make the last alpha steps appear invisible anyway
        val alphaKept = sqrt(1.0 - transparency / 100.0)
        val alpha = (((color ushr 24) and 0xFF) * alphaKept).roundToInt().coerceIn(0, 0xFF)

        return (color and 0xFFFFFF) or (alpha shl 24)
    }

    fun restoreDefaults() {
        paletteSetting.value = ColorPalette.PineAmber
        backgroundSetting.value = BackgroundSource.None
        backgroundFitSetting.value = BackgroundFit.Cover
        textTransparencySetting.value = 0
        panelTransparencySetting.value = 0
        borderTransparencySetting.value = 0
        borderThicknessSetting.value = 2
        hoverStrengthSetting.value = 16
        uiScaleSetting.value = 100
        fontSetting.value = SystemFonts.defaultName
        textShadowSetting.value = false
        prefixColourSetting.value = ChatPrefixColour.Default
        prefixHexSetting.value = ""
        customColorFields.forEach { it.setting.value = "" }
        ConfigBackground.forgetLoadedPicture()
    }

    fun applyImportedAppearance() {
        val font = fontSetting.value
        if (!SystemFonts.isBuiltIn(font)) SystemFonts.install(font)

        ConfigBackground.forgetLoadedPicture()
    }

    private val savedPictureSetting = ChoiceSetting(
        key = "SavedBackground",
        displayName = "Saved Image",
        description = "One of the pictures already in the mod's backgrounds folder",
        options = { ConfigBackground.savedPictureFiles() },
        onChosen = { ConfigBackground.forgetLoadedPicture() }
    )

    private val localFileSetting = ActionSetting(
        key = "LocalBackground",
        displayName = "Add An Image",
        description = "Copies a picture from this computer into the mod's backgrounds folder",
        buttonLabel = "Choose…",
        onPressed = { ConfigBackground.choosePictureFile(it) }
    )

    private val urlSetting = TextSetting(
        key = "BackgroundUrl",
        displayName = "Image Link",
        description = "A link the picture is read from, followed again as it changes",
        value = "",
        detail = {
            SettingDetail.Text(
                "Only follow links you trust. Whoever owns it can change the picture at any time, " +
                        "and the mod re-reads it while this screen is open.",
                Common.UI.DANGER_COLOR
            )
        }
    )

    private val saveLinkSetting = ActionSetting(
        key = "SaveLinkBackground",
        displayName = "Keep A Copy",
        description = "Writes what the link last gave into the backgrounds folder",
        buttonLabel = "Save to config",
        onPressed = { ConfigBackground.saveLinkPicture() }
    )

    private val backgroundFitSetting = EnumSetting(
        key = "BackgroundFit",
        displayName = "Fit",
        description = "How the picture is fitted when it is not the same shape as the space",
        value = BackgroundFit.Cover
    )

    val backgroundFit: BackgroundFit get() = backgroundFitSetting.value

    private val backgroundDimSetting = IntSetting(
        key = "BackgroundDim",
        displayName = "Dim",
        description = "How much black is laid over the picture, so writing on it stays readable",
        value = 40,
        range = 0..100,
        step = 5,
        mouseScrollEnabled = false
    )

    val backgroundDim: Int
        get() {
            val alpha = (backgroundDimSetting.value * 0xFF / 100).coerceIn(0, 0xFF)

            return alpha shl 24
        }

    private val deletePictureSetting = ActionSetting(
        key = "DeleteBackground",
        displayName = "Remove An Image",
        description = "Deletes the picture picked above from the mod's backgrounds folder",
        buttonLabel = "Delete",
        onPressed = { ConfigBackground.deletePictureFile(savedPictureSetting.value) }
    )

    private val openFolderSetting = ActionSetting(
        key = "OpenBackgroundFolder",
        displayName = "Backgrounds Folder",
        description = "Opens the folder the pictures are kept in",
        buttonLabel = "Open",
        onPressed = { ConfigBackground.openPictureFolder() }
    )

    private val backgroundScreensSetting = ToggleListSetting(
        key = "BackgroundScreens",
        displayName = "Background Image Location",
        description = "Which of the mod's screens the picture is drawn behind",
        value = mutableListOf(ListEntry(CONFIG_SCREEN, CONFIG_SCREEN, true)),
        choices = { listOf(CONFIG_SCREEN, GREENHOUSE_SCREEN, PREVIEW_SCREEN) },
        searchable = false
    )

    fun backgroundShowsOn(screen: String): Boolean =
        backgroundScreensSetting.value.any { it.value == screen && it.enabled }

    private val backgroundSetting = EnumSetting(
        key = "Background",
        displayName = "Image Background",
        description = "A picture behind the settings, from this computer or from a link",
        value = BackgroundSource.None,
        childrenProvider = { source ->
            when (source) {
                BackgroundSource.None -> emptyList()
                BackgroundSource.LocalFile -> listOf(
                    savedPictureSetting, localFileSetting, deletePictureSetting,
                    openFolderSetting, backgroundFitSetting, backgroundDimSetting
                )
                BackgroundSource.Url -> listOf(
                    urlSetting, saveLinkSetting, openFolderSetting,
                    backgroundFitSetting, backgroundDimSetting
                )
            }
        }
    )

    val backgroundSource: BackgroundSource
        get() = if (baseSetting.value) backgroundSetting.value else BackgroundSource.None

    val backgroundFile: String get() = savedPictureSetting.value

    val backgroundUrl: String get() = urlSetting.value

    fun useSavedPicture(name: String) {
        savedPictureSetting.value = name
    }

    private val uiScaleSetting = IntSetting(
        key = "UiScale",
        displayName = "UI Scale",
        description = "How large the mod's own screens are drawn, on top of the game's gui scale",
        value = 100,
        range = 50..200,
        step = 10,
        mouseScrollEnabled = false,
        detail = {
            SettingDetail.Text(
                "Unexpected behavior may occur with extreme values.",
                Common.UI.DANGER_COLOR
            )
        }
    )

    val uiScale: Float get() = if (baseSetting.value) uiScaleSetting.value / 100f else 1f

    private val interfaceGroup = ParentSetting(
        key = "Interface",
        displayName = "Interface",
        description = "How the mod's screens are sized",
        children = listOf(uiScaleSetting)
    )

    private val coloursGroup = ParentSetting(
        key = "Colours",
        displayName = "Colours",
        description = "The palette the mod draws in, and how solid its panels and frames are",
        children = listOf(
            paletteSetting,
            borderThicknessSetting,
            hoverStrengthSetting,
            panelTransparencySetting,
            borderTransparencySetting
        )
    )

    private val prefixHexSetting = TextSetting(
        key = "PrefixHex",
        displayName = "Prefix Colour",
        description = "The chat prefix colour as hex, such as FFAA00. Unreadable falls back to gold",
        value = ""
    )

    private val prefixColourSetting = EnumSetting(
        key = "PrefixColour",
        displayName = "Chat Prefix",
        description = "The colour of the [MA] tag the mod puts before its chat messages",
        value = ChatPrefixColour.Default,
        childrenProvider = { picked ->
            if (picked == ChatPrefixColour.Custom) listOf(prefixHexSetting) else emptyList()
        }
    )

    val prefixColour: Int
        get() {
            if (!baseSetting.value) return ChatPrefixColour.Default.rgb

            val picked = prefixColourSetting.value
            if (picked == ChatPrefixColour.FollowAccent) return palette.accent and 0xFFFFFF
            if (picked != ChatPrefixColour.Custom) return picked.rgb

            val typed = prefixHexSetting.value.trim().removePrefix("#").removePrefix("0x")

            return typed.takeIf { it.length == 6 }?.toIntOrNull(16) ?: ChatPrefixColour.Default.rgb
        }

    private val fontSetting = ChoiceSetting(
        key = "Font",
        displayName = "Font",
        description = "What the mod's screens write in: the game's fonts, or one installed on this computer",
        value = SystemFonts.defaultName,
        options = { SystemFonts.choices() },
        confirm = { pickedFont ->
            if (SystemFonts.isBuiltIn(pickedFont)) {
                null
            } else {
                ChoiceSetting.Confirmation(
                    question = "Use this font? The game will reload its resources.",
                    warning = if (SystemFonts.coverage(pickedFont) < SystemFonts.WARN_BELOW) {
                        "Most of the characters in this font will not render correctly. " +
                                "Are you sure you want to continue?"
                    } else {
                        null
                    }
                )
            }
        },
        onChosen = { picked -> if (!SystemFonts.isBuiltIn(picked.value)) SystemFonts.install(picked.value) }
    )

    val fontId: Identifier?
        get() = if (baseSetting.value) SystemFonts.fontIdFor(fontSetting.value) else null

    private val textShadowSetting = BooleanSetting(
        key = "TextShadow",
        displayName = "Text Shadow",
        description = "Draws a shadow under the mod's writing, which helps over a busy background",
        value = false
    )

    val textShadow: Boolean get() = baseSetting.value && textShadowSetting.value

    private val textGroup = ParentSetting(
        key = "Text",
        displayName = "Text",
        description = "How the mod's writing is drawn, on screen and in chat",
        children = listOf(fontSetting, textTransparencySetting, textShadowSetting, prefixColourSetting)
    )

    private val backgroundGroup = ParentSetting(
        key = "Background",
        displayName = "Background",
        description = "A background image behind the settings, from this computer or from a link",
        children = listOf(backgroundSetting, backgroundScreensSetting)
    )

    private val appearanceGroup = ParentSetting(
        key = "Appearance",
        displayName = "Appearance",
        description = "How the mod's screens are drawn",
        children = listOf(interfaceGroup, coloursGroup, textGroup, backgroundGroup)
    )

    private val presetSetting = PresetLibrarySetting(
        key = "UiPresets",
        displayName = "UI Presets",
        description = "Change how the mod looks, with an option to save as a preset so you can quickly change between variations",
        settingUnder = { appearanceGroup },
        defaultName = "Default"
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = true,
        children = listOf(presetSetting, appearanceGroup)
    )

    const val CATEGORY: String = "customization"

    const val CONFIG_SCREEN: String = "Config"
    const val GREENHOUSE_SCREEN: String = "Greenhouse"
    const val PREVIEW_SCREEN: String = "Crop Preview"
}
