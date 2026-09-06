package org.magic.magicaddons.util


import net.minecraft.network.chat.MutableComponent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.time.Instant

object ChatUtils {
    private const val WARNING_COOLDOWN_SECONDS: Long = 60

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

    fun buildWithPrefix(message: String?): MutableComponent {
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

    fun buildWithPrefix(message: Component?): MutableComponent {
        val prefix = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)

        return if (message != null && message != Component.empty()) prefix.append(message) else prefix
    }
    fun sendWithCommand(message: String, command: String) {
        val component = buildWithCommand(message, command)
        Minecraft.getInstance().player?.sendSystemMessage(component)
    }

    /** The prefixed message, clicking it runs [command]. */
    fun buildWithCommand(message: String, command: String): Component {
        return buildWithPrefix(
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
    }

    fun cooldownReady(): Boolean {
        return lastWarningTime
            ?.plusSeconds(WARNING_COOLDOWN_SECONDS)
            ?.isBefore(Instant.now())
            ?: true
    }

    /** Sends the warnings, at most once a minute. */
    fun sendWarningsComponents(messages: List<Component>) {
        if (cooldownReady()) {
            lastWarningTime = Instant.now()
            messages.forEach {
                send(it)
            }
        }
    }

}