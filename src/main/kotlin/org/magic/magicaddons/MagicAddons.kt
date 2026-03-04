package org.magic.magicaddons

import net.fabricmc.api.ModInitializer
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles
import org.magic.magicaddons.util.ScreenUtil

class MagicAddons : ModInitializer {


    override fun onInitialize() {

        ScreenUtil.register()
        MainCommand
        HidePowderCoatingParticles
    }
}
