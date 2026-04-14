package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.SettingNode

abstract class SettingWidget<T>(
    protected val node: SettingNode<T>
) : Drawable, Element {

    // Layout
    var x: Int = 0
    var y: Int = 0
    open var width: Int = 20
    open var height: Int = 40

    protected val childPadding: Int = 4

    open var hovered: Boolean = false
    open var childrenExpanded: Boolean = false

    open val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    protected val borderColor: Int = 0xFF000000.toInt()
    protected val borderSize: Int = 2
    protected val backgroundColor: Int = 0xFF555555.toInt()

    protected val textXPad: Int = 4
    protected val textYPad: Int = 10


    open fun init() {
        layoutChildren()
        childrenWidgets.forEach { it.init() }
    }

    protected open fun layoutChildren() {
        var currentY = y + height + childPadding

        childrenWidgets.forEach {
            it.x = x + 10
            it.y = currentY
            it.width = width - 10
            currentY += it.getTotalHeight() + childPadding
        }
    }

    abstract override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

    protected fun renderChildren(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (!childrenExpanded) return
        childrenWidgets.forEach {
            it.render(ctx, mouseX, mouseY, delta)
        }
    }

    protected fun renderTooltip(ctx: DrawContext, mouseX: Int, mouseY: Int) {
        if (hovered && node.tooltip.isNotBlank()) {
            ctx.drawTooltip(Text.literal(node.tooltip), mouseX + 8, mouseY + 8)
        }
    }


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (childrenExpanded) {
            childrenWidgets.forEach {
                if (it.mouseClicked(click, doubled)) return true
            }
        }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)

        childrenWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.charTyped(input)) return true
        }
        return false
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.keyPressed(input)) return true
        }
        return false
    }

    override fun isFocused(): Boolean = isFocused

    override fun setFocused(focused: Boolean) {
        this.isFocused = focused
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x..(x + width) &&
                mouseY.toInt() in y..(y + height)
    }

    open fun getTotalHeight(): Int {
        if (!childrenExpanded) return height

        return height + childrenWidgets.sumOf {
            it.getTotalHeight() + childPadding
        }
    }

    override fun toString(): String {
        return "${node.displayName}: ${node.value}"
    }
}