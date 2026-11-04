package org.magic.magicaddons.commands.internal

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractInternalCommand
import org.magic.magicaddons.data.server.CoopPlanSync

object FollowCoopPlanCommand : AbstractInternalCommand() {

    override val argument: String = "FollowCoopPlan"

    private const val MAX_PLOT_NUMBER: Int = 999

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, Int>(
                    "plot",
                    IntegerArgumentType.integer(1, MAX_PLOT_NUMBER)
                ).executes {
                    CoopPlanSync.followCoopPlanOn(IntegerArgumentType.getInteger(it, "plot"))
                    return@executes 1
                }
            )
}
