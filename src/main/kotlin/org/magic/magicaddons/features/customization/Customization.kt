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

/** What the mod's screens look like. The colours in Common.UI are read from the palette picked here. */
object Customization : Feature() {

    override val id: String = "Customization"
    override val displayName: String = "Appearance"
    override val description: String = "The colours the mod's screens are drawn in"
    override val category: String = CATEGORY

    /** One colour of a custom palette, written as hex. Blank or unreadable falls back to the preset's. */
    private class HexSetting(key: String, displayName: String, val slot: (PaletteColors) -> Int) {
        val setting = TextSetting(
            key = key,
            displayName = displayName,
            description = "A colour as hex, such as 1F3330. Left blank, the preset's colour is used",
            value = ""
        )

        fun colorOr(fallback: PaletteColors): Int {
            val typed = setting.value.trim().removePrefix("#").removePrefix("0x")
            val rgb = typed.takeIf { it.length == 6 }?.toIntOrNull(16) ?: return slot(fallback)

            return rgb or 0xFF000000.toInt()
        }
    }

    private val hexFields = listOf(
        HexSetting("CustomPanel", "Panel") { it.background },
        HexSetting("CustomFrame", "Frame") { it.border },
        HexSetting("CustomField", "Field") { it.field },
        HexSetting("CustomText", "Text") { it.text },
        HexSetting("CustomDimText", "Dim Text") { it.textDim },
        HexSetting("CustomDisabledText", "Disabled Text") { it.disabledText },
        HexSetting("CustomAccent", "Accent") { it.accent }
    )

    /** The preset the custom colours start from, which is whichever was picked before Custom was. */
    private var seededFrom: ColorPalette = ColorPalette.PineAmber

    private val paletteSetting = EnumSetting(
        key = "ColorPalette",
        displayName = "Colour Palette",
        description = "The colours of the mod's panels, frames, text and highlights",
        value = ColorPalette.PineAmber,
        childrenProvider = { picked ->
            if (picked == ColorPalette.Custom) {
                seedCustomFields()
                hexFields.map { it.setting }
            } else {
                seededFrom = picked
                emptyList()
            }
        }
    )

    /** Writes the preset's colours into any custom field still blank, so editing starts from it. */
    private fun seedCustomFields() {
        val from = seededFrom.colors

        hexFields.forEach { field ->
            if (field.setting.value.isBlank()) {
                field.setting.value = "%06X".format(field.slot(from) and 0xFFFFFF)
            }
        }
    }

    /** The palette every screen draws in; turned off, the mod goes back to the colours it ships with. */
    val palette: PaletteColors
        get() {
            if (!baseSetting.value) return ColorPalette.PineAmber.colors
            if (paletteSetting.value != ColorPalette.Custom) return paletteSetting.value.colors

            val fallback = seededFrom.colors

            return PaletteColors(
                background = hexFields[0].colorOr(fallback),
                border = hexFields[1].colorOr(fallback),
                field = hexFields[2].colorOr(fallback),
                text = hexFields[3].colorOr(fallback),
                textDim = hexFields[4].colorOr(fallback),
                disabledText = hexFields[5].colorOr(fallback),
                accent = hexFields[6].colorOr(fallback)
            )
        }

    private val borderThicknessSetting = IntSetting(
        key = "BorderThickness",
        displayName = "Border Thickness",
        description = "How thick the frame around every panel is drawn",
        value = 2,
        range = 1..4,
        scrollable = false
    )

    /** How thick a panel's frame is drawn. */
    val borderSize: Int get() = if (baseSetting.value) borderThicknessSetting.value else 2

    private val hoverStrengthSetting = IntSetting(
        key = "HoverStrength",
        displayName = "Hover Strength",
        description = "How brightly a control lights up under the mouse",
        value = 16,
        range = 0..60,
        step = 2,
        scrollable = false
    )

