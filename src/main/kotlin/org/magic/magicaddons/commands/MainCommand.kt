package org.magic.magicaddons.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.MainDebug
import org.magic.magicaddons.commands.features.EditFeature
import org.magic.magicaddons.commands.misc.PlaySound
import org.magic.magicaddons.commands.features.ToggleFeature
import org.magic.magicaddons.commands.features.farming.GreenhouseScreenCommand
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.util.ScreenUtil


object MainCommand {
    val commandList = mutableListOf(
        GreenhouseScreenCommand,
        MainInternal,
        ToggleFeature,
        EditFeature,
        MainDebug,
        PlaySound
    )

    init {
        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback { dispatcher, _ ->

                val roots = listOf(
                    literal(Common.MOD_NAME),
                    literal("ma"),
                    literal("MA")
                )

                roots.forEach { root ->
                    root.executes {
                        val config = ConfigScreen(
                            Component.literal("Magic Addons Config"),
                            null
                        )
                        ScreenUtil.setScreen(config)
                        1
                    }

                    commandList.forEach { command ->
                        root.then(command.build())
                    }

                    dispatcher.register(root)
                }
            }
        )
    }
}