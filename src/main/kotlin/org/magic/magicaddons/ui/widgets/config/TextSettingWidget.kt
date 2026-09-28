package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.RemovableRowWidget
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class TextSettingWidget(
    private val setting: TextSetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    private var valueBeforeEditing: String = setting.value

    private val textBox = TextField(0, BOX_HEIGHT).also {
        it.value = setting.value
        it.setResponder { typed ->
            setting.value = typed
            if (historyOverlay.isOpen) historyOverlay.rebuildRows()
        }
    }

    private val historyOverlay = HistoryOverlay()

    override fun belowTextHeight(): Int = BOX_HEIGHT

    override fun layoutControl() {
        textBox.x = belowTextLeft()
        textBox.y = belowTextTop()
        textBox.width = belowTextWidth()
        if (!textBox.isFocused && textBox.value != setting.value) textBox.value = setting.value
        if (historyOverlay.isOpen) historyOverlay.placeRows()
    }

    private fun openHistory() {
        historyOverlay.rebuildRows()
        historyOverlay.isOpen = true
        overlays.addOverlay(historyOverlay)
    }

    private fun closeHistory() {
        historyOverlay.isOpen = false
        overlays.removeOverlay(historyOverlay)
    }

    private fun applyHistoryValue(value: String) {
        val previousValue = setting.value
        setting.value = value
        textBox.value = value
        setting.history.remove(value)
        setting.history.add(previousValue)
        valueBeforeEditing = value
        textBox.isFocused = false
        closeHistory()
    }

    private fun removeHistoryValue(value: String) {
        setting.history.remove(value)
        historyOverlay.rebuildRows()
    }

    override fun renderBelowText(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        textBox.render(graphics)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val wasFocused = textBox.isFocused

        if (textBox.mouseClicked(event, doubled)) {
            openHistory()
            return true
        }

        if (wasFocused && textBox.value != valueBeforeEditing) {
            if (valueBeforeEditing.isNotBlank()) setting.history.add(valueBeforeEditing)
            valueBeforeEditing = setting.value
        }
        return false
    }

    override fun dropFocus() {
        textBox.isFocused = false
        super.dropFocus()
    }

    override fun charTyped(event: CharacterEvent): Boolean = textBox.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean = textBox.keyPressed(event) || super.keyPressed(event)

    inner class HistoryOverlay : OverlayRenderable {

        var isOpen: Boolean = false

        override val renderPriority: Int = OverlayRenderable.DROPDOWN_PRIORITY

        override var hoveredElement: GuiEventListener? = null

        private val rows: MutableList<RemovableRowWidget<String>> = mutableListOf()

        fun rebuildRows() {
            rows.clear()

            val filterText = textBox.value.trim()

            setting.history.filter { it.contains(filterText, ignoreCase = true) }.forEach { value ->
                rows.add(
                    RemovableRowWidget(
                        value = value,
                        onClick = { applyHistoryValue(value) },
                        onRemove = { removeHistoryValue(value) }
                    )
                )
            }
            rows.lastOrNull()?.hasDividerBelow = false
            placeRows()
        }

        fun placeRows() {
            var currentY = textBox.y + textBox.height
            rows.forEach { row ->
                row.x = textBox.x
                row.y = currentY
                row.width = textBox.width
                row.fitHeight(textBox.height)
                currentY += row.height
            }
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
            isOpen = false
        }
    }

    private companion object {
        const val BOX_HEIGHT: Int = 16
    }
}
