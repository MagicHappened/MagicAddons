package org.magic.magicaddons.commands.internal

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils

object MainInternal : AbstractCommand() {
    override val argument: String = "internal"
    override val description: String = "internal commands used by chat hovers"
    val internalCommandList = mutableListOf<AbstractCommand>(
    )

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val mainInternalCommand: LiteralArgumentBuilder<FabricClientCommandSource> =
            LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
                it.source.sendError(ChatUtils.buildWithPrefix("missing 3rd argument (are you trying to run internal commands?)"))
                return@executes 0
            }
        internalCommandList.forEach { command ->
            mainInternalCommand.then(command.build())
        }
        return mainInternalCommand
    }
}