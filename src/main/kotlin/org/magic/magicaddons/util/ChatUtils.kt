package org.magic.magicaddons.util


import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.time.Instant

object ChatUtils {
    //todo add warning system instead of send and add cooldown
    var lastWarningtime: Instant? = null

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
        val component = buildWithCommand(message, command)
        Minecraft.getInstance().player?.displayClientMessage(component, false)
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
        return lastWarningtime
            ?.plusSeconds(60)
            ?.isBefore(Instant.now())
            ?: true
    }

    fun sendWarning(message: String) {
        if (cooldownReady()) {
            lastWarningtime = Instant.now()
            sendWithPrefix(message)
        }
    }
    fun buildWarning(message: String): Component? {
        if (cooldownReady()) {
            lastWarningtime = Instant.now()
            return buildWithPrefix(message)
        }
        return null
    }
    fun sendWarnings(messages: List<String>) {
        if (cooldownReady()) {
            lastWarningtime = Instant.now()
            messages.forEach {
                sendWithPrefix(it)
            }
        }
    }
    fun sendWarnings(messages: List<Component>) {
        if (cooldownReady()) {
            lastWarningtime = Instant.now()
            messages.forEach {
                sendWithPrefix(it)
            }
        }
    }

    //todo remember that needs to send 3 warnings at a time.
    fun sendWarningWithCommand(message: String, command: String) {

    }

}