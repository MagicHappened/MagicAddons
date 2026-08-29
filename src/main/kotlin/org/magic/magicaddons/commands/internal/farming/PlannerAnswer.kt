package org.magic.magicaddons.commands.internal.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

/**
 * The two answers to being told a plan is finished.
 *
 * Written as commands because the question is asked in chat and answered by clicking a word in it.
 * Turning the planner off takes the plan away from the greenhouse; leaving it on only stops the
 * question being asked again, since a plan still worth looking at is not a plan worth nagging over.
 */
object UnplanGreenhouse : AbstractCommand() {

    override val argument: String = "unplan"
    override val description: String = "Takes the plan off the greenhouse being stood in"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                GreenhouseData.unplanCurrentGreenhouse()
                return@executes 1
            }
}

object KeepPlanner : AbstractCommand() {

    override val argument: String = "keepPlanner"
    override val description: String = "Leaves the plan on, and stops asking about it"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                val grid = GreenhouseData.getCurrentGrid() ?: run {
                    ChatUtils.sendWithPrefix("Not standing in a greenhouse.")
                    return@executes 0
                }

                grid.state.completionMuted = true

                ChatUtils.sendWithPrefix(
                    Component.literal("Planner left on, and will not ask again about this one.")
                        .withStyle(ChatFormatting.GRAY)
                )

                return@executes 1
            }
}
