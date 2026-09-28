package org.magic.magicaddons.ui.hud

import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.features.Feature

sealed interface HudLine {
    class Text(val text: Component) : HudLine
    class LabelValue(val label: Component, val value: Component) : HudLine
}

class HudContent(val lines: List<HudLine>)

class ConfigTarget(val feature: Feature, val path: List<SettingNode<*>>)

abstract class HudElement(val id: String, val name: String) {

    abstract val defaultX: Int
    abstract val defaultY: Int

    open val defaultAlpha: Float = 1f

    open val hasTextShadow: Boolean = false

    abstract fun currentContent(): HudContent?

    abstract fun sampleContent(): HudContent

    open val configTarget: ConfigTarget? = null

    open val situations: Set<HudSituation> = setOf(HudSituation.EVERYTHING)

    fun showsIn(situation: HudSituation): Boolean =
        situation == HudSituation.EVERYTHING || HudSituation.EVERYTHING in situations || situation in situations
}

object HudElements {
    val all: List<HudElement>
        get() = listOf(
            org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseHud,
            org.magic.magicaddons.features.foraging.safarihelper.SafariHelper.hud,
            org.magic.magicaddons.features.mining.PickaxeAbilityCooldown.hud
        ).filter { it.configTarget?.feature?.isAvailable != false }

    fun elementById(id: String): HudElement? = all.firstOrNull { it.id == id }
}
