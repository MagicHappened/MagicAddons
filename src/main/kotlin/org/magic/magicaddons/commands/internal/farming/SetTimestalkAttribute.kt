package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.toExactDuration
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

/**
 * set the Timestalk attribute level manually, until sb-api fixes its shard api
 */
object SetTimestalkAttribute : AbstractCommand() {

    const val NAME: String = "setTimestalkAttributeL57"

    override val argument: String = NAME

    private const val MAX_LEVEL: Int = 10

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                explain()
                return@executes 1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, Int>(
                    "level",
                    IntegerArgumentType.integer(0, MAX_LEVEL)
                ).executes {
                    val level = IntegerArgumentType.getInteger(it, "level")

                    GreenhouseData.miscInfo.greenhouseSpeedAttribute = level

                    ChatUtils.sendWithPrefix(
                        Component.literal("Timestalk attribute set to level $level")
                            .withStyle(ChatFormatting.GREEN)
                    )

                    GreenhouseData.currentGrowthTickMs()?.let { tick ->
                        ChatUtils.sendWithPrefix("A growth tick is now ${tick.toExactDuration()}")
                    }

                    return@executes 1
                }
            )
    }

    /** Says what is wanted, then hands the player the command with only the number left to add. */
    private fun explain() {
        ChatUtils.sendWithPrefix(
            Component.literal("Type your Timestalk attribute level below:")
                .withStyle(ChatFormatting.YELLOW)
        )
        Minecraft.getInstance().setScreenAndShow(
            // auto type the internal command, leaving only the level to be typed in
            ChatScreen("${MainInternal.COMMAND} $argument ", false)
        )
    }
}
