package org.magic.magicaddons.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.Common

abstract class AbstractCommand {
    abstract val argument: String
    abstract fun build(): LiteralArgumentBuilder<FabricClientCommandSource>

    open val aliases: List<String> = emptyList()

}

abstract class AbstractInternalCommand : AbstractCommand() {
    val command: String get() = "/${Common.MOD_NAME} ${MainCommand.INTERNAL_ARGUMENT} $argument"
}

abstract class AbstractDebugCommand : AbstractCommand()
