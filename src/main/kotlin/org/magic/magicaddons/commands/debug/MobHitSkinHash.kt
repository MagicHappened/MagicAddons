package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils

object MobHitSkinHash : AbstractCommand() {
    override val argument: String = "MobHitSkinHash"
    override val description: String = "returns the skin hash for the next mob hit"
    var debugOnNextMobHit = false

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return literal<FabricClientCommandSource>(argument)
            .executes {
                debugOnNextMobHit = true
                ChatUtils.sendWithPrefix("Next Mob hit will return the skin hash!")
                return@executes 1
            }

    }
}