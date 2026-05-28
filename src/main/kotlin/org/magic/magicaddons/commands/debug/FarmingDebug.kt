package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

object FarmingDebug : AbstractCommand() {
    override val argument: String = "farming"
    override val description: String = "returns data for greenhouse testing"
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                GreenhouseData.greenhouseGrids.forEach {
                    ChatUtils.sendWithPrefix("Unscanned ticks for ${it.layout.name ?: it.layout.id} - ${it.state.pendingTicks}")
                }
                return@executes 1
            }
    }
}