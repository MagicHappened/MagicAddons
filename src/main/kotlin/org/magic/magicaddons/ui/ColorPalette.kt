package org.magic.magicaddons.ui

/**
 * The colours the mod's panels are drawn in. Only the chrome is here: warnings, water and the other
 * colours that carry a meaning stay the same whichever palette is picked.
 */
class PaletteColors(
    /** The ground of a panel. */
    val background: Int,
    /** The frame around a panel, lighter than the panel itself. */
    val border: Int,
    /** The ground of a text field or checkbox, darker than the panel it sits in. */
    val field: Int,
    /** Plain text on a panel. */
    val text: Int,
    /** A label beside the value it names, quieter than the value itself. */
    val textDim: Int,
    /** Text on a control that does nothing at the moment. */
    val disabledText: Int,
    /** The frame of a picked control, and the used part of a slider. */
    val accent: Int
)

enum class ColorPalette(private val label: String, val colors: PaletteColors) {
    PineAmber(
        "Pine & Amber",
        PaletteColors(
            background = 0xFF1F3330.toInt(),
            border = 0xFF4C7A63.toInt(),
            field = 0xFF13221F.toInt(),
            text = 0xFFF2ECDC.toInt(),
            textDim = 0xFFB4C2AF.toInt(),
            disabledText = 0xFF6E8577.toInt(),
            accent = 0xFFE0A84A.toInt()
        )
    ),
    SlateIce(
        "Slate & Ice",
        PaletteColors(
            background = 0xFF1E2630.toInt(),
            border = 0xFF4A6076.toInt(),
            field = 0xFF141A22.toInt(),
            text = 0xFFE8EEF4.toInt(),
            textDim = 0xFFAFBECC.toInt(),
            disabledText = 0xFF6E7F90.toInt(),
            accent = 0xFF5FB0E5.toInt()
        )
    ),
    PlumRose(
        "Plum & Rose",
        PaletteColors(
            background = 0xFF2A2033.toInt(),
            border = 0xFF6E4C7A.toInt(),
            field = 0xFF1B1422.toInt(),
            text = 0xFFF1E7F2.toInt(),
            textDim = 0xFFC3B0C8.toInt(),
            disabledText = 0xFF87718F.toInt(),
            accent = 0xFFE07AA8.toInt()
        )
    ),
    EmberAsh(
        "Ember & Ash",
        PaletteColors(
            background = 0xFF2B2523.toInt(),
            border = 0xFF7A5A4C.toInt(),
            field = 0xFF1C1715.toInt(),
            text = 0xFFF4EAE2.toInt(),
            textDim = 0xFFCBB8AC.toInt(),
            disabledText = 0xFF8C7A6E.toInt(),
            accent = 0xFFE8703A.toInt()
        )
    ),
    MonoGold(
        "Mono & Gold",
        PaletteColors(
            background = 0xFF262626.toInt(),
            border = 0xFF5A5A5A.toInt(),
            field = 0xFF171717.toInt(),
            text = 0xFFEDEDED.toInt(),
            textDim = 0xFFB8B8B8.toInt(),
            disabledText = 0xFF7C7C7C.toInt(),
            accent = 0xFFD4AF37.toInt()
        )
    ),

    /** The colours are the player's own, typed as hex; these stand in for any field left blank. */
    Custom(
        "Custom",
        PaletteColors(
            background = 0xFF1F3330.toInt(),
            border = 0xFF4C7A63.toInt(),
            field = 0xFF13221F.toInt(),
            text = 0xFFF2ECDC.toInt(),
            textDim = 0xFFB4C2AF.toInt(),
            disabledText = 0xFF6E8577.toInt(),
            accent = 0xFFE0A84A.toInt()
        )
    );

    /** The picker shows this, so it reads as a name rather than as the constant. */
    override fun toString(): String = label
}
