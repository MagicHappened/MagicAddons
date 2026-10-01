package org.magic.magicaddons.ui.widgets

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.data.server.ServerSession.ConnectionStatus
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLinesAtCursor
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.splitMod

object ServerCableIcon {

    const val SIZE: Int = 9

    private const val TEXTURE_SIZE: Int = 96

    private const val TOOLTIP_WIDTH: Int = 220

    private const val SECONDS_PER_MINUTE: Long = 60

    private val TEXTURE: Identifier = Identifier.fromNamespaceAndPath("magicaddons", "textures/ui/server_cable.png")

    private var isHovered: Boolean = false

    fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        if (inRect(mouseX, mouseY, x, y, SIZE, SIZE)) isHovered = true

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            x, y,
            0f, 0f,
            SIZE, SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE,
            colorOf(ServerSession.status)
        )
    }

    fun drawTooltipIfHovered(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, isMouseInsidePanel: Boolean) {
        val wasHovered = isHovered
        isHovered = false
        if (!wasHovered || !isMouseInsidePanel) return

        val font = Minecraft.getInstance().font
        val lines = (listOf(Component.literal("Requires authentication to the MagicAddons server")) + statusLinesOf(ServerSession.status))
            .flatMap { font.splitMod(it, TOOLTIP_WIDTH) }

        graphics.drawTooltipLinesAtCursor(lines, mouseX, mouseY)
    }

    private fun colorOf(status: ConnectionStatus): Int = when (status) {
        ConnectionStatus.Authenticated -> Common.UI.TEXT_DIM_COLOR
        ConnectionStatus.Authenticating -> Common.UI.PENDING_COLOR
        else -> Common.UI.DANGER_COLOR
    }

    fun statusLinesOf(status: ConnectionStatus): List<Component> = when (status) {
        ConnectionStatus.Authenticated -> listOf(Component.literal("Currently authenticated"))
        ConnectionStatus.Authenticating -> listOf(Component.literal("Authenticating..."))
        ConnectionStatus.SwitchedOff -> listOf(Component.literal("Unavailable, enable the toggle under \"Account\" category to use"))
        ConnectionStatus.Failed -> listOf(Component.literal("Unavailable, unable to authenticate, look at the logs for details"))
        is ConnectionStatus.RateLimited -> listOf(
            Component.literal("Unavailable, rate limited by the server"),
            Component.literal("Retrying again in ${timeLeftUntil(status.retryAtMs)}").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
        )
    }

    private fun timeLeftUntil(timeMs: Long): String {
        val secondsLeft = ((timeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        val minutes = secondsLeft / SECONDS_PER_MINUTE
        val seconds = secondsLeft % SECONDS_PER_MINUTE

        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }
}
