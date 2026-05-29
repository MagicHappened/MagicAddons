package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils
import java.time.Instant

object FarmingDebug : AbstractCommand() {
    override val argument: String = "farming"
    override val description: String = "returns data for greenhouse testing"
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                GreenhouseData.greenhouseGrids.forEach { grid ->
                    ChatUtils.sendWithPrefix("Pending ticks for ${grid.layout.name ?: grid.layout.id} : ${grid.state.pendingGrowthTicks}")
                }
                return@executes 1
            }
    }
}