package org.magic.magicaddons.util


import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.time.Instant

object ChatUtils {
    var lastWarningTime: Instant? = null

    fun send(message: String){
        send(Component.literal(message))
    }
    fun send(message: Component){
        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    fun sendWithPrefix(message: String) {
        sendWithPrefix(Component.literal(message).withStyle(ChatFormatting.WHITE))
    }

    fun buildWithPrefix(message: String?): Component {
        val body = message?.takeIf { it.isNotBlank() } ?: return Component.literal("")

        return buildWithPrefix(Component.literal(body).withStyle(ChatFormatting.WHITE))
    }

    fun sendWithPrefix(message: Component) {
        val prefixed = buildWithPrefix(message)
        Minecraft.getInstance().player?.sendSystemMessage(prefixed)
    }

    /** Runs a command as if the player typed it, [command] is given without the leading slash. */
    fun sendCommand(command: String) {
        Minecraft.getInstance().player?.connection?.sendCommand(command)
    }

    fun buildWithPrefix(message: Component?): Component {
        val prefix = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)

        return if (message != null && message != Component.empty()) prefix.append(message) else prefix
    }
    fun sendWithCommand(message: String, command: String) {
        val component = buildWithCommand(message, command)
        Minecraft.getInstance().player?.sendSystemMessage(component)
    }

    fun buildWithCommand(message: String, command: String): Component {
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
        return component
    }

    fun cooldownReady(): Boolean {
        return lastWarningTime
            ?.plusSeconds(60)
            ?.isBefore(Instant.now())
            ?: true
    }

    fun sendWarning(message: String) {
        if (cooldownReady()) {
            lastWarningTime = Instant.now()
            sendWithPrefix(message)
        }
    }
    fun buildWarning(message: String): Component? {
        if (cooldownReady()) {
            lastWarningTime = Instant.now()
            return buildWithPrefix(message)
        }
        return null
    }
    fun sendWarnings(messages: List<String>) {
        sendWarningsComponents(messages.map { Component.literal(it) })
    }
    fun sendWarningsComponents(messages: List<Component>) {
        if (cooldownReady()) {
            lastWarningTime = Instant.now()
            messages.forEach {
                send(it)
            }
        }
    }

    fun sendWarningWithCommand(message: String, command: String) {
        if (cooldownReady()) {
            lastWarningTime = Instant.now()
            sendWithCommand(message,command)
        }
    }

}