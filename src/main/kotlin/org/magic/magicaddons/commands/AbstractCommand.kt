package org.magic.magicaddons.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandSource

abstract class AbstractCommand {
    abstract val argument: String
    abstract val description: String
    abstract fun build(): LiteralArgumentBuilder<FabricClientCommandSource>

}