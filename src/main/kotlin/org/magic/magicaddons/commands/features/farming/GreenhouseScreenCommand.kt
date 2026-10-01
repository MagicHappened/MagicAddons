package org.magic.magicaddons.commands.features.farming

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.CropWords
import org.magic.magicaddons.commands.misc.LinkCommand
import org.magic.magicaddons.data.server.GreenhouseDataSync
import org.magic.magicaddons.data.server.GreenhouseDataSync.SyncOutcome
import org.magic.magicaddons.data.server.ServerRequirements
import org.magic.magicaddons.ui.screens.CropPreviewScreen
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

object GreenhouseScreenCommand : AbstractCommand() {
    override val argument: String = "GreenhouseScreen"
    override val aliases: List<String> = listOf("gh")

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val preview = LiteralArgumentBuilder.literal<FabricClientCommandSource>("preview")
            .executes {
                ScreenUtil.setScreen(CropPreviewScreen(null))
                1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("crop", StringArgumentType.word())
                    .suggests { _, builder -> CropWords.suggestCrops(builder) }
                    .executes {
                        val word = StringArgumentType.getString(it, "crop")
                        val def = CropWords.find(word)
                        if (def == null) {
                            ChatUtils.sendWithPrefix("No crop called $word.")
                            return@executes 0
                        }
                        ScreenUtil.setScreen(CropPreviewScreen(null, def))
                        1
                    }
            )

        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                ScreenUtil.setScreen(GreenhouseScreen())
                1
            }
            .then(preview)
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("SyncData").executes {
                    syncGreenhouseData()
                    1
                }
            )
    }

    private fun syncGreenhouseData() {
        ServerRequirements.missingRequirementMessage(needsDiscordIntegration = true)?.let {
            ChatUtils.sendWithPrefix(it)
            return
        }

        GreenhouseDataSync.syncByCommand().thenAccept { outcome ->
            Minecraft.getInstance().execute { ChatUtils.sendWithPrefix(messageFor(outcome)) }
        }
    }

    private fun messageFor(outcome: SyncOutcome): Component = when (outcome) {
        SyncOutcome.Sent -> Component.literal("Greenhouse data sent to the MagicAddons server.")
        SyncOutcome.NotLinked -> Component.literal("Not linked to Discord yet, run ")
            .append(
                ChatUtils.buildStyled(
                    LinkCommand.command,
                    ChatFormatting.AQUA,
                    Component.literal("Click to run"),
                    ClickEvent.RunCommand(LinkCommand.command),
                    underlined = true
                )
            )
        SyncOutcome.FeatureOff -> ServerRequirements.missingRequirementMessage(needsDiscordIntegration = true)
            ?: Component.literal("Discord integration is off.")
        SyncOutcome.NoGreenhouseData -> Component.literal("No scanned greenhouse or your tick time is unknown")
        is SyncOutcome.CoolingDown -> Component.literal("You can sync again in ${(outcome.waitMs / 60_000) + 1} minutes.")
        is SyncOutcome.Failed -> Component.literal("The server did not accept the data (${outcome.status ?: "unreachable"}). See the log.")
    }
}
