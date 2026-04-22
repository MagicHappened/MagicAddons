package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip

abstract class SettingWidget<T>(
    protected val node: SettingNode<T>
) : Renderable, GuiEventListener, NarratableEntry {

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

    abstract override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float)

    protected fun renderChildren(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        if (!childrenExpanded) return
        childrenWidgets.forEach {
            it.render(graphics, mouseX, mouseY, delta)
        }
    }

    protected fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (hovered && node.tooltip.isNotBlank()) {
            graphics.drawSimpleTooltip(node.tooltip, mouseX + 8, mouseY + 8)
        }
    }


    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (childrenExpanded) {
            childrenWidgets.forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) return true
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

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.charTyped(characterEvent)) return true
        }
        return false
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.keyPressed(keyEvent)) return true
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

    override fun narrationPriority(): NarratableEntry.NarrationPriority {
        return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {
    }

}