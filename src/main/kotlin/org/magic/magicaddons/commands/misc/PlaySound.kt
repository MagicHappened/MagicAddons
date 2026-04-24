package org.magic.magicaddons.commands.misc

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils

object PlaySound : AbstractCommand() {
    override val argument: String = "playsound"
    override val description: String = "plays the sound from the given sound path"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
        command.executes {
            it.source.sendError(ChatUtils.buildWithPrefix("No sound path provided"))
            return@executes 0
        }
        command.then(
            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("soundpath", StringArgumentType.word())
                .executes {
                    playSound(it)
                    return@executes 1
                }
                .then(
                    RequiredArgumentBuilder.argument<FabricClientCommandSource, Float>("volume", FloatArgumentType.floatArg())
                        .executes {
                            val volume = FloatArgumentType.getFloat(it,"volume")
                            playSound(it, volume)
                            return@executes 1
                        }
                        .then(
                            RequiredArgumentBuilder.argument<FabricClientCommandSource, Float>("pitch", FloatArgumentType.floatArg())
                                .executes {
                                    val volume = FloatArgumentType.getFloat(it,"volume")
                                    val pitch = FloatArgumentType.getFloat(it,"pitch")
                                    playSound(it, volume, pitch)
                                    return@executes 1
                                }
                        )
                )
        )
        return command
    }

    private fun playSound(ctx: CommandContext<FabricClientCommandSource>, volume: Float = 1.0F, pitch: Float = 1.0F) {
        val soundId = Identifier.parse(StringArgumentType.getString(ctx, "soundpath"))
        val soundEvent = SoundEvent.createVariableRangeEvent(soundId)
        Minecraft.getInstance().player?.playSound(soundEvent, volume, pitch)
        ctx.source.sendFeedback(ChatUtils.buildWithPrefix("Played sound: $soundId"))
    }

}