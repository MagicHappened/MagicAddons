package org.magic.magicaddons

import net.fabricmc.api.ModInitializer
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.WorldEntities

class MagicAddons : ModInitializer {
    val featuresList: MutableList<Feature> = mutableListOf(
        HidePowderCoatingParticles,
        HighlightMobs
    )

    override fun onInitialize() {
        WorldEntities
        ScreenUtil.register()
        MainCommand
    }
}
