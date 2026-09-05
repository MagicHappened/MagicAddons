package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.RemovableRowWidget
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.util.ScreenUtil.drawBorder

/**
 * A text box under the description, with its history dropping down under it. The history is an
 * overlay, so it draws over whatever sits below and takes clicks before it.
 */
class TextSettingWidget(
    private val setting: TextSetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    override val controlWidth: Int = 0
    override val controlHeight: Int = 0

    private var lastFocusedValue: String = setting.value

    private val textBox = TextField(0, BOX_HEIGHT).also {
        it.setMaxLength(256)
        it.value = setting.value
        it.setResponder { typed ->
            setting.value = typed
            // what is typed doubles as the search through the old values
            if (history.open) history.rebuild()
        }
    }

    private val history = HistoryOverlay()

    override fun extraHeight(): Int = BOX_HEIGHT

    override fun layoutControl() {
        textBox.x = extraLeft()
        textBox.y = extraTop()
        textBox.width = extraWidth()
        if (!textBox.focused) textBox.value = setting.value
        if (history.open) history.rebuild()
    }

    private fun openHistory() {
        history.rebuild()
        history.open = true
        overlays.addOverlay(history)
    }

    private fun closeHistory() {
        history.open = false
        overlays.removeOverlay(history)
    }

    private fun applyHistoryValue(value: String) {
        val previousValue = setting.value
        setting.value = value
        textBox.value = value
        setting.history.remove(value)
        setting.history.add(previousValue)
        textBox.focused = false
        closeHistory()
    }

    private fun removeHistoryValue(value: String) {
        setting.history.remove(value)
        history.rebuild()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun renderExtra(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        textBox.render(graphics)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val wasFocused = textBox.focused

        if (textBox.mouseClicked(event, doubled)) {
            openHistory()
            return true
        }

        // any other click: the screen has already closed the history, this only settles the text
        if (wasFocused && textBox.value != lastFocusedValue) {
            if (lastFocusedValue.isNotBlank()) setting.history.add(lastFocusedValue)
            lastFocusedValue = setting.value
        }
        return false
    }

    override fun dropFocus() {
        textBox.focused = false
        super.dropFocus()
    }

    override fun charTyped(event: CharacterEvent): Boolean = textBox.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean = textBox.keyPressed(event) || super.keyPressed(event)

    /** The previous values, dropped down under the box as rows that apply or remove themselves. */
    inner class HistoryOverlay : OverlayRenderable, Focusable {

        var open: Boolean = false

        override var focusedState: Boolean = false

        override val renderPriority: Int = 1

        override var hoveredElement: GuiEventListener? = null

        private val rows: MutableList<RemovableRowWidget<String>> = mutableListOf()

        fun rebuild() {
            rows.clear()

            var currentY = textBox.y + textBox.height
            val typed = textBox.value.trim()

            setting.history.filter { it.contains(typed, ignoreCase = true) }.forEach { value ->
                val row = RemovableRowWidget(
                    value = value,
                    onClick = { applyHistoryValue(value) },
                    onRemove = { removeHistoryValue(value) }
                )
                row.x = textBox.x
                row.y = currentY
                row.width = textBox.width
                row.fitHeight(textBox.height)

                currentY += row.height
                rows.add(row)
            }
            rows.lastOrNull()?.dividerBelow = false
        }

        override val overlayX: Int get() = textBox.x
        override val overlayY: Int get() = textBox.y + textBox.height
        override val overlayWidth: Int get() = textBox.width
        override val overlayHeight: Int get() = rows.sumOf { row -> row.height }

        override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
            if (rows.isEmpty()) return
            rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
            graphics.drawBorder(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)
        }

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
            rows.toList().any { it.mouseClicked(mouseButtonEvent, doubled) }

        override fun mouseMoved(mouseX: Double, mouseY: Double) {
            rows.forEach { it.mouseMoved(mouseX, mouseY) }
        }

        override fun onClosed() {
            open = false
        }
    }

    private companion object {
        const val BOX_HEIGHT: Int = 16
    }
}
