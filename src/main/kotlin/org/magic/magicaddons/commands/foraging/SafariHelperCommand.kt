package org.magic.magicaddons.commands.foraging

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.foraging.SafariHelper
import org.magic.magicaddons.features.foraging.SafariZone
import org.magic.magicaddons.util.ChatUtils

object SafariHelperCommand : AbstractCommand() {

    override val argument: String = "SafariHelper"
    override val description: String = "Safari helper utilities"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val remainingMobs = literal<FabricClientCommandSource>("SendRemainingMobs")
            .executes {
                sendRemainingReport(it.source, null)
                return@executes 1
            }

        SafariZone.entries.forEach { zone ->
            remainingMobs.then(
                literal<FabricClientCommandSource>(zone.displayName.lowercase())
                    .executes {
                        sendRemainingReport(it.source, zone)
                        return@executes 1
                    }
            )
        }

        return literal<FabricClientCommandSource>(argument)
            .executes {
                it.source.sendError(ChatUtils.buildWithPrefix("Missing safari helper action."))
                return@executes 1
            }
            .then(remainingMobs)
    }

    /**
     * Reports [zone], or every zone that is not finished yet when it is null.
     *
     * Every biome is sent as its own message so that a mod which copies a message on right click
     * copies exactly one biome, title and remaining mobs together.
     */
    private fun sendRemainingReport(source: FabricClientCommandSource, zone: SafariZone?) {
        val zones = zone?.let { listOf(it) } ?: SafariZone.entries.toList()
        // a finished biome is only worth reporting when it was asked for by name
        val reported = zones.filter { zone != null || SafariHelper.remainingIn(it).isNotEmpty() }

        if (reported.isEmpty()) {
            source.sendFeedback(
                ChatUtils.buildWithPrefix(
                    Component.literal("All safari uniques caught").withStyle(ChatFormatting.GREEN)
                )
            )
            return
        }

        source.sendFeedback(
            ChatUtils.buildWithPrefix(
                Component.literal("Zones remaining:").withStyle(ChatFormatting.GOLD)
            )
        )

        reported.forEach { biome -> source.sendFeedback(biomeReport(biome)) }
    }

    private fun biomeReport(biome: SafariZone): Component {
        val remaining = SafariHelper.remainingIn(biome)

        val report = Component.literal("${biome.displayName} Biome left:").withStyle(ChatFormatting.GOLD)

        if (remaining.isEmpty()) {
            return report.append(Component.literal("\nall caught").withStyle(ChatFormatting.GREEN))
        }

        return report.append(
            Component.literal("\n${remaining.joinToString(", ")}").withStyle(ChatFormatting.GREEN)
        )
    }
}
