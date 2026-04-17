package org.magic.magicaddons


import net.fabricmc.api.ModInitializer
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.EntityUtils

class MagicAddons : ModInitializer {

    override fun onInitialize() {
        DataHandler.init()
        EntityUtils
        ScreenUtil.register()
        MainCommand


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }

    }
}
