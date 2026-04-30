package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils

object MainDebug : AbstractCommand() {
    override val argument: String = "debug"
    override val description: String = "base debug command"
    val debugCommandList = mutableListOf<AbstractCommand>(
        DebugFarming
    )

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val mainDebugCommand: LiteralArgumentBuilder<FabricClientCommandSource> =
            LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
                it.source.sendError(ChatUtils.buildWithPrefix("missing debug feature argument"))
                return@executes 0
            }
        debugCommandList.forEach { command ->
            mainDebugCommand.then(command.build())
        }
        return mainDebugCommand
    }
}