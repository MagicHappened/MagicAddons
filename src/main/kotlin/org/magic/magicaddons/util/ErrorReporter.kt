package org.magic.magicaddons.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import java.lang.reflect.InvocationTargetException
import java.time.Duration
import java.time.Instant

object ErrorReporter {

    private val REPEAT_LOG_INTERVAL: Duration = Duration.ofMinutes(1)

    private class ReportedError(var lastLoggedAt: Instant, var timesSinceLogged: Int, var isShownInChat: Boolean)

    private val reportedErrors: MutableMap<String, ReportedError> = HashMap()

    fun report(errorLocation: String, error: Throwable) {
        val rootError = unwrapReflectionError(error)
        if (rootError is VirtualMachineError) throw rootError

        val errorKey = "$errorLocation|${rootError.javaClass.name}|${rootError.stackTrace.firstOrNull()}"
        val previousReport = reportedErrors[errorKey]

        val report = if (previousReport == null) {
            Common.LOGGER.error("Something went wrong in $errorLocation", rootError)
            ReportedError(Instant.now(), 0, isShownInChat = false).also { reportedErrors[errorKey] = it }
        } else {
            previousReport.timesSinceLogged++

            val now = Instant.now()
            if (now.isAfter(previousReport.lastLoggedAt.plus(REPEAT_LOG_INTERVAL))) {
                Common.LOGGER.error("Something went wrong in $errorLocation ${previousReport.timesSinceLogged} more times")
                previousReport.lastLoggedAt = now
                previousReport.timesSinceLogged = 0
            }
            previousReport
        }

        if (!report.isShownInChat) report.isShownInChat = sendChatLine(errorLocation, rootError)
    }

    private fun sendChatLine(errorLocation: String, rootError: Throwable): Boolean {
        val player = Minecraft.getInstance().player ?: return false

        val details = buildString {
            appendLine("MagicAddons ${VersionChecker.currentVersion()}")
            appendLine("Where: $errorLocation")
            appendLine()
            append(rootError.stackTraceToString())
        }

        val chatLine = ChatUtils.buildStyled(
            "Something went wrong in $errorLocation: ${rootError.javaClass.simpleName}. Click to copy the details.",
            ChatFormatting.RED,
            Component.literal("Copies the error and where it happened, for a bug report"),
            ClickEvent.CopyToClipboard(details),
        )
        player.sendSystemMessage(ChatUtils.buildWithPrefix(chatLine))
        return true
    }

    private fun unwrapReflectionError(error: Throwable): Throwable =
        if (error is InvocationTargetException) error.targetException ?: error else error
}
