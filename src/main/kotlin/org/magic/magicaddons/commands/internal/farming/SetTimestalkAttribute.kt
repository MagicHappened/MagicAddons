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
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

/**
 * Takes the Timestalk attribute level by hand, since the shard api does not report this one. Run
 * bare, it opens the chat with the command already typed.
 */
object SetTimestalkAttribute : AbstractCommand() {

    override val argument: String = "setTimestalkAttributeL57"
    override val description: String = "Sets your Timestalk attribute level, which decides greenhouse tick speed"

    /** Ten levels, half a percent each, which is the five percent the formula tops out at. */
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
                        val seconds = tick / 1000

                        ChatUtils.sendWithPrefix(
                            "A growth tick is now %dh %02dm %02ds".format(
                                seconds / 3600, seconds % 3600 / 60, seconds % 60
                            )
                        )
                    }

                    return@executes 1
                }
            )
    }

    /** Says what is wanted, then hands the player the command with only the number left to add. */
    private fun explain() {
        ChatUtils.sendWithPrefix(
            Component.literal("Open your Attribute Menu and find Timestalk, then type its level below.")
                .withStyle(ChatFormatting.YELLOW)
        )
        Minecraft.getInstance().setScreenAndShow(
            // not a draft: the text is put in as though the player had typed it, ready to add to
            ChatScreen("/MagicAddons internal $argument ", false)
        )
    }
}
