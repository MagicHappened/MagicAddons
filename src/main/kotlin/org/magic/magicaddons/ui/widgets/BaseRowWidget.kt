package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

open class BaseRowWidget<T>(
    val value: T
) : Focusable {


    val BUTTON = Identifier.fromNamespaceAndPath("minecraft", "widget/button")


    var hovered = false
    var width: Int = 200
    var height: Int = 20

    var x: Int = 0
    var y: Int = 0

    override var focusedState: Boolean = false
    open val textLeftPadding = 4

    /** Room kept above and below the text when the row grows to fit it. */
    open val textVerticalPadding = 4

    protected val font get() = Minecraft.getInstance().font

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0

    protected open fun getSprite(): Identifier {
        return BUTTON
    }

    protected open fun label(): Component = Component.literal(value.toString())

    /** How wide the text may be before it wraps. */
    protected fun textWidth(): Int =
        width - getLeftReservedWidth() - getRightReservedWidth() - textLeftPadding * 2

    /** Grows the row to hold its wrapped text, never below [minHeight]. Call after setting the width. */
    fun fitHeight(minHeight: Int) {
        height = (wrappedHeight(font, label(), textWidth()) + textVerticalPadding * 2).coerceAtLeast(minHeight)
    }

    open fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val usableWidth = width - getRightReservedWidth() - getLeftReservedWidth()
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            getSprite(),
            x + getLeftReservedWidth(),
            y,
            usableWidth,
            height
        )

        val text = label()
        val textHeight = wrappedHeight(font, text, textWidth())

        graphics.drawWrappedText(
            font,
            text,
            x + textLeftPadding + getLeftReservedWidth(),
            y + (height - textHeight) / 2,
            textWidth(),
            Common.UI.TEXT_COLOR
        )

    }
    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x..x+width && mouseY.toInt() in y..y+height)
    }




    open fun isMouseOverRow(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x+getLeftReservedWidth()..x+width-getRightReservedWidth() && mouseY.toInt() in y..y+height)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOverRow(mouseX, mouseY)
    }


}
