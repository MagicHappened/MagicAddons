package org.magic.magicaddons.ui.fonts

import net.minecraft.network.chat.FontDescription
import org.magic.magicaddons.features.customization.Customization

object ModFont {

    private var depth: Int = 0

    private var scopeFont: FontDescription? = null

    inline fun <T> replaceDefaultFont(block: () -> T): T {
        enter()
        try {
            return block()
        } finally {
            leave()
        }
    }

    fun enter() {
        if (depth++ == 0) scopeFont = Customization.fontId?.let { FontDescription.Resource(it) }
    }

    fun leave() {
        if (--depth == 0) scopeFont = null
    }

    @JvmStatic
    fun substitute(description: FontDescription): FontDescription {
        if (depth == 0 || description != FontDescription.DEFAULT) return description

        return scopeFont ?: description
    }
}
