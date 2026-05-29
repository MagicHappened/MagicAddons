package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils
import java.time.Instant

object FarmingDebug : AbstractCommand() {
    override val argument: String = "farming"
    override val description: String = "returns data for greenhouse testing"
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                val cropGrowth = GreenhouseData.miscInfo.cropGrowthValue ?: return@executes 0
                val cropSpeed = GreenhouseData.miscInfo.cropSpeedUpgradeValue ?: return@executes 0
                val uniques = GreenhouseData.getCurrentUniques()
                val uniquesString = uniques.joinToString(",")
                ChatUtils.sendWithPrefix("Uniques amount: ${uniques.size}")
                ChatUtils.sendWithPrefix("Uniques: $uniquesString")
                ChatUtils.sendWithPrefix("Growth $cropGrowth, Speed: $cropSpeed")
                val doubleAmount = GreenhouseData.computeGrowthStageTimeSeconds(
                    uniques.size,
                    cropGrowth,
                    cropSpeed
                )
                val msAmount = doubleAmount.toLong() * 1000L
                ChatUtils.sendWithPrefix("Calculated time: MS $msAmount")

                val timestamp = Instant.now().plusMillis(msAmount)
                ChatUtils.sendWithPrefix("Calculated time: TIMESTAMP $timestamp")
                ChatUtils.sendWithPrefix("Calculated seconds before long conversion (as double): $doubleAmount")
                return@executes 1
            }
    }
}