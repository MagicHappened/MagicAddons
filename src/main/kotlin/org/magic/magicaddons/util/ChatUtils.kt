package org.magic.magicaddons.util


import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object ChatUtils {
    //todo add warning system instead of send and add cooldown

    fun sendWithPrefix(message: String) {
        val prefixed = buildWithPrefix(message)
        Minecraft.getInstance().player?.displayClientMessage(prefixed, false)
    }

    fun buildWithPrefix(message: String): Component {
        return buildWithPrefix(Component.literal(message).withStyle(ChatFormatting.WHITE))
    }

    fun sendWithPrefix(message: Component) {
        val prefixed = buildWithPrefix(message)
        Minecraft.getInstance().player?.displayClientMessage(prefixed, false)
    }

    fun buildWithPrefix(message: Component): Component {
        val component = Component.literal("[MA] ")
            .withStyle(ChatFormatting.GOLD)
            .append(message)
        return component
    }
    fun sendWithCommand(message: String, command: String) {
        val component = Component.literal("[MA] ")
            .withStyle(ChatFormatting.GOLD)
            .append(
                Component.literal(message)
                    .withStyle(
                        Style.EMPTY
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(
                                ClickEvent.RunCommand(command)
                            )
                            .withHoverEvent(
                                HoverEvent.ShowText(
                                    Component.literal("Running: $command")
                                )
                            )
                    )
            )

        Minecraft.getInstance().player?.displayClientMessage(component, false)
    }

}