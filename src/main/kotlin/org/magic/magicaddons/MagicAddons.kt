package org.magic.magicaddons

import net.fabricmc.api.ModInitializer
import net.minecraft.client.MinecraftClient
import org.magic.magicaddons.commands.MagicAddonsCommand
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles
import org.magic.magicaddons.render.RenderManager
import org.magic.magicaddons.util.LocationUtils
import org.magic.magicaddons.util.ScreenUtil

class MagicAddons : ModInitializer {


    override fun onInitialize() {

        MagicAddonsCommand.initCommands()
        LocationUtils.register()
        ScreenUtil.register()
        HidePowderCoatingParticles

    }
}
