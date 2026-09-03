package org.magic.magicaddons


import net.fabricmc.api.ClientModInitializer
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.config.ConfigNotices
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.EntityUtils

class MagicAddons : ClientModInitializer {

    override fun onInitializeClient() {
        EntityUtils
        ScreenUtil.register()
        MainCommand
        // listening before the config loads, since a migration may have something to say
        ConfigNotices


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }

    }
}
