package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.screens.FeatureEditScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip

abstract class SettingWidget<T>(
    protected val node: SettingNode<T>,
    var requestRelayout: (() -> Unit)? = null
) : Renderable, GuiEventListener, NarratableEntry {

    var x: Int = 0
    var y: Int = 0
    open var width: Int = 20
    open var height: Int = 40

    protected val childPadding: Int = 4
    var baseWidget = false
    open var hovered: Boolean = false
    open var childrenExpanded: Boolean = false

    abstract val hasChildren: Boolean
    abstract val childrenWidgets: MutableList<SettingWidget<*>>

    protected val borderColor: Int = 0xFF000000.toInt()
    protected val borderSize: Int = 2
    protected val backgroundColor: Int = 0xFF555555.toInt()

    protected val textXPad: Int = 4
    protected val textYPad: Int = 10


    open fun initChildren() {
        node.children?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it).apply {
                requestRelayout = {
                    this@SettingWidget.layoutChildrenBut(this@SettingWidget) // this is calling upper layer!! dont touch
                }
            })
        }
    }
    open fun layout(){}

    open fun layoutChildren() {
        if (!childrenExpanded) return
        var currentY = y + height + childPadding

        childrenWidgets.forEach {
            currentY = layoutChild(it,currentY)
        }
    }
    private fun layoutChildrenBut(child: SettingWidget<*>) {
        if (!childrenExpanded) return
        var currentY = y + height + childPadding

        childrenWidgets.forEach {
            if (it == child) return@forEach
            currentY = layoutChild(it,currentY)
        }
    }

    private fun layoutChild(child: SettingWidget<*>, currentY: Int): Int {
        child.x = x + 10
        child.y = currentY
        child.width = width - 10
        child.layout()
        child.layoutChildren()
        return currentY + child.getTotalHeight() + childPadding
    }

    abstract override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float)

    protected fun renderChildren(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        if (!childrenExpanded) return
        childrenWidgets.forEach {
            it.render(graphics, mouseX, mouseY, delta)
        }
    }

     fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (hovered && node.tooltip.isNotBlank()) {
            graphics.drawSimpleTooltip(node.tooltip, mouseX + 8, mouseY + 8)
        }
    }


    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val inside = isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

        if (inside && mouseButtonEvent.button() == 1) { //right clicked on widget
            if (!hasChildren){
                return false // we call super after base widget so this is fine
            }
            if (!childrenExpanded){
                childrenExpanded = true
                initChildren()
                layoutChildren()
            }
            else {
                childrenWidgets.clear()
                childrenExpanded = false
            }

            if (!baseWidget){ // prevent triggering twice.
                requestRelayout?.invoke()
            }
            return true
        }

        if (childrenExpanded) {
            childrenWidgets.forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled))
                    return true
            }
        }


        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
        val currentScreen = Minecraft.getInstance().screen
        if (currentScreen is FeatureEditScreen && hovered) {
            currentScreen.hoveredWidget = this
        }
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