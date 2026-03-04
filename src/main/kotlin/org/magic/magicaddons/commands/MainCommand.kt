package org.magic.magicaddons.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.text.Text

object MainCommand {
    var commandList: List<AbstractCommand> = emptyList()
    init {

        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback { dispatcher, _ ->

                val main = literal("main")
                    .executes {
                        it.source.sendFeedback(Text.literal("Main command"))
                        1
                    }

                commandList.forEach { command ->
                    main.then(command.buildLiteral())
                }

                dispatcher.register(main)
            }
        )
    }


}