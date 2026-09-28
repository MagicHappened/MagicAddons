package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.util.ScreenUtil.easedProgress
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

class DropdownWidget<T>(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var values: List<T>,
    var currentValue: T?,
    val overlayContext: OverlayContext,
    val onRightClickValue: ((T?, MouseButtonEvent) -> Unit)? = null,
    val onValueChanged: ((T) -> Unit)? = null,
    val isSearchable: Boolean = true,
) : Renderable {
    val list = DropdownList()

    private val font = Minecraft.getInstance().font
    private var isListOpen = false
    private var isHovered = false

    var frameColor: Int? = null

    var overlayBudget: Int? = null

    private val searchBox = TextField(0, 0, Component.literal(Common.UI.SEARCH_HINT)).apply {
        setResponder { if (isListOpen) list.rebuildRows() }
    }

    private fun pickValue(newValue: T) {
        currentValue = newValue
        closeList()
        onValueChanged?.invoke(newValue)
    }

    private fun openList() {
        searchBox.value = ""
        searchBox.isFocused = isSearchable
        isListOpen = true
        openedAt = System.currentTimeMillis()
        list.rebuildRows()
        overlayContext.addOverlay(list)
    }

    private var openedAt: Long = 0L

    fun closeList() {
        isListOpen = false
        searchBox.isFocused = false
        overlayContext.removeOverlay(list)
    }

    private fun arrowText(): String =
        if (if (isListOpen) list.opensDown else list.wouldOpenDown(values.size)) ARROW_DOWN else ARROW_UP

    private fun arrowLeft(): Int = x + width - font.width(arrowText()) - TEXT_PADDING

    private var longestValueWidth: Int = 0

    private var measuredValuesFingerprint: Int? = null

    fun fitToValues(maxWidth: Int) {
        val valuesFingerprint = 31 * values.hashCode() + currentValue.hashCode()
        if (measuredValuesFingerprint != valuesFingerprint) {
            val everyName = values.map { it.toString() } + listOfNotNull(currentValue?.toString()) + PLACEHOLDER
            longestValueWidth = everyName.maxOfOrNull { font.width(it) } ?: 0
            measuredValuesFingerprint = valuesFingerprint
        }

        width = (longestValueWidth + TEXT_PADDING * 2 + font.width(ARROW_DOWN) + Common.UI.SPACING)
            .coerceIn(MIN_WIDTH, maxWidth.coerceAtLeast(MIN_WIDTH))
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val textY = y + (height - font.lineHeight) / 2
        val arrow = arrowText()

        val roomForName = arrowLeft() - Common.UI.SPACING - (x + TEXT_PADDING)

        if (isListOpen && isSearchable) {
            searchBox.x = x
            searchBox.y = y
            searchBox.width = width
            searchBox.height = height
            searchBox.render(graphics)
        } else {
            graphics.drawButtonPanel(
                x, y, x + width, y + height, isHovered,
                pressed = isListOpen,
                frame = frameColor ?: Common.UI.BORDER_COLOR,
                frameSize = Common.UI.CONTROL_BORDER_SIZE
            )
            val shownName = ellipsised(font, currentValue?.toString() ?: PLACEHOLDER, roomForName)
            graphics.modText(font, Component.literal(shownName), x + TEXT_PADDING, textY, Common.UI.TEXT_COLOR)
        }

        graphics.modText(font, Component.literal(arrow), arrowLeft(), textY, Common.UI.TEXT_COLOR)
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        isHovered = isMouseOver(mouseX, mouseY)
    }

    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        when (mouseButtonEvent.button()) {
            0 -> {
                if (isListOpen && isSearchable && searchBox.mouseClicked(mouseButtonEvent, false)) return true
                if (isListOpen) closeList() else openList()
            }
            1 -> onRightClickValue?.invoke(currentValue, mouseButtonEvent)
        }
        return true
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, width, height)

    inner class DropdownList : OverlayRenderable {

        override val renderPriority: Int = OverlayRenderable.DROPDOWN_PRIORITY

        override fun onClosed() {
            isListOpen = false
            searchBox.isFocused = false
        }

        override var hoveredElement: GuiEventListener? = null

        val rowHeight: Int
            get() = this@DropdownWidget.height

        val rows: MutableList<RowWidget<T>> = mutableListOf()

        var opensDown: Boolean = true
            private set

        fun wouldOpenDown(rowsWanted: Int): Boolean {
            val spaceBelow = viewBottom() - (this@DropdownWidget.y + this@DropdownWidget.height)
            val spaceAbove = this@DropdownWidget.y - viewTop()

            return listHeightFor(rowsWanted) <= spaceBelow || spaceBelow >= spaceAbove
        }

        private fun listHeightFor(rowCount: Int): Int = rowHeight * rowCount

        private fun viewTop(): Int = (McCompat.currentScreen() as? ScrollView)?.viewTop ?: 0

        private fun viewRight(): Int =
            (McCompat.currentScreen() as? ScrollView)?.viewRight
                ?: McCompat.currentScreen()?.width
                ?: Minecraft.getInstance().window.guiScaledWidth

        private fun viewBottom(): Int =
            (McCompat.currentScreen() as? ScrollView)?.viewBottom
                ?: McCompat.currentScreen()?.height
                ?: Minecraft.getInstance().window.guiScaledHeight

        private var matchingValues: List<T> = emptyList()

        private var visibleRows: Int = 1
        private var scroll: Int = 0

        fun rebuildRows() {
            scroll = 0

            matchingValues = values.filter { it.toString().contains(searchBox.value.trim(), ignoreCase = true) }

            opensDown = wouldOpenDown(matchingValues.size)

            val roomForRows = (if (opensDown) {
                viewBottom() - (this@DropdownWidget.y + this@DropdownWidget.height)
            } else {
                this@DropdownWidget.y - viewTop()
            }).coerceAtMost(overlayBudget ?: Int.MAX_VALUE)

            visibleRows = (roomForRows / rowHeight).coerceAtLeast(1)

            buildVisibleRows()
        }

        private fun buildVisibleRows() {
            longestRowWidth = null

            rows.clear()

            matchingValues.drop(scroll).take(visibleRows).forEach { value ->
                rows.add(
                    RowWidget(value).apply {
                        isSelected = value == currentValue
                        isTextWrapped = false
                    }
                )
            }
            rows.lastOrNull()?.hasDividerBelow = false

            layoutRows()
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
            if (!isMouseOver(mouseX, mouseY)) return false
            if (matchingValues.size <= visibleRows) return true

            scroll = stepScroll(scroll, scrollY, matchingValues.size, visibleRows)

            buildVisibleRows()
            return true
        }

        override val overlayX: Int
            get() = this@DropdownWidget.x

        override val overlayY: Int
            get() = if (opensDown) {
                this@DropdownWidget.y + this@DropdownWidget.height
            } else {
                (this@DropdownWidget.y - overlayHeight).coerceAtLeast(viewTop())
            }

        private var longestRowWidth: Int? = null

        override val overlayWidth: Int
            get() {
                val longestNameWidth = longestRowWidth ?: (
                    matchingValues.drop(scroll).take(visibleRows).maxOfOrNull { font.width(it.toString()) } ?: 0
                ).also { longestRowWidth = it }
                val wantedWidth = longestNameWidth + TEXT_PADDING * 2
                val roomToEdge = viewRight() - overlayX

                return wantedWidth.coerceAtMost(roomToEdge).coerceAtLeast(this@DropdownWidget.width)
            }

        override val overlayHeight: Int
            get() = listHeightFor(rows.size)

        fun layoutRows() {
            var currentY = overlayY
            val rowWidth = overlayWidth

            rows.forEach {
                it.x = overlayX
                it.y = currentY
                it.width = rowWidth
                it.height = rowHeight
                currentY += rowHeight
            }
        }

        override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
            layoutRows()

            val openProgress = easedProgress(openedAt, OPEN_MS)
            val stillOpening = openProgress < 1f
            if (stillOpening) {
                val shownHeight = kotlin.math.round(overlayHeight * openProgress).toInt()
                val clipTop = if (opensDown) overlayY else overlayY + overlayHeight - shownHeight
                graphics.enableScissor(overlayX, clipTop, overlayX + overlayWidth, clipTop + shownHeight)
            }

            rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
            graphics.drawBorder(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

            if (matchingValues.size > visibleRows) {
                graphics.drawScrollBar(
                    overlayX + overlayWidth - Common.UI.SCROLLBAR_WIDTH - Common.UI.BORDER_SIZE,
                    overlayY,
                    overlayHeight,
                    matchingValues.size,
                    visibleRows,
                    scroll
                )
            }

            if (stillOpening) graphics.disableScissor()
        }

        override fun charTyped(characterEvent: CharacterEvent): Boolean = isSearchable && searchBox.charTyped(characterEvent)

        override fun keyPressed(keyEvent: KeyEvent): Boolean = isSearchable && searchBox.keyPressed(keyEvent)

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
            rows.toList().forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) {
                    when (mouseButtonEvent.button()) {
                        0 -> pickValue(it.value)
                        1 -> onRightClickValue?.invoke(it.value, mouseButtonEvent)
                    }
                    return true
                }
            }
            return false
        }

        override fun mouseMoved(mouseX: Double, mouseY: Double) {
            hoveredElement = null
            rows.forEach {
                it.mouseMoved(mouseX, mouseY)
                if (hoveredElement == null && it.isMouseOverText(mouseX, mouseY)) {
                    hoveredElement = it
                }
            }
        }
    }

    private companion object {
        const val ARROW_DOWN: String = "↓"
        const val ARROW_UP: String = "↑"
        const val PLACEHOLDER: String = "Select…"

        const val TEXT_PADDING: Int = Common.UI.TEXT_X_PAD + Common.UI.CONTROL_BORDER_SIZE
        const val MIN_WIDTH: Int = 60

        const val OPEN_MS: Long = 150
    }
}
