package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.Common

abstract class AbstractSelectorContextMenu<T>(
    val values: List<T>
) : AbstractContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    /** Rows are rebuilt from scratch, a second init would otherwise list everything twice. */
    protected fun clearWidgets() {
        valueWidgets.clear()
    }


    protected open fun getMaxRowWidth(): Int {
        val font = Minecraft.getInstance().font

        val maxTextWidth = values.maxOfOrNull {
            font.width(it.toString())
        } ?: 0

        return maxTextWidth + paddingLeft + paddingRight
    }

    protected open val rowHeight = 20
    protected open val paddingLeft: Int = Common.UI.TEXT_X_PAD
    protected open val paddingRight: Int = Common.UI.TEXT_X_PAD

    protected open val rowStartY = overlayY


    open fun init(){
        buildWidgets()
        var currentY = rowStartY
        valueWidgets.forEach { widget ->
            widget.x = overlayX
            widget.y = currentY
            widget.width = overlayWidth
            widget.fitHeight(rowHeight)
            currentY += widget.height
        }
    }

    protected abstract val valueWidgets: MutableList<ClickableRowWidget<T>>

    protected open fun buildWidgets(){
        clearWidgets()

        values.forEach {
            valueWidgets.add(createRow(it))
        }
    }

    protected open fun createRow(value: T): ClickableRowWidget<T> {
        return ClickableRowWidget(
            value = value,
            onClick = { onValueSelected(it.value) }
        )
    }

    // every selector context menu gets the mouse move for its children widgets already
    // then it can implement mouseMoved of its own and call this with super after.
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        valueWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
            if (hoveredElement == null) {
                if (it.isMouseOver(mouseX, mouseY)) {
                    hoveredElement = it
                }
            }
        }
    }

    abstract fun onValueSelected(value: T)
}