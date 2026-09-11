package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData

/** Takes the plan off the greenhouse being stood in, for the Unplan button and a click in chat. */
object UnplanGreenhouse : AbstractCommand() {

    override val argument: String = "unplan"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                GreenhouseData.unplanCurrentGreenhouse()
                return@executes 1
            }
}
