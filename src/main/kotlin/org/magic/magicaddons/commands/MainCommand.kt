package org.magic.magicaddons.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.ConfigScreen
import org.magic.magicaddons.util.ScreenUtil

object MainCommand {
    val commandList = mutableListOf<AbstractCommand>()

    fun registerCommand(command: AbstractCommand) {
        commandList += command
    }
    init {

        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback { dispatcher, _ ->

                val main = literal(Common.MOD_NAME)
                    .executes {
                        val configScreen = ConfigScreen()
                        ScreenUtil.setScreen(configScreen)
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