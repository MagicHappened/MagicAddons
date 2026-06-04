package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.util.ChatUtils

object FarmingDebug : AbstractCommand() {
    var footprint: Footprint = Footprint(1,1)

    override val argument: String = "farming"
    override val description: String = "returns data for greenhouse testing"
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                GreenhousePresets.generatePrototype()
                return@executes 1
            }.then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("footprint", StringArgumentType.word())
                    .executes {
                        val stringArg = StringArgumentType.getString(it, "footprint")
                        try {
                            footprint = Footprint(stringArg[0].digitToInt(),stringArg[1].digitToInt())
                        } catch (e: Exception){
                            ChatUtils.sendWithPrefix("doofas")
                            return@executes 0
                        }
                        ChatUtils.sendWithPrefix("changed footprint")
                        return@executes 1
                    }
            )
    }
}