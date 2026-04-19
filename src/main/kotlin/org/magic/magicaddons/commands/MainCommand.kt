package org.magic.magicaddons.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.text.Text
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.MainDebug
import org.magic.magicaddons.commands.features.EditFeature
import org.magic.magicaddons.commands.misc.PlaySound
import org.magic.magicaddons.commands.features.ToggleFeature
import org.magic.magicaddons.commands.features.farming.GreenhouseScreen
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.util.ScreenUtil


object MainCommand {
    val commandList = mutableListOf(
        GreenhouseScreen,
        ToggleFeature,
        EditFeature,
        MainDebug,
        PlaySound
    )

    init {

        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback { dispatcher, _ ->

                val main = literal(Common.MOD_NAME)
                    .executes {
                        val config = ConfigScreen(Text.literal("Magic Addons Config"), null)
                        ScreenUtil.setScreen(config)
                        return@executes 1
                    }

                commandList.forEach { command ->
                    main.then(command.build())
                }

                dispatcher.register(main)
            }
        )
    }


}