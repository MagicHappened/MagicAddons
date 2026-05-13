package org.magic.magicaddons


import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.greenhouse.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.handlers.CodecStorage
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.EntityUtils

class MagicAddons : ClientModInitializer {

    override fun onInitializeClient() {
        EntityUtils
        ScreenUtil.register()
        MainCommand
        DataHandler.init()

        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }



    }
}
