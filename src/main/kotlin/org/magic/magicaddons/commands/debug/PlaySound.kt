package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils

object PlaySound : AbstractCommand() {
    override val argument: String = "playsound"
    override val description: String = "plays the sound from the given sound path"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>("playsound")
        command.executes {
            it.source.sendError(ChatUtils.buildWithPrefix("No sound path provided"))
            return@executes 0
        }
        // public ArgumentCommandNode(final String name, final ArgumentType<T> type, final Command<S> command, final Predicate<S> requirement,
        // final CommandNode<S> redirect, final RedirectModifier<S> modifier, final boolean forks, final SuggestionProvider<S> customSuggestions)
        /* todo finish this command
        command.then(ArgumentCommandNode(
            "soundpath",
            StringArgumentType.string(),
            Command.SINGLE_SUCCESS

        ))
            .then(LiteralArgumentBuilder.literal("playsound"))

        */
        return command
    }
}