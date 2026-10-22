package org.magic.magicaddons.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.config.FeatureCommand
import org.magic.magicaddons.commands.config.HudCommand
import org.magic.magicaddons.commands.debug.AuthDebug
import org.magic.magicaddons.commands.debug.ExportCommand
import org.magic.magicaddons.commands.debug.FarmingDebug
import org.magic.magicaddons.commands.debug.MiningDebug
import org.magic.magicaddons.commands.debug.PlaySound
import org.magic.magicaddons.commands.farming.FortuneCommand
import org.magic.magicaddons.commands.farming.GreenhouseScreenCommand
import org.magic.magicaddons.commands.foraging.SafariHelperCommand
import org.magic.magicaddons.commands.internal.ChorusExplanation
import org.magic.magicaddons.commands.internal.GetPlannerItemCommand
import org.magic.magicaddons.commands.internal.TickReport
import org.magic.magicaddons.commands.misc.VersionCommand
import org.magic.magicaddons.commands.server.LinkCommand
import org.magic.magicaddons.commands.server.PrivacyCommand
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil


object MainCommand {
    const val INTERNAL_ARGUMENT: String = "internal"

    private const val DEBUG_ARGUMENT: String = "debug"

    private val ROOTS: List<String> = listOf(Common.MOD_NAME, "ma", "MA")

    val commandList: List<AbstractCommand> = listOf(
        GreenhouseScreenCommand,
        FortuneCommand,
        SafariHelperCommand,
        FeatureCommand,
        HudCommand,
        LinkCommand,
        PrivacyCommand,
        VersionCommand,
        FarmingDebug,
        MiningDebug,
        AuthDebug,
        PlaySound,
        ExportCommand,
        TickReport,
        ChorusExplanation,
        GetPlannerItemCommand
    )

    private val internalDispatcher: CommandDispatcher<FabricClientCommandSource> by lazy {
        CommandDispatcher<FabricClientCommandSource>().apply {
            ROOTS.forEach { root ->
                val internal = LiteralArgumentBuilder.literal<FabricClientCommandSource>(INTERNAL_ARGUMENT)
                commandList.filterIsInstance<AbstractInternalCommand>().forEach { internal.then(it.build()) }
                register(LiteralArgumentBuilder.literal<FabricClientCommandSource>(root).then(internal))
            }
        }
    }

    fun runClickedInternalCommand(command: String): Boolean {
        val typed = command.removePrefix("/")
        val words = typed.split(' ')
        if (words.size < 2 || words[0] !in ROOTS || words[1] != INTERNAL_ARGUMENT) return false

        val source = Minecraft.getInstance().connection?.suggestionsProvider as? FabricClientCommandSource ?: return true
        try {
            internalDispatcher.execute(typed, source)
        } catch (failure: CommandSyntaxException) {
            source.sendError(Component.literal(failure.message ?: typed))
        }
        return true
    }

    init {
        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback { dispatcher, _ ->

                ROOTS.map { literal(it) }.forEach { root ->
                    root.executes {
                        val config = ConfigScreen(null)
                        ScreenUtil.setScreen(config)
                        1
                    }

                    val debug = literal(DEBUG_ARGUMENT).executes {
                        it.source.sendError(ChatUtils.buildWithPrefix("missing debug feature argument"))
                        return@executes 0
                    }

                    commandList.filterNot { it is AbstractInternalCommand }.forEach { command ->
                        val parent = if (command is AbstractDebugCommand) debug else root
                        val node = command.build().build()
                        parent.then(node)
                        command.aliases.forEach { alias ->
                            parent.then(literal(alias).executes(node.command).redirect(node))
                        }
                    }

                    root.then(debug)
                    dispatcher.register(root)
                }
            }
        )
    }
}
