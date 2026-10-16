package org.magic.magicaddons.features.mining

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.SBLocation

object XpOrbHider : Feature() {

    override val id: String = "XpOrbHider"
    override val displayName: String = "XP Orb Hider"
    override val description: String = "Hides experience orbs on the ground."
    override val category: String = "mining"

    private val onlyOnMiningIslandsSetting = BooleanSetting(
        key = "OnlyOnMiningIslands",
        displayName = "Only on mining islands",
        description = "Only hides them in the Dwarven Mines, Crystal Hollows and Mineshafts.",
        value = false
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(onlyOnMiningIslandsSetting)
    )

    fun shouldHideXpOrbs(): Boolean =
        baseSetting.isEnabled && (!onlyOnMiningIslandsSetting.value || SBLocation.MiningIslands.inside())
}
