package org.magic.magicaddons

import net.fabricmc.api.ModInitializer
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles
import org.magic.magicaddons.util.LocationUtils
import org.magic.magicaddons.util.ScreenUtil

class MagicAddons : ModInitializer {


    override fun onInitialize() {

        LocationUtils.register()
        ScreenUtil.register()
        MainCommand
    }
}
