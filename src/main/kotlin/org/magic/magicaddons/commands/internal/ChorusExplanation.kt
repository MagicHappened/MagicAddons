package org.magic.magicaddons.commands.internal

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractInternalCommand
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings

object ChorusExplanation : AbstractInternalCommand() {


    override val argument: String = "chorusExplanation"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            PlantWarnings.sendChorusExplanation()
            return@executes 1
        }
}
