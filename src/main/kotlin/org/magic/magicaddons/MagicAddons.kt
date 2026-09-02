package org.magic.magicaddons


import org.magic.magicaddons.render.CropPreviewRenderer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
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

        // the crop preview draws plants into the gui the way the inventory draws the player, and
        // the pipeline only draws states it was handed a renderer for at startup
        PictureInPictureRendererRegistry.register { CropPreviewRenderer(it.bufferSource()) }
        MainCommand
        DataHandler.init()


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }

    }
}
