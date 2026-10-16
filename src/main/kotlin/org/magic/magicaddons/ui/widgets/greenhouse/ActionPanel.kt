package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormat
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.ui.widgets.PickContext
import org.magic.magicaddons.ui.widgets.PickWithOptionContext
import org.magic.magicaddons.util.ChatUtils

abstract class ActionPanel(protected val overlayContext: OverlayContext) : Renderable, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    var x: Int = 0
        private set

    var y: Int = 0
        private set

    var availableWidth: Int = 0
        private set

    protected abstract val buttons: List<ClickableButtonWidget>

    protected open fun isShown(button: ClickableButtonWidget): Boolean = true

    protected open fun headerHeight(): Int = 0

    protected abstract fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean

    protected open fun groupOf(button: ClickableButtonWidget): Int = 0

    protected open fun groupLabel(group: Int): String? = null

    protected open val dropdowns: List<DropdownWidget<*>> = emptyList()

    protected open fun isShown(dropdown: DropdownWidget<*>): Boolean = true

    protected open fun groupOf(dropdown: DropdownWidget<*>): Int = 0

    private val groupLabelPositions = mutableMapOf<Int, Pair<Int, Int>>()

    private val font get() = Minecraft.getInstance().font

    private fun shownDropdowns(): List<DropdownWidget<*>> = dropdowns.filter { isShown(it) }

    fun layoutIn(x: Int, y: Int, availableWidth: Int) {
        this.x = x
        this.y = y
        this.availableWidth = availableWidth
        groupLabelPositions.clear()

        var rowY = y + PADDING + headerHeight()
        val shownButtonsByGroup = buttons.filter { isShown(it) }.groupBy { groupOf(it) }
        val shownDropdownsByGroup = shownDropdowns().groupBy { groupOf(it) }

        (shownButtonsByGroup.keys + shownDropdownsByGroup.keys).sorted().forEach { group ->
            groupLabel(group)?.let {
                groupLabelPositions[group] = (x + PADDING) to rowY
                rowY += font.lineHeight + Common.UI.SPACING
            }

            var rowX = x + PADDING
            var rowHeight = 0

            fun place(width: Int, height: Int): Pair<Int, Int> {
                if (rowX + width > x + availableWidth - PADDING && rowX > x + PADDING) {
                    rowX = x + PADDING
                    rowY += rowHeight + Common.UI.SPACING
                    rowHeight = 0
                }
                val at = rowX to rowY
                rowX += width + Common.UI.SPACING
                rowHeight = maxOf(rowHeight, height)
                return at
            }

            shownButtonsByGroup[group].orEmpty().forEach { button ->
                val (buttonX, buttonY) = place(button.width, button.height)
                button.x = buttonX
                button.y = buttonY
            }
            shownDropdownsByGroup[group].orEmpty().forEach { dropdown ->
                dropdown.fitToValues(availableWidth - PADDING * 2)
                dropdown.height = ClickableButtonWidget.DEFAULT_HEIGHT
                val (dropdownX, dropdownY) = place(dropdown.width, dropdown.height)
                dropdown.x = dropdownX
                dropdown.y = dropdownY
            }

            rowY += rowHeight + Common.UI.SPACING_LARGE
        }
    }

    fun hasShownButtons(): Boolean = buttons.any { isShown(it) } || shownDropdowns().isNotEmpty()

    val contentHeight: Int
        get() {
            val bottom = (buttons.filter { isShown(it) }.map { it.y + it.height } + shownDropdowns().map { it.y + it.height })
                .maxOrNull() ?: return 0

            return bottom - y + PADDING
        }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        groupLabelPositions.forEach { (group, at) ->
            groupLabel(group)?.let { graphics.modText(font, it, at.first, at.second, Common.UI.TEXT_DIM_COLOR) }
        }
        buttons.filter { isShown(it) }
            .forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }
        shownDropdowns().forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }
    }

    open fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (shownDropdowns().any { it.mouseClicked(mouseButtonEvent, doubled) }) return true
        buttons.filter { isShown(it) }.forEach { button ->
            if (button.mouseClicked(mouseButtonEvent, doubled)) {
                return onPressed(button, mouseButtonEvent)
            }
        }

        return false
    }

    open fun mouseMoved(mouseX: Double, mouseY: Double) {
        buttons.forEach { it.mouseMoved(mouseX, mouseY) }
        dropdowns.forEach { it.mouseMoved(mouseX, mouseY) }
        hoveredElement = buttons.firstOrNull { isShown(it) && it.isMouseOver(mouseX, mouseY) }
    }

    protected fun <T> openMenu(event: MouseButtonEvent, title: String, values: List<T>, onPick: (T) -> Unit) {
        val menu = PickContext(event.x.toInt(), event.y.toInt(), title, values, overlayContext, onPick)
        menu.init()
        overlayContext.addContext(menu)
    }

    protected fun <T> openMenuWithOption(
        event: MouseButtonEvent,
        title: String,
        values: List<T>,
        optionLabel: String,
        isOptionChecked: Boolean,
        optionTooltip: String,
        onPick: (T, Boolean) -> Unit
    ) {
        val menu = PickWithOptionContext(
            event.x.toInt(), event.y.toInt(), title, values,
            optionLabel, isOptionChecked, optionTooltip, overlayContext, onPick
        )
        menu.init()
        overlayContext.addContext(menu)
    }

    protected fun copyExportToClipboard(result: LayoutTransferResult, format: LayoutFormat, layoutName: String) {
        result.notes.forEach { ChatUtils.sendWithPrefix(it) }

        when (result) {
            is LayoutTransferResult.Failure -> ChatUtils.sendWithPrefix(result.reason)
            is LayoutTransferResult.Exported -> {
                Minecraft.getInstance().keyboardHandler.clipboard = result.text
                ChatUtils.sendWithPrefix("Copied a ${format.displayName} layout for $layoutName to your clipboard")
            }
            is LayoutTransferResult.Imported -> Unit
        }
    }

    companion object {
        const val PADDING: Int = 6

        const val NO_ROTATE_LABEL: String = "No Rotate"
        const val NO_ROTATE_TOOLTIP: String = "Disables automatic rotation for the least amount of effort when building a preset"
    }
}
