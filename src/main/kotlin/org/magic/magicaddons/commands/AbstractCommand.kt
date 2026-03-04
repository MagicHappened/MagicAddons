package org.magic.magicaddons.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

abstract class AbstractCommand {
    abstract val argument: String
    abstract val description: String
    abstract fun execute()
    abstract fun build(): LiteralArgumentBuilder<FabricClientCommandSource>
    init {
        MainCommand.registerCommand(this)
    }

}