package org.magic.magicaddons.util


import net.minecraft.network.chat.MutableComponent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.util.compat.McCompat
import org.magic.mixins.ChatComponentAccessor
import java.time.Instant

object ChatUtils {
    private const val WARNING_COOLDOWN_SECONDS: Long = 60

    private const val COPY_HINT: String = "Click to copy"

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

    /** sends the prefixed line and returns it, for [retract] */
    fun sendWithPrefix(message: Component): Component {
        val prefixed = buildWithPrefix(message)
        Minecraft.getInstance().player?.sendSystemMessage(prefixed)
        return prefixed
    }

    /** removes the newest copy of a line this mod sent from chat */
    fun retract(message: Component) {
        val chat = McCompat.chat() as ChatComponentAccessor
        val messages = chat.`magicaddons$allMessages`()
        val wanted = message.string

        val newest = messages.filter { it.content().string == wanted }.maxByOrNull { it.addedTime() } ?: return
        messages.remove(newest)
        chat.`magicaddons$refreshTrimmedMessages`()
    }

    fun sendCommand(command: String) {
        Minecraft.getInstance().player?.connection?.sendCommand(command)
    }

    /** a length of time as the mod writes it in chat: "2m 18s", or "45s" under a minute */
    fun shortDuration(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)

        return if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
    }

    fun buildWithPrefix(message: Component?): MutableComponent {
        val prefix = Component.literal("[MA] ").withColor(Customization.prefixColour)

        return if (message != null && message != Component.empty()) prefix.append(message) else prefix
    }
    fun sendWithCommand(message: String, command: String) {
        val component = buildWithCommand(message, command)
        Minecraft.getInstance().player?.sendSystemMessage(component)
    }

    fun buildWithCommand(message: String, command: String): Component =
        buildWithPrefix(
            buildStyled(
                message,
                ChatFormatting.WHITE,
                Component.literal("Running: $command"),
                ClickEvent.RunCommand(command),
            )
        )

    fun buildStyled(
        text: String,
        color: ChatFormatting? = null,
        hover: Component? = null,
        click: ClickEvent? = null,
        underlined: Boolean = false,
    ): MutableComponent {
        var style = Style.EMPTY
        color?.let { style = style.withColor(it) }
        hover?.let { style = style.withHoverEvent(HoverEvent.ShowText(it)) }
        click?.let { style = style.withClickEvent(it) }
        if (underlined) style = style.withUnderlined(true)

        return Component.literal(text).setStyle(style)
    }

    fun buildWithHover(message: String, hover: Component): Component =
        buildWithPrefix(buildStyled(message, hover = hover))

    fun sendWithCopyableHover(message: String, copied: String, hover: String = copied) {
        send(
            buildWithPrefix(
                buildStyled(
                    message,
                    hover = Component.literal("$hover\n$COPY_HINT"),
                    click = ClickEvent.CopyToClipboard(copied),
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