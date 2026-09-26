package org.magic.magicaddons.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import java.lang.reflect.InvocationTargetException
import java.time.Duration
import java.time.Instant

/**
 * Where an error in this mod ends up: the full trace in the log and a line in chat the first time
 * each distinct error happens, clicked to copy the details for a bug report. One that keeps
 * happening is counted and written out once a minute.
 */
object ErrorReporter {

    /** How often an error already reported is worth another line in the log. */
    private val REPEAT_INTERVAL: Duration = Duration.ofMinutes(1)

    /** When an error was last written out, and how often it has happened since. */
    private class Reported(var lastLogged: Instant, var sinceLogged: Int)

    /** The errors already reported this session, so a broken frame does not flood chat or the log. */
    private val seen: MutableMap<String, Reported> = HashMap()

    fun report(errorLocation: String, error: Throwable, vararg extra: Pair<String, Any?>) {
        val cause = unwrap(error)
        // the game itself is going down; nothing here can help
        if (cause is VirtualMachineError) throw cause

        val key = "$errorLocation|${cause.javaClass.name}|${cause.stackTrace.firstOrNull()}"
        val already = seen[key]

        // an error thrown every frame wrote its whole trace every frame, which is a log nobody can read
        if (already != null) {
            already.sinceLogged++

            val now = Instant.now()
            if (now.isAfter(already.lastLogged.plus(REPEAT_INTERVAL))) {
                Common.LOGGER.error("Something went wrong in $errorLocation ${already.sinceLogged} more times")
                already.lastLogged = now
                already.sinceLogged = 0
            }
            return
        }

        seen[key] = Reported(Instant.now(), 0)
        Common.LOGGER.error("Something went wrong in $errorLocation", cause)

        val player = Minecraft.getInstance().player ?: return

        val details = buildString {
            appendLine("MagicAddons ${VersionChecker.currentVersion()}")
            appendLine("Where: $errorLocation")
            extra.forEach { (name, value) -> appendLine("$name: $value") }
            appendLine()
            append(cause.stackTraceToString())
        }

        val line = ChatUtils.buildStyled(
            "Something went wrong in $errorLocation: ${cause.javaClass.simpleName}. Click to copy the details.",
            ChatFormatting.RED,
            Component.literal("Copies the error and where it happened, for a bug report"),
            ClickEvent.CopyToClipboard(details),
        )
        player.sendSystemMessage(ChatUtils.buildWithPrefix(line))
    }

    /** The error itself, out of the reflection wrapper an event handler throws through. */
    private fun unwrap(error: Throwable): Throwable =
        if (error is InvocationTargetException) error.targetException ?: error else error
}
