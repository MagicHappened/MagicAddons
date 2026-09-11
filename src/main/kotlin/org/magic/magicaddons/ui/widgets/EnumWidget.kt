package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.ScrollView
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.stepScroll
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
    val onRightClickValue: ((T?, MouseButtonEvent) -> Unit)? = null,
    val valueChanged: ((T) -> Unit)? = null,
    /** Whether the open box turns into a search field; a short list has nothing to search. */
    val searchable: Boolean = true,
) : Renderable {
    val overlay = EnumOverlay()

    /** The gap between frame and contents, wide enough that the name is not touching the frame. */
    private val textPad: Int = Common.UI.TEXT_X_PAD + Common.UI.CONTROL_BORDER_SIZE

    /** Narrow enough to still look like a selector when every value is a short word. */
    private val minWidth: Int = 60

    private val font = Minecraft.getInstance().font
    private var overlayOpen = false
    private var hovered = false

    /** A frame colour of the owner's choosing, when the pick is worth showing on the box itself. */
    var frameColor: Int? = null

    /** How many pixels the open list may take, null for whatever the screen has. */
    var overlayBudget: Int? = null

    /** Typing here narrows the rows to the values containing the text. Shown in the box while open. */
    private val search = TextField(0, 0, Component.literal(Common.UI.SEARCH_HINT)).apply {
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
        openedAt = System.currentTimeMillis()
        overlay.rebuildRows()
        overlayContext.addOverlay(overlay)
    }

    /** When the list was opened, so it can grow out from the selector rather than appear whole. */
    private var openedAt: Long = 0L

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
        val everyName = values.map { it.toString() } + listOfNotNull(currentValue?.toString()) + PLACEHOLDER
        val longestNameWidth = everyName.maxOfOrNull { font.width(it) } ?: 0

        width = (longestNameWidth + textPad * 2 + font.width(ARROW) + Common.UI.SPACING)
            .coerceIn(minWidth, maxWidth.coerceAtLeast(minWidth))
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val textY = y + (height - font.lineHeight) / 2
        val arrow = arrowText()

        // the arrow keeps to the far side and the name is given what is left, so a long name runs
        // out of room before it runs into the arrow rather than under it
        val roomForName = arrowLeft() - Common.UI.SPACING - (x + textPad)

        // open, the whole box is the search field; closed, it is a button showing the pick
        if (overlayOpen && searchable) {
            search.x = x
            search.y = y
            search.width = width
            search.height = height
            search.render(graphics)
        } else {
            graphics.drawButtonPanel(
                x, y, x + width, y + height, hovered,
                pressed = overlayOpen,
                frame = frameColor ?: Common.UI.BORDER_COLOR,
                frameSize = Common.UI.CONTROL_BORDER_SIZE
            )
            val shownName = ellipsised(font, currentValue?.toString() ?: PLACEHOLDER, roomForName)
            graphics.modText(font, Component.literal(shownName), x + textPad, textY, Common.UI.TEXT_COLOR)
        }

        graphics.modText(font, Component.literal(arrow), arrowLeft(), textY, Common.UI.TEXT_COLOR)
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
    }

    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        when (mouseButtonEvent.button()) {
            0 -> {
                // open, a click in the field moves the caret; only the arrow's side shuts the list
                if (overlayOpen && searchable && search.mouseClicked(mouseButtonEvent, false)) return true
                if (overlayOpen) close() else open()
            }
            1 -> onRightClickValue?.invoke(currentValue, mouseButtonEvent)
        }
        return true
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, width, height)

    inner class EnumOverlay : OverlayRenderable {

        override val renderPriority: Int = OverlayRenderable.DROPDOWN_PRIORITY

        // closing the overlay any other way, such as a click landing outside it, would otherwise
        // leave the widget believing it is still open and swallow the next click on it
        override fun onClosed() {
            overlayOpen = false
            search.focused = false
        }

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
        private fun viewTop(): Int = (McCompat.currentScreen() as? ScrollView)?.viewTop ?: 0

        /** The far side of the room an overlay may take, which is the panel's edge or the screen's. */
        private fun viewRight(): Int =
            (McCompat.currentScreen() as? ScrollView)?.viewRight
                ?: McCompat.currentScreen()?.width
                ?: Minecraft.getInstance().window.guiScaledWidth
        private fun viewBottom(): Int =
            (McCompat.currentScreen() as? ScrollView)?.viewBottom
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

            val roomForRows = (if (opensDown) {
                viewBottom() - (this@EnumWidget.y + this@EnumWidget.height)
            } else {
                this@EnumWidget.y - viewTop()
            }).coerceAtMost(overlayBudget ?: Int.MAX_VALUE)

            visibleRows = (roomForRows / overlayRowHeight).coerceAtLeast(1)

            buildWindow()
        }

        /** The rows for the stretch of the list the scroll is looking at. */
        private fun buildWindow() {
            valueWidgets.clear()

            matching.drop(scroll).take(visibleRows).forEach { value ->
                valueWidgets.add(
                    ClickableRowWidget(value).apply {
                        selected = value == currentValue
                        // a long file name reads better cut short than wrapped onto a second line
                        wrapText = false
                    }
                )
            }
            valueWidgets.lastOrNull()?.dividerBelow = false

            layoutOverlay()
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
            if (!isMouseOver(mouseX, mouseY)) return false
            if (matching.size <= visibleRows) return true

            scroll = stepScroll(scroll, scrollY, matching.size, visibleRows)

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
        /**
         * As wide as the longest name needs, but never past the room left between the selector and
         * the edge it opens against. Names that still do not fit are cut short by the rows instead.
         */
        override val overlayWidth: Int
            get() {
                val longestNameWidth = matching.maxOfOrNull { font.width(it.toString()) } ?: 0
                val wantedWidth = longestNameWidth + textPad * 2
                val roomToEdge = viewRight() - overlayX

                return wantedWidth.coerceAtMost(roomToEdge).coerceAtLeast(this@EnumWidget.width)
            }
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

            // clipped to how far it has opened, growing away from the selector on whichever side it
            // sits; not clipped at all once open, since on a scaled screen the clip rounds down and
            // shaves the right and bottom borders off
            val opened = eased(openedAt, OPEN_MS)
            val stillOpening = opened < 1f
            if (stillOpening) {
                val shownHeight = kotlin.math.round(overlayHeight * opened).toInt()
                val clipTop = if (opensDown) overlayY else overlayY + overlayHeight - shownHeight
                graphics.enableScissor(overlayX, clipTop, overlayX + overlayWidth, clipTop + shownHeight)
            }

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

            if (stillOpening) graphics.disableScissor()
        }

        override fun charTyped(characterEvent: CharacterEvent): Boolean = searchable && search.charTyped(characterEvent)

        override fun keyPressed(keyEvent: KeyEvent): Boolean = searchable && search.keyPressed(keyEvent)

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
            valueWidgets.toList().forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) {
                    when (mouseButtonEvent.button()) {
                        0 -> this@EnumWidget.valueChanged(it.value)
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
        const val PLACEHOLDER: String = "Select…"

        /** How long an opening list takes to grow to its full height. */
        const val OPEN_MS: Long = 150
    }
}
