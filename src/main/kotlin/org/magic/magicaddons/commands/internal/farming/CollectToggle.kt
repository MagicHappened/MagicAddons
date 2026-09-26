package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.debug.CropCollector

/** for the collector chat lines, hopefully will not need after the data is collected. */
object CollectToggle : AbstractCommand() {

    const val NAME: String = "collectToggle"

    override val argument: String = NAME

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, Int>(
                    "id",
                    IntegerArgumentType.integer(0)
                ).executes {
                    CropCollector.toggleEntry(IntegerArgumentType.getInteger(it, "id"))
                    return@executes 1
                }
            )
}
