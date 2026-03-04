package org.magic.magicaddons.commands

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.util.ScreenUtil

object MagicAddonsCommand {

    val commandList = mutableListOf<AbstractCommand>()

    fun registerCommand(command: AbstractCommand) {
        commandList += command
    }

    fun initCommands() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            val root: LiteralArgumentBuilder<FabricClientCommandSource> =
                ClientCommandManager.literal("MagicAddons")
                    .executes {
                        ScreenUtil.setScreen(ConfigScreen(Text.literal("MagicAddonsConfigScreen")))
                        return@executes SINGLE_SUCCESS
                    }

            // Attach subcommands
            commandList.forEach { command ->
                root.then(command.build())
            }

            dispatcher.register(root)
        }
    }
}