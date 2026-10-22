package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ItemStackJson

object ExportHeldItem {

    private const val ARGUMENT: String = "heldItem"

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(ARGUMENT).executes { copyHeldItem() }

    private fun copyHeldItem(): Int {
        val stack = Minecraft.getInstance().player?.mainHandItem?.takeUnless { it.isEmpty }
        if (stack == null) {
            ChatUtils.sendWithPrefix("Your hand is empty.")
            return 0
        }

        val json = ItemStackJson.of(stack)
        if (json == null) {
            ChatUtils.sendWithPrefix("Not connected to a server.")
            return 0
        }

        Minecraft.getInstance().keyboardHandler.clipboard = ItemStackJson.text(json)
        ChatUtils.sendWithPrefix("Copied ${stack.hoverName.string} to your clipboard.")
        return 1
    }
}
