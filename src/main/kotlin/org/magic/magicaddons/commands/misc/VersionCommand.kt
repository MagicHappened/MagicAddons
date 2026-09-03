package org.magic.magicaddons.commands.misc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.VersionChecker

/** Asks GitHub whether a newer build exists, and answers either way. */
object VersionCommand : AbstractCommand() {
    override val argument: String = "version"
    override val description: String = "checks whether a newer version of the mod is available"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)

        command.executes {
            ChatUtils.sendWithPrefix("Checking for a newer version…")

            VersionChecker.check { found ->
                val player = Minecraft.getInstance().player ?: return@check

                if (found.outdated) {
                    player.sendSystemMessage(VersionChecker.message(found))
                } else {
                    ChatUtils.sendWithPrefix("Up to date (${found.current})")
                }
            }

            return@executes 1
        }

        return command
    }
}
