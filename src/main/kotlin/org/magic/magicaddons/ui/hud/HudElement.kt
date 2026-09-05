package org.magic.magicaddons.ui.hud

import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.features.Feature

/** One line of a hud element: plain text, or a label with its value set against the right edge. */
sealed interface HudLine {
    class Text(val text: Component) : HudLine
    class Pair(val label: Component, val value: Component) : HudLine
}

class HudContent(val lines: List<HudLine>)

/** The setting a hud element belongs to, so the editor can open the config on it. */
class ConfigTarget(val feature: Feature, val path: List<SettingNode<*>>)

/** Something drawn on the hud: what it shows now, what it looks like in the editor, and where it starts out. */
abstract class HudElement(val id: String, val name: String) {

    abstract val defaultX: Int
    abstract val defaultY: Int

    /** How solid the background, frame and dividers start out, from none at zero to full at one. */
    open val defaultAlpha: Float = 1f

    open val shadow: Boolean = false

    /** What to draw right now, or null while there is nothing to show. */
    abstract fun content(): HudContent?

    /** Typical content, so the editor can lay the element out when it has nothing to show. */
    abstract fun sample(): HudContent

    open val configTarget: ConfigTarget? = null

    /** Where this element can show; none listed means anywhere. */
    open val situations: Set<HudSituation> = emptySet()

    fun showsIn(situation: HudSituation): Boolean =
        situation == HudSituation.EVERYTHING || situations.isEmpty() || situation in situations
}

/** Every hud element there is, named here so each registers before the editor asks for them. */
object HudElements {
    val all: List<HudElement>
        get() = listOf(
            org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseHud,
            org.magic.magicaddons.features.foraging.safarihelper.SafariHelper.hud
        )

    fun byId(id: String): HudElement? = all.firstOrNull { it.id == id }
}
