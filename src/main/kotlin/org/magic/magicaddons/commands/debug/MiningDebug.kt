package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.mining.PickaxeAbilityCooldown
import org.magic.magicaddons.util.ChatUtils

object MiningDebug : AbstractCommand() {
    override val argument: String = "mining"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("cooldownInfo").executes {
                    val held = Minecraft.getInstance().player?.mainHandItem ?: return@executes 0
                    ChatUtils.sendWithPrefix("Pickaxe ability cooldown:")
                    PickaxeAbilityCooldown.debugLines(held).forEach { ChatUtils.sendWithPrefix(it) }
                    return@executes 1
                }
            )
}
