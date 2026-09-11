package org.magic.magicaddons.commands.internal

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.internal.farming.CollectToggle
import org.magic.magicaddons.commands.internal.farming.GetPlannerItemCommand
import org.magic.magicaddons.commands.internal.farming.IgnoreFarmingWarnings
import org.magic.magicaddons.commands.internal.farming.UnplanGreenhouse
import org.magic.magicaddons.commands.internal.farming.SetTimestalkAttribute
import org.magic.magicaddons.util.ChatUtils

object MainInternal : AbstractCommand() {
    override val argument: String = "internal"

    /** The starting path of every internal command */
    const val PATH: String = "${Common.MOD_NAME} internal"

    const val COMMAND: String = "/$PATH"

    val internalCommandList = listOf(
        CollectToggle,
        IgnoreFarmingWarnings,
        UnplanGreenhouse,
        SetTimestalkAttribute,
        GetPlannerItemCommand
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