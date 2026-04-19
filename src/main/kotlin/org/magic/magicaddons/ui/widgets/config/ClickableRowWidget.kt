package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.magic.magicaddons.ui.widgets.BaseRowWidget

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

    override fun render(ctx: DrawContext) {
        super.render(ctx)
        val textRenderer = MinecraftClient.getInstance().textRenderer
        if (onRemove != null) {
            val rx = x + width - removeWidth - removePadding

            ctx.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                Identifier.of("minecraft", "widget/button"),
                rx,
                y + removePadding,
                removeWidth,
                height - removePadding * 2
            )

            ctx.drawText(
                textRenderer,
                Text.literal("X"),
                rx + (removeWidth - textRenderer.getWidth("X"))/2,
                y + removePadding + textRenderer.fontHeight/2,
                0xFFFF0000.toInt(),
                false
            )
        }
    }

    open fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mx = click.x.toInt()
        val my = click.y.toInt()

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