package org.magic.magicaddons


import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.entity.EntityRenderers
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.ServerUtils

class MagicAddons : ClientModInitializer {



    override fun onInitializeClient() {

        EntityUtils
        ServerUtils
        ScreenUtil.register()
        MainCommand
        DataHandler.init()


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }



    }
}
