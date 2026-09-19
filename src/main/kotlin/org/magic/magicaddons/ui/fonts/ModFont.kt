package org.magic.magicaddons.ui.fonts

import net.minecraft.network.chat.FontDescription
import org.magic.magicaddons.features.customization.Customization

/**
 * While a block runs inside [applied], every piece of text drawn or measured in the game's default
 * font is drawn or measured in the font the settings pick instead. The mod's screens and HUD run
 * inside it, so their widths and their writing always agree.
 */
object ModFont {

    private var depth: Int = 0

    private var scopeFont: FontDescription? = null

    inline fun <T> applied(block: () -> T): T {
        enter()
        try {
            return block()
        } finally {
            leave()
        }
    }

    /**
     * The font is worked out once a scope rather than once a glyph: working it out reads the
     * settings and the resource pack folder, which is far too much to do per character drawn.
     */
    fun enter() {
        if (depth++ == 0) scopeFont = Customization.fontId?.let { FontDescription.Resource(it) }
    }

    fun leave() {
        if (--depth == 0) scopeFont = null
    }

    /** called from the Font mixin for every glyph lookup */
    @JvmStatic
    fun substitute(description: FontDescription): FontDescription {
        if (depth == 0 || description != FontDescription.DEFAULT) return description

        return scopeFont ?: description
    }
}
