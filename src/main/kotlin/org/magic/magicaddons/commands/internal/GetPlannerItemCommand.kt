package org.magic.magicaddons.commands.internal

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractInternalCommand
import org.magic.magicaddons.features.farming.greenhousePresets.PlannerNeeds

object GetPlannerItemCommand : AbstractInternalCommand() {


    override val argument: String = "GetPlannerItem"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                    "command",
                    StringArgumentType.greedyString()
                ).executes {
                    PlannerNeeds.onRequestClicked(StringArgumentType.getString(it, "command"))
                    return@executes 1
                }
            )
}
