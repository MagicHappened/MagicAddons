package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.debug.MobHitDebugInfo
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils

object DebugHighlightMobs : AbstractCommand() {
    override val argument: String = "PrintDebugHighlighted"
    override val description: String = "Prints information about mobs being highlighted by MobHighlight Feature"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                runCommand()
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, Float>(
                    "range",
                    FloatArgumentType.floatArg()
                ).executes {
                    val range = FloatArgumentType.getFloat(it, "range")
                    runCommand(range)
                }
            )
    }

    private fun runCommand(range: Float? = null): Int {
        HighlightMobs.highlightedEntityList ?: return 0
        val entityList = HighlightMobs.highlightedEntityList!!
        entityList.forEach { entity ->
            val entityPos = Vec3d(entity.x, entity.y, entity.z)
            val distance = entityPos.distanceTo(MinecraftClient.getInstance().player?.entityPos)
            if (!(distance <= (range ?: 999F))) return@forEach

            ChatUtils.sendWithPrefix("Type: ${entity.type}")
            if (entity !is LivingEntity) return@forEach

            MobHitDebugInfo.attackMobDebug(entity)
            ChatUtils.sendWithPrefix("------------------------")


        }

        return 1
    }
}