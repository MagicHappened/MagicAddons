package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import org.magic.magicaddons.util.ChatUtils

object TickReport : AbstractCommand() {

    const val NAME: String = "tickReport"

    override val argument: String = NAME

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                PlantWarnings.sendReport(PlantWarnings.activeProfile())
                return@executes 1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                    "profile",
                    StringArgumentType.greedyString()
                ).executes {
                    val name = StringArgumentType.getString(it, "profile")
                    val profile = PlantWarnings.profileNamed(name)

                    if (profile == null) {
                        ChatUtils.sendWithPrefix("No greenhouses recorded for profile $name.")
                        return@executes 0
                    }

                    PlantWarnings.sendReport(profile)
                    return@executes 1
                }
            )
}
