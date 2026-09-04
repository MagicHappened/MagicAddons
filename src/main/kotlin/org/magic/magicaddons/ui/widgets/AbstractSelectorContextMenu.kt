package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.max

/**
 * A titled panel listing [values] as rows under a search field, the field narrowing the rows as
 * the player types. Picking a row hands the value to [onValueSelected].
 */
abstract class AbstractSelectorContextMenu<T>(
    val values: List<T>,
    private val title: String,
    /** A handful of rows needs no search field; a long list gets one. */
    private val withSearch: Boolean = true
) : AbstractContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    protected val font = Minecraft.getInstance().font

    protected open val rowHeight = 20
    protected open val paddingLeft: Int = Common.UI.TEXT_X_PAD
    protected open val paddingRight: Int = Common.UI.TEXT_X_PAD

    private val titlePad = Common.UI.SPACING

    private val search = TextField(0, rowHeight, Component.literal(SEARCH_HINT)).apply {
        setResponder { buildWidgets(); layoutRows() }
    }

    protected val valueWidgets: MutableList<ClickableRowWidget<T>> = mutableListOf()

    /** Wide enough for the longest row and the title. */
    override val overlayWidth: Int
        get() {
            val longest = values.maxOfOrNull { font.width(it.toString()) } ?: 0
            return max(longest, font.width(title)) + paddingLeft + paddingRight
        }

    private val titleHeight: Int get() = font.lineHeight + titlePad * 2

    private val searchHeight: Int get() = if (withSearch) rowHeight else 0

    override val overlayHeight: Int
        get() = titleHeight + searchHeight + valueWidgets.sumOf { it.height }

    open fun init() {
        search.value = ""
        search.focused = true
        buildWidgets()
        layoutRows()
    }

    private fun buildWidgets() {
        valueWidgets.clear()

        values
            .filter { it.toString().contains(search.value.trim(), ignoreCase = true) }
            .forEach { valueWidgets.add(createRow(it)) }
        valueWidgets.lastOrNull()?.dividerBelow = false
    }

    private fun layoutRows() {
        search.x = overlayX
        search.y = overlayY + titleHeight
        search.width = overlayWidth

        var currentY = search.y + searchHeight

        valueWidgets.forEach { widget ->
            widget.x = overlayX
            widget.y = currentY
            widget.width = overlayWidth
            widget.fitHeight(rowHeight)
            currentY += widget.height
        }
    }

    protected open fun createRow(value: T): ClickableRowWidget<T> {
        return ClickableRowWidget(
            value = value,
            onClick = { onValueSelected(it.value) }
        )
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BACKGROUND_COLOR)

        graphics.text(
            font,
            Component.literal(title),
            overlayX + paddingLeft,
            overlayY + titlePad,
            Common.UI.TEXT_COLOR,
            false
        )

        if (withSearch) {
            search.render(graphics)
        } else {
            // the search field used to part the title from the rows; a line does it now
            val lineY = overlayY + titleHeight - 1
            graphics.fill(overlayX, lineY, overlayX + overlayWidth, lineY + 1, Common.UI.DIVIDER_COLOR)
        }
        valueWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
        graphics.drawBorder(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false
        if (withSearch && search.mouseClicked(mouseButtonEvent, doubled)) return true

        valueWidgets.toList().forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }
        return true
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = withSearch && search.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean = withSearch && search.keyPressed(keyEvent)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        valueWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
            if (hoveredElement == null && it.isMouseOver(mouseX, mouseY)) {
                hoveredElement = it
            }
        }
    }

    abstract fun onValueSelected(value: T)

    companion object {
        const val SEARCH_HINT: String = "Search…"
    }
}
