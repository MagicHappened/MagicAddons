package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip

abstract class SettingWidget<T>(
    protected val node: SettingNode<T>,
    var requestRelayout: (() -> Unit)? = null
) : Renderable, Focusable, NarratableEntry {

    override var focusedState: Boolean = false


    var x: Int = 0
    var y: Int = 0
    open var width: Int = 20
    open var height: Int = 40

    protected val childPadding: Int = 4

    /** The gap between a row and the live line under it. */
    private val detailPadding: Int = 2

    var baseWidget = false
    open var hovered: Boolean = false
    open var childrenExpanded: Boolean = false

    abstract val hasChildren: Boolean
    abstract val childrenWidgets: MutableList<SettingWidget<*>>

    val borderColor = Common.UI.BORDER_COLOR
    val borderSize = Common.UI.BORDER_SIZE
    val backgroundColor: Int = Common.UI.BACKGROUND_COLOR
    val textXPad: Int = Common.UI.TEXT_X_PAD

    val textYPad: Int = 10


    open fun initChildren() {
        node.children?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it).apply {
                requestRelayout = {
                    this@SettingWidget.layoutChildrenBut(this@SettingWidget) // this is calling upper layer!! dont touch
                    this@SettingWidget.requestRelayout?.invoke()
                }
            })
        }
    }
    open fun layout(){}

    /** Room this row's live detail wants now, zero when it has none. Asked, since it changes. */
    fun detailHeight(): Int {
        val detail = node.detail?.invoke() ?: return 0

        return detail.height(Minecraft.getInstance().font) + detailPadding
    }

    /** Draws the live line in the strip under the row, if there is one to draw. */
    protected fun renderDetail(graphics: GuiGraphicsExtractor) {
        val detail = node.detail?.invoke() ?: return
        val font = Minecraft.getInstance().font

        detail.render(
            graphics,
            font,
            x + textXPad,
            y + height + detailPadding,
            width - textXPad * 2
        )
    }

    open fun layoutChildren() {
        if (!childrenExpanded) return
        var currentY = y + height + detailHeight() + childPadding

        childrenWidgets.forEach {
            currentY = layoutChild(it,currentY)
        }
    }
    private fun layoutChildrenBut(child: SettingWidget<*>) {
        if (!childrenExpanded) return
        var currentY = y + height + detailHeight() + childPadding

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

    abstract override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)

    protected fun extractChildrenRenderStates(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (!childrenExpanded) return
        childrenWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }
    }

     fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
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
        childrenWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
    }

    /** The deepest widget under the mouse, children before parents, or null when it is elsewhere. */
    open fun hoveredWidget(): SettingWidget<*>? =
        childrenWidgets.firstNotNullOfOrNull { it.hoveredWidget() } ?: takeIf { hovered }

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

    /** The wheel, offered to this row and then to whatever it has open. */
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        }
        return false
    }

    /**
     * Drag and release, handed round by hand: the config screen consumes clicks itself, so vanilla
     * never records which widget is being dragged.
     */
    override fun mouseDragged(
        mouseButtonEvent: MouseButtonEvent,
        dragX: Double,
        dragY: Double
    ): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.mouseDragged(mouseButtonEvent, dragX, dragY)) return true
        }
        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        if (!childrenExpanded) return false

        childrenWidgets.forEach {
            if (it.mouseReleased(mouseButtonEvent)) return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x until (x + width) &&
                mouseY.toInt() in y until (y + height)
    }

    open fun getTotalHeight(): Int {
        if (!childrenExpanded) return height + detailHeight()

        return height + detailHeight() + childrenWidgets.sumOf {
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