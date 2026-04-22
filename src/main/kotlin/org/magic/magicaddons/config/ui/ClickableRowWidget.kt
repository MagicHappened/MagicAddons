package org.magic.magicaddons.config.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import tech.thatgravyboat.skyblockapi.platform.drawTexture

open class ClickableRowWidget<T>(
    value: T,
    displayText: (T) -> String,
    val onClick: (ClickableRowWidget<T>) -> Unit,
    val onRemove: ((ClickableRowWidget<T>) -> Unit)? = null
) : BaseRowWidget<T>(value, displayText) {

    private val removeWidth = 18
    private val removePadding = 2

    override fun getRightReservedWidth(): Int {
        return super.getRightReservedWidth() + removeWidth
    }

    override fun render(graphics: GuiGraphics) {
        super.render(graphics)
        val font = Minecraft.getInstance().font
        if (onRemove != null) {
            val rx = x + width - removeWidth - removePadding

            graphics.drawTexture(
                Identifier.fromNamespaceAndPath("minecraft", "widget/button"),
                rx,
                y+ removePadding,
                removeWidth,
                height - removePadding * 2
            )

            graphics.drawString(
                font,
                Component.literal("X"),
                rx + (removeWidth - font.width("X"))/2,
                y + removePadding + font.lineHeight/2,
                0xFFFF0000.toInt(),
                false
            )
        }
    }

    open fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = mouseButtonEvent.x.toInt()
        val my = mouseButtonEvent.y.toInt()

        if (mx !in x..x + width || my !in y..y + height) return false

        if (onRemove != null) {
            val rx = x + width - removeWidth
            if (mx in rx..x + width) {
                onRemove.invoke(this)
                return true
            }
        }

        onClick(this)
        return true
    }
}