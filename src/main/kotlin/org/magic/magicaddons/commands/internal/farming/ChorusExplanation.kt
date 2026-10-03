package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings

object ChorusExplanation : AbstractCommand() {

    const val NAME: String = "chorusExplanation"

    override val argument: String = NAME

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            PlantWarnings.sendChorusExplanation()
            return@executes 1
        }
}
