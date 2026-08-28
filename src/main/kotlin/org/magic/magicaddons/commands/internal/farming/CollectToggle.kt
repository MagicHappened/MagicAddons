package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.debug.CropCollector

/**
 * Flips one collector entry between confirmed and not.
 *
 * Exists to be clicked rather than typed: every line the collector lists carries this command with
 * its own id, so confirming a plant is pointing at it instead of copying numbers around.
 */
object CollectToggle : AbstractCommand() {

    override val argument: String = "collectToggle"
    override val description: String = "Confirms or unconfirms a plant the crop collector listed"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, Int>(
                    "id",
                    IntegerArgumentType.integer(0)
                ).executes {
                    CropCollector.toggle(IntegerArgumentType.getInteger(it, "id"))
                    return@executes 1
                }
            )
}
