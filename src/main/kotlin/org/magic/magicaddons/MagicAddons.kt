package org.magic.magicaddons

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import org.magic.magicaddons.commands.MainCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.PresetStorage
import org.magic.magicaddons.ui.fonts.SystemFonts
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhouseKey
import org.magic.magicaddons.render.CropPreviewRenderer
import org.magic.magicaddons.render.ItemIconRenderer
import org.magic.magicaddons.render.MarkerRenderer
import org.magic.magicaddons.ui.hud.HudEditorKey
import org.magic.magicaddons.ui.hud.HudRenderer
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ServerTime
import org.magic.magicaddons.util.ServerUtils
import org.magic.magicaddons.util.VersionChecker

class MagicAddons : ClientModInitializer {

    @Suppress("UNUSED_EXPRESSION")
    override fun onInitializeClient() {
        EntityUtils
        ServerUtils
        ServerTime
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
        ModFiles.init()
        PresetStorage.loadPresets()
        SystemFonts.init()
        VersionChecker


        if (!MagicAddonsConfigJsonHandler.load()){
            MagicAddonsConfigJsonHandler.save()
        }

    }
}