    /** The white laid over a control the mouse is on. */
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
        scrollable = false
    )

    private val panelTransparencySetting = IntSetting(
        key = "PanelTransparency",
        displayName = "Panel Transparency",
        description = "How much of what is behind them shows through the mod's panels",
        value = 0,
        range = 0..100,
        step = 5,
        scrollable = false
    )

    private val borderTransparencySetting = IntSetting(
        key = "BorderTransparency",
        displayName = "Border Transparency",
        description = "How much of what is behind them shows through the mod's frames and dividers",
        value = 0,
        range = 0..100,
        step = 5,
        scrollable = false
    )

    /** A text colour with the text transparency taken out of it. */
    fun fadedText(color: Int): Int = faded(color, textTransparencySetting.value)

    /** A panel colour with the panel transparency taken out of it. */
    fun fadedPanel(color: Int): Int = faded(color, panelTransparencySetting.value)

    /** A frame or divider colour with the border transparency taken out of it. */
    fun fadedBorder(color: Int): Int = faded(color, borderTransparencySetting.value)

    private fun faded(color: Int, transparency: Int): Int {
        if (!baseSetting.value || transparency <= 0) return color
        if (transparency >= 100) return color and 0xFFFFFF

        // alpha and how faint something looks are not the same: taking a straight tenth of the alpha
        // leaves a colour already invisible, so the slider is curved to keep the last steps apart
        val left = sqrt(1.0 - transparency / 100.0)
        val alpha = (((color ushr 24) and 0xFF) * left).roundToInt().coerceIn(0, 0xFF)

        return (color and 0xFFFFFF) or (alpha shl 24)
    }

    /** Puts every appearance setting back to what the mod ships with. */
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
        prefixColourSetting.value = PrefixColour.Default
        prefixHexSetting.value = ""
        hexFields.forEach { it.setting.value = "" }
        ConfigBackground.forgetLoadedPicture()
    }

    /**
     * Puts into effect what was written straight into the settings rather than picked in the ui: the
     * font has to be installed before it can be drawn with, and the background picture is cached.
     */
    fun reapplyAppearance() {
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

    /** How the picture is fitted to the space behind the settings. */
    val backgroundFit: BackgroundFit get() = backgroundFitSetting.value

    private val backgroundDimSetting = IntSetting(
        key = "BackgroundDim",
        displayName = "Dim",
        description = "How much black is laid over the picture, so writing on it stays readable",
        value = 40,
        range = 0..100,
        step = 5,
        scrollable = false
    )

    /** The black laid over the picture, or zero when none is asked for. */
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

    /** Whether the picture is drawn behind the screen named. */
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

    /** Where the picture behind the config screen comes from; turned off, there is no picture. */
    val backgroundSource: BackgroundSource
        get() = if (baseSetting.value) backgroundSetting.value else BackgroundSource.None

    /** The picture in the mod's backgrounds folder that is picked, empty when none is. */
    val backgroundFile: String get() = savedPictureSetting.value

    /** The link the picture is read from. */
    val backgroundUrl: String get() = urlSetting.value

    /** Points the saved picture setting at a file just added, so a new pick shows at once. */
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
        // scrolling the settings past it would otherwise resize the whole screen under the mouse
        scrollable = false,
        detail = {
            SettingDetail.Text(
                "Unexpected behavior may occur with extreme values.",
                Common.UI.DANGER_COLOR
            )
        }
    )

    /** What the mod's screens multiply their own drawing scale by. */
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
        value = PrefixColour.Default,
        childrenProvider = { picked ->
            if (picked == PrefixColour.Custom) listOf(prefixHexSetting) else emptyList()
        }
    )

    /** The colour the chat prefix is drawn in. */
    val prefixColour: Int
        get() {
            if (!baseSetting.value) return PrefixColour.Default.rgb

            val picked = prefixColourSetting.value
            if (picked == PrefixColour.FollowAccent) return palette.accent and 0xFFFFFF
            if (picked != PrefixColour.Custom) return picked.rgb

            val typed = prefixHexSetting.value.trim().removePrefix("#").removePrefix("0x")

            return typed.takeIf { it.length == 6 }?.toIntOrNull(16) ?: PrefixColour.Default.rgb
        }

    private val fontSetting = ChoiceSetting(
        key = "Font",
        displayName = "Font",
        description = "What the mod's screens write in: the game's fonts, or one installed on this computer",
        value = SystemFonts.defaultName,
        options = { SystemFonts.choices() },
        confirm = { picked ->
            if (SystemFonts.isBuiltIn(picked)) {
                null
            } else {
                ChoiceSetting.Confirmation(
                    question = "Use this font? The game will reload its resources.",
                    warning = if (SystemFonts.coverage(picked) < SystemFonts.WARN_BELOW) {
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

    /** The font the mod's screens write in, or null for the game's own, which needs no style. */
    val fontId: Identifier?
        get() = if (baseSetting.value) SystemFonts.fontIdFor(fontSetting.value) else null

    private val textShadowSetting = BooleanSetting(
        key = "TextShadow",
        displayName = "Text Shadow",
        description = "Draws a shadow under the mod's writing, which helps over a busy background",
        value = false
    )

    /** Whether the mod's screens draw their writing with a shadow under it. */
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
        description = "A picture behind the settings, from this computer or from a link",
        children = listOf(backgroundSetting, backgroundScreensSetting)
    )

    /** Everything a preset is taken from: the whole look, minus the list of presets itself. */
    private val appearanceGroup = ParentSetting(
        key = "Appearance",
        displayName = "Appearance",
        description = "How the mod's screens are drawn",
        children = listOf(interfaceGroup, coloursGroup, textGroup, backgroundGroup)
    )

    private val presetSetting = PresetLibrarySetting(
        key = "UiPresets",
        displayName = "UI Presets",
        description = "Saved looks: pick one to put it on, or name one and save what is set now",
        subject = { appearanceGroup },
        defaultName = "Default"
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        // the palette applies whatever this says, so it is on and stays on
        value = true,
        children = listOf(presetSetting, appearanceGroup)
    )

    const val CATEGORY: String = "customization"

    const val CONFIG_SCREEN: String = "Config"
    const val GREENHOUSE_SCREEN: String = "Greenhouse"
    const val PREVIEW_SCREEN: String = "Crop Preview"
}
