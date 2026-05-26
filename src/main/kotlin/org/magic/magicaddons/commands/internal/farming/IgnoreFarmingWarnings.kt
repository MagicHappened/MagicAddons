package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

object IgnoreFarmingWarnings : AbstractCommand() {
    override val argument: String = "ignoreFarmingWarnings"
    override val description: String = "Ignores farming warnings"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            it.source.sendFeedback(ChatUtils.buildWithPrefix(
                "Got it, warning messages from farming will now be ignored."))
            GreenhouseData.miscInfo.shouldIgnoreWarning = true
            return@executes 1
        }
    }
}