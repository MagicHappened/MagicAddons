package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.screens.ScrollableScreen
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.compat.McCompat

/**
 * A selector: a button showing the picked value. Open, the button becomes a search field with the
 * list of values under it, narrowed by what is typed. The list scrolls when the screen runs out of room.
 */
class EnumWidget<T>(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var values: List<T>,
    var currentValue: T?,
    val overlayContext: OverlayContext,
    val onLeftClickValue: ((T?, MouseButtonEvent) -> Unit)? = null,
    val onRightClickValue: ((T?, MouseButtonEvent) -> Unit)? = null,
    val valueChanged: ((T) -> Unit)? = null,
    /** Whether the open box turns into a search field; a short list has nothing to search. */
    val searchable: Boolean = true,
) : Renderable, Focusable {
    val overlay = EnumOverlay(1)

    /** The gap between frame and contents, wide enough that the name is not touching the frame. */
    private val textPad: Int = Common.UI.TEXT_X_PAD + Common.UI.BORDER_SIZE

    /** Narrow enough to still look like a selector when every value is a short word. */
    private val minWidth: Int = 60

    val font = Minecraft.getInstance().font
    var overlayOpen = false
    var hovered = false

    /** How many pixels the open list may take, null for whatever the screen has. */
    var overlayBudget: Int? = null

    override var focusedState: Boolean = false

    /** Typing here narrows the rows to the values containing the text. Shown in the box while open. */
    private val search = TextField(0, 0, Component.literal(SEARCH_HINT)).apply {
        setResponder { if (overlayOpen) overlay.rebuildRows() }
    }

    private fun valueChanged(newValue: T) {
        currentValue = newValue
        close()
        valueChanged?.invoke(newValue)
    }

    private fun open() {
        search.value = ""
        search.focused = searchable
        overlayOpen = true
        overlay.rebuildRows()
        overlayContext.addOverlay(overlay)
    }

    private fun close() {
        overlayOpen = false
        search.focused = false
        overlayContext.removeOverlay(overlay)
    }

    /** Where the arrow sits, kept clear of the name and of the search field. */
    private fun arrowText(): String =
        if (if (overlayOpen) overlay.opensDown else overlay.wouldOpenDown(values.size)) ARROW else ARROW_UP

    private fun arrowLeft(): Int = x + width - font.width(arrowText()) - textPad

    /** Shuts the list without picking anything, for a screen laying itself out again. */
    fun closeList() = close()

    /**
     * Sets the width from the longest value it might show. Measured rather than guessed, so a name
     * is only ellipsised when it is too long for the screen.
     */
    fun fitToValues(maxWidth: Int) {
        val shown = values.map { it.toString() } + listOfNotNull(currentValue?.toString()) + PLACEHOLDER
        val longest = shown.maxOfOrNull { font.width(it) } ?: 0

        width = (longest + textPad * 2 + font.width(ARROW) + Common.UI.SPACING)
            .coerceIn(minWidth, maxWidth.coerceAtLeast(minWidth))
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val textY = y + (height - font.lineHeight) / 2
        val arrow = arrowText()

        // the arrow keeps to the far side and the name is given what is left, so a long name runs
        // out of room before it runs into the arrow rather than under it
        val room = arrowLeft() - Common.UI.SPACING - (x + textPad)

        // open, the whole box is the search field; closed, it is a button showing the pick
        if (overlayOpen && searchable) {
            search.x = x
            search.y = y
            search.width = width
            search.height = height
            search.render(graphics)
        } else {
            graphics.drawButtonPanel(x, y, x + width, y + height, hovered, pressed = overlayOpen)
            val name = currentValue?.toString() ?: PLACEHOLDER
            val shown = if (font.width(name) <= room) {
                name
            } else {
                font.plainSubstrByWidth(name, room - font.width(ELLIPSIS)) + ELLIPSIS
            }
            graphics.text(font, Component.literal(shown), x + textPad, textY, Common.UI.TEXT_COLOR, false)
        }

        graphics.text(font, Component.literal(arrow), arrowLeft(), textY, Common.UI.TEXT_COLOR, false)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        when (mouseButtonEvent.button()) {
            0 -> {
                // open, a click in the field moves the caret; only the arrow's side shuts the list
                if (overlayOpen && searchable && search.mouseClicked(mouseButtonEvent, false)) return true
                if (overlayOpen) close() else open()
                onLeftClickValue?.invoke(currentValue, mouseButtonEvent)
            }
            1 -> onRightClickValue?.invoke(currentValue, mouseButtonEvent)
        }
        return true
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x until x + width &&
                mouseY.toInt() in y until y + height
    }

    inner class EnumOverlay(override val renderPriority: Int) : OverlayRenderable, Focusable {

        // closing the overlay any other way, such as a click landing outside it, would otherwise
        // leave the widget believing it is still open and swallow the next click on it
        override fun onClosed() {
            overlayOpen = false
            search.focused = false
        }

        override var focusedState: Boolean = false

        override var hoveredElement: GuiEventListener? = null

        /** A row is as tall as the closed selector, so the list reads as the same control opened. */
        val overlayRowHeight: Int
            get() = this@EnumWidget.height

        val valueWidgets: MutableList<ClickableRowWidget<T>> = mutableListOf()

        /** Whether the list grows downward from the widget, settled when the rows are built. */
        var opensDown: Boolean = true
            private set

        /** Which way a list of [rowsWanted] rows would open: down when it fits, else the roomier side. */
        fun wouldOpenDown(rowsWanted: Int): Boolean {
            val spaceBelow = viewBottom() - (this@EnumWidget.y + this@EnumWidget.height)
            val spaceAbove = this@EnumWidget.y - viewTop()

            return heightFor(rowsWanted) <= spaceBelow || spaceBelow >= spaceAbove
        }

        private fun heightFor(rows: Int): Int = overlayRowHeight * rows

        /** The visible edges in the widget's own coordinates, which scroll on a scrolling screen. */
        private fun viewTop(): Int = ScrollableScreen.current()?.viewTop ?: 0
        private fun viewBottom(): Int =
            ScrollableScreen.current()?.viewBottom
                ?: McCompat.currentScreen()?.height
                ?: Minecraft.getInstance().window.guiScaledHeight

        /** Everything the search lets through, of which a scrolled window is on screen. */
        private var matching: List<T> = emptyList()

        private var visibleRows: Int = 1
        private var scroll: Int = 0

        /** Builds the rows the search lets through, no more than the room allows. The rest is scrolled. */
        fun rebuildRows() {
            scroll = 0

            matching = values.filter { it.toString().contains(search.value.trim(), ignoreCase = true) }

            opensDown = wouldOpenDown(matching.size)

            val space = (if (opensDown) {
                viewBottom() - (this@EnumWidget.y + this@EnumWidget.height)
            } else {
                this@EnumWidget.y - viewTop()
            }).coerceAtMost(overlayBudget ?: Int.MAX_VALUE)

            visibleRows = (space / overlayRowHeight).coerceAtLeast(1)

            buildWindow()
        }

        /** The rows for the stretch of the list the scroll is looking at. */
        private fun buildWindow() {
            valueWidgets.clear()

            matching.drop(scroll).take(visibleRows).forEach { value ->
                valueWidgets.add(ClickableRowWidget(value).apply { selected = value == currentValue })
            }
            valueWidgets.lastOrNull()?.dividerBelow = false

            layoutOverlay()
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
            if (!isMouseOver(mouseX.toInt(), mouseY.toInt())) return false
            if (matching.size <= visibleRows) return true

            scroll = (scroll - scrollY.toInt().coerceIn(-1, 1))
                .coerceIn(0, matching.size - visibleRows)

            buildWindow()
            return true
        }

        override val overlayX: Int
            get() = this@EnumWidget.x

        /** Under the selector, or above it when the screen runs out: a list off the bottom cannot be picked. */
        override val overlayY: Int
            get() = if (opensDown) {
                this@EnumWidget.y + this@EnumWidget.height
            } else {
                (this@EnumWidget.y - overlayHeight).coerceAtLeast(viewTop())
            }
        override val overlayWidth: Int
            get() = this@EnumWidget.width
        override val overlayHeight: Int
            get() = heightFor(valueWidgets.size)

        fun layoutOverlay() {
            var currentY = overlayY

            valueWidgets.forEach {
                it.x = overlayX
                it.y = currentY
                it.width = overlayWidth
                it.height = overlayRowHeight
                currentY += overlayRowHeight
            }
        }

        override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
            layoutOverlay()

            valueWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
            graphics.drawBorder(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

            if (matching.size > visibleRows) {
                graphics.drawScrollBar(
                    overlayX + overlayWidth - Common.UI.SCROLLBAR_WIDTH - Common.UI.BORDER_SIZE,
                    overlayY,
                    overlayHeight,
                    matching.size,
                    visibleRows,
                    scroll
                )
            }
        }

        override fun charTyped(characterEvent: CharacterEvent): Boolean = searchable && search.charTyped(characterEvent)

        override fun keyPressed(keyEvent: KeyEvent): Boolean = searchable && search.keyPressed(keyEvent)

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
            valueWidgets.toList().forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) {
                    when (mouseButtonEvent.button()) {
                        0 -> {
                            this@EnumWidget.onLeftClickValue?.invoke(it.value, mouseButtonEvent)
                            this@EnumWidget.valueChanged(it.value)
                        }
                        1 -> this@EnumWidget.onRightClickValue?.invoke(it.value, mouseButtonEvent)
                    }
                    return true
                }
            }
            return false
        }

        override fun mouseMoved(mouseX: Double, mouseY: Double) {
            hoveredElement = null
            valueWidgets.forEach {
                it.mouseMoved(mouseX, mouseY)
                if (hoveredElement == null && it.isMouseOverRow(mouseX, mouseY)) {
                    hoveredElement = it
                }
            }
        }
    }

    private companion object {
        const val ARROW: String = "↓"
        const val ARROW_UP: String = "↑"
        const val ELLIPSIS: String = "…"
        const val PLACEHOLDER: String = "Select…"
        const val SEARCH_HINT: String = "Search…"
    }
}
