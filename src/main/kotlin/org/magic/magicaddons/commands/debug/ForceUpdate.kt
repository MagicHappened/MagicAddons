package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

object ForceUpdate : AbstractCommand() {
    override val argument: String = "ForceUpdate"
    override val description: String = "marks greenhouses as needing a rescan"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            var count = 0
            GreenhouseData.greenhouseGrids.forEach{
                it.state.needsUpdate = true
                count++
            }
            ChatUtils.sendWithPrefix("Marked $count Greenhouses for rescan")
            return@executes 1
        }
    }
}