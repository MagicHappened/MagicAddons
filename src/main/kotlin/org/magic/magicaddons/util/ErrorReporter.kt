package org.magic.magicaddons.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.Common
import java.lang.reflect.InvocationTargetException

/**
 * Where an error in this mod ends up: the full trace in the log every time, and a line in chat the
 * first time each distinct error happens, clicked to copy the details for a bug report.
 */
object ErrorReporter {

    /** The errors already said in chat this session, so one broken frame does not flood it. */
    private val seen: MutableSet<String> = HashSet()

    fun report(where: String, error: Throwable, vararg extra: Pair<String, Any?>) {
        val cause = unwrap(error)
        // the game itself is going down; nothing here can help
        if (cause is VirtualMachineError) throw cause

        Common.LOGGER.error("Something went wrong in $where", cause)

        val key = "$where|${cause.javaClass.name}|${cause.stackTrace.firstOrNull()}"
        if (!seen.add(key)) return
        val player = Minecraft.getInstance().player ?: return

        val details = buildString {
            appendLine("MagicAddons ${VersionChecker.currentVersion()}")
            appendLine("Where: $where")
            extra.forEach { (name, value) -> appendLine("$name: $value") }
            appendLine()
            append(cause.stackTraceToString())
        }

        val line = Component.literal("Something went wrong in $where: ${cause.javaClass.simpleName}. Click to copy the details.")
            .withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.RED)
                    .withClickEvent(ClickEvent.CopyToClipboard(details))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Copies the error and where it happened, for a bug report")))
            )
        player.sendSystemMessage(ChatUtils.buildWithPrefix(line))
    }

    /** The error itself, out of the reflection wrapper an event handler throws through. */
    private fun unwrap(error: Throwable): Throwable =
        if (error is InvocationTargetException) error.targetException ?: error else error
}
