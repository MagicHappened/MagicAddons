package org.magic.magicaddons.commands.misc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.VersionChecker

object VersionCommand : AbstractCommand() {
    override val argument: String = "version"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)

        command.executes {
            ChatUtils.sendWithPrefix("Checking for a newer version…")

            VersionChecker.check { found ->
                val player = Minecraft.getInstance().player ?: return@check

                if (found.outdated) {
                    player.sendSystemMessage(VersionChecker.updateMessage(found))
                } else {
                    ChatUtils.sendWithPrefix("Up to date (${found.current})")
                }
            }

            return@executes 1
        }

        return command
    }
}
