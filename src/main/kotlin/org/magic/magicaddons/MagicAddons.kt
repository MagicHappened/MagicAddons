package org.magic.magicaddons


import net.fabricmc.api.ModInitializer
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.WorldEntities

class MagicAddons : ModInitializer {

    override fun onInitialize() {
        WorldEntities
        ScreenUtil.register()
        MainCommand


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }
        else {
            FeatureManager.syncFromConfig()
        }

    }
}
