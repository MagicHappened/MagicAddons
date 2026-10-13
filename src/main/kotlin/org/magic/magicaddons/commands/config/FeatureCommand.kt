package org.magic.magicaddons.commands.config

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils

object FeatureCommand : AbstractCommand() {
    override val argument: String = "feature"

    private val subcommands: List<AbstractCommand> = listOf(ToggleFeature, EditFeature)

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            it.source.sendError(ChatUtils.buildWithPrefix("Missing toggle or edit."))
            return@executes 0
        }
        subcommands.forEach { command.then(it.build()) }
        return command
    }
}
