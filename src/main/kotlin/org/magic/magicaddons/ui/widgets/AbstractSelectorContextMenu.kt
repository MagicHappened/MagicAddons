package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.max

/**
 * A panel listing [values] as rows under a search field, the field narrowing the rows as
 * the player types. Picking a row hands the value to [onValueSelected]. Opened at ([x], [y]) and
 * folded back on screen by [init] once its size is known.
 */
abstract class AbstractSelectorContextMenu<T>(
    x: Int,
    y: Int,
    val values: List<T>,
    private val title: String,
    private val withSearch: Boolean = true
) : AbstractContextMenu() {

    final override var overlayX: Int = x
        private set
    final override var overlayY: Int = y
        private set

    override var hoveredElement: GuiEventListener? = null

    protected val font = Minecraft.getInstance().font

    protected open val rowHeight = 20
    private val paddingLeft: Int = Common.UI.TEXT_X_PAD
    private val paddingRight: Int = Common.UI.TEXT_X_PAD

    private val titlePadding = Common.UI.SPACING

    private val searchBox = TextField(0, rowHeight, Component.literal(Common.UI.SEARCH_HINT)).apply {
        setResponder { buildWidgets(); layoutRows() }
    }

    protected val valueWidgets: MutableList<ClickableRowWidget<T>> = mutableListOf()

    /** dynamically acquired for the longest names in the overlay. */
    override val overlayWidth: Int
        get() {
            val longest = values.maxOfOrNull { font.width(it.toString()) } ?: 0
            return max(longest, font.width(title)) + paddingLeft + paddingRight
        }

    private val titleHeight: Int get() = font.lineHeight + titlePadding * 2

    private val searchHeight: Int get() = if (withSearch) rowHeight else 0

    override val overlayHeight: Int
        get() = titleHeight + searchHeight + valueWidgets.sumOf { it.height }

    /** Builds the rows, then moves the menu so the whole of it is on screen. */
    open fun init() {
        searchBox.value = ""
        searchBox.focused = true
        buildWidgets()

        val (x, y) = OverlayRenderable.placeOnScreen(overlayX, overlayY, overlayWidth, overlayHeight)
        overlayX = x
        overlayY = y
        layoutRows()
    }

    private fun buildWidgets() {
        valueWidgets.clear()

        values
            .filter { it.toString().contains(searchBox.value.trim(), ignoreCase = true) }
            .forEach { valueWidgets.add(createRow(it)) }
        valueWidgets.lastOrNull()?.dividerBelow = false
    }

    private fun layoutRows() {
        searchBox.x = overlayX
        searchBox.y = overlayY + titleHeight
        searchBox.width = overlayWidth

        var currentY = searchBox.y + searchHeight

        valueWidgets.forEach { widget ->
            widget.x = overlayX
            widget.y = currentY
            widget.width = overlayWidth
            widget.fitHeight(rowHeight)
            currentY += widget.height
        }
    }

    private fun createRow(value: T): ClickableRowWidget<T> =
        ClickableRowWidget(value = value, onClick = { onValueSelected(it.value) })

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BACKGROUND_COLOR)

        graphics.text(
            font,
            Component.literal(title),
            overlayX + paddingLeft,
            overlayY + titlePadding,
            Common.UI.TEXT_COLOR,
            false
        )

        if (withSearch) {
            searchBox.render(graphics)
        } else {
            // a line separates the title from the rows
            val lineY = overlayY + titleHeight - 1
            graphics.fill(overlayX, lineY, overlayX + overlayWidth, lineY + 1, Common.UI.DIVIDER_COLOR)
        }
        valueWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
        graphics.drawBorder(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false
        if (withSearch && searchBox.mouseClicked(mouseButtonEvent, doubled)) return true

        valueWidgets.toList().forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }
        return true
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = withSearch && searchBox.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean = withSearch && searchBox.keyPressed(keyEvent)

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
}
