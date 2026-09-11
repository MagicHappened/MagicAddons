package org.magic.magicaddons.ui

/**
 * The colours the mod's panels are drawn in.
 */
class PaletteColors(
    val background: Int,
    val border: Int,
    val field: Int,
    val text: Int,
    val textDim: Int,
    val disabledText: Int,
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
    // the entry keeps its old name so a saved config still finds it
    EmberAsh(
        "Crimson & Ash",
        PaletteColors(
            background = 0xFF2A1B1D.toInt(),
            border = 0xFF8A3A40.toInt(),
            field = 0xFF1A1012.toInt(),
            text = 0xFFF6E8E8.toInt(),
            textDim = 0xFFD2B4B8.toInt(),
            disabledText = 0xFF8E6A6E.toInt(),
            accent = 0xFFE03A3A.toInt()
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
