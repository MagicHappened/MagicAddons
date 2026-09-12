package org.magic.magicaddons


import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseKey
import org.magic.magicaddons.ui.hud.HudEditorKey
import org.magic.magicaddons.ui.hud.HudRenderer
import org.magic.magicaddons.config.ConfigNotices
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.render.CropPreviewRenderer
import org.magic.magicaddons.render.MarkerRenderer
import org.magic.magicaddons.render.ItemIconRenderer
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.ServerUtils
import org.magic.magicaddons.util.VersionAnnouncer

class MagicAddons : ClientModInitializer {

    // objects named here so they register; the names are the point, not their values
    @Suppress("UNUSED_EXPRESSION")
    override fun onInitializeClient() {
        EntityUtils
        ServerUtils
        ScreenUtil.register()

        // the gui only draws picture-in-picture states it was handed a renderer for at startup
        //? if >=26.2 {
        /*PictureInPictureRendererRegistry.register { CropPreviewRenderer() }
        PictureInPictureRendererRegistry.register { ItemIconRenderer() }
        *///?} else {
        PictureInPictureRendererRegistry.register { CropPreviewRenderer(it.bufferSource()) }
        PictureInPictureRendererRegistry.register { ItemIconRenderer(it.bufferSource()) }
        //?}
        MainCommand
        GreenhouseKey
        HudEditorKey
        HudRenderer
        MarkerRenderer
        DataHandler.init()
        // listening before the config loads, since a migration may have something to say
        ConfigNotices
        VersionAnnouncer


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }

    }
}
