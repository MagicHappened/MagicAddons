package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.RemovableRowWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.compat.McCompat

/**
 * A text box with its history dropping down under it. The history is an overlay, so it draws over
 * whatever setting sits below and takes clicks before them.
 */
class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {

    override val hasChildren: Boolean = false
    override var childrenExpanded: Boolean = false
    override var hovered: Boolean = false

    private var lastFocusedValue: String = setting.value

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    val textFieldPadding: Int = 1

    private val textWidget by lazy {
        EditBox(
            Minecraft.getInstance().font,
            width - (borderSize + textFieldPadding) * 2,
            20,
            Component.literal("")
        )
    }

    private val history = HistoryOverlay()

    private fun overlayContext(): OverlayContext? = McCompat.currentScreen() as? OverlayContext

    override fun layout() {
        val font = Minecraft.getInstance().font

        textWidget.x = x + borderSize + textFieldPadding
        textWidget.y = y + borderSize + textFieldPadding + font.lineHeight + textXPad * 2
        textWidget.height = height - (borderSize + textFieldPadding + textXPad) * 2 - font.lineHeight
        textWidget.setMaxLength(256)

        textWidget.value = setting.value

        textWidget.setResponder {
            setting.value = it
        }

        if (history.open) history.rebuild()
    }

    private fun openHistory() {
        history.rebuild()
        history.open = true
        overlayContext()?.addOverlay(history)
    }

    private fun closeHistory() {
        history.open = false
        overlayContext()?.removeOverlay(history)
    }

    private fun applyHistoryValue(value: String) {
        val previousValue = setting.value
        setting.value = value
        textWidget.value = value
        setting.history.remove(value)
        setting.history.add(previousValue)
        textWidget.isFocused = false
        closeHistory()
    }

    private fun removeHistoryValue(value: String) {
        setting.history.remove(value)
        history.rebuild()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(x, y, x + width, y + height, backgroundColor)
        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        textWidget.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.text(
            Minecraft.getInstance().font,
            Component.literal("${setting.displayName}: "),
            x + textXPad + borderSize,
            y + textXPad + borderSize,
            0xFFCCCCCC.toInt(),
            false
        )

        renderDetail(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (textWidget.mouseClicked(mouseButtonEvent, doubled)) {
            textWidget.isFocused = true
            openHistory()
            return true
        }

        // any other click: the screen has already closed the history, this only settles the text
        val wasFocused = textWidget.isFocused
        textWidget.isFocused = false

        if (wasFocused && textWidget.value != lastFocusedValue) {
            if (lastFocusedValue.isNotBlank()) {
                setting.history.add(lastFocusedValue)
            }
            lastFocusedValue = setting.value
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (textWidget.isFocused) {
            return textWidget.charTyped(characterEvent)
        }
        return false
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (textWidget.isFocused) {
            return textWidget.keyPressed(keyEvent)
        }
        return false
    }

    override fun getTotalHeight(): Int = height + detailHeight()

    /** The previous values, dropped down under the box as rows that apply or remove themselves. */
    inner class HistoryOverlay : OverlayRenderable, Focusable {

        var open: Boolean = false

        override var focusedState: Boolean = false

        override val renderPriority: Int = 1

        override var hoveredElement: GuiEventListener? = null

        private val rows: MutableList<RemovableRowWidget<String>> = mutableListOf()

        fun rebuild() {
            rows.clear()

            var currentY = textWidget.y + textWidget.height

            setting.history.forEach { value ->
                val row = RemovableRowWidget(
                    value = value,
                    onClick = { applyHistoryValue(value) },
                    onRemove = { removeHistoryValue(value) }
                )

                row.x = textWidget.x
                row.y = currentY
                row.width = textWidget.width
                row.height = textWidget.height

                currentY += row.height
                rows.add(row)
            }
        }

        override val overlayX: Int get() = textWidget.x
        override val overlayY: Int get() = textWidget.y + textWidget.height
        override val overlayWidth: Int get() = textWidget.width
        override val overlayHeight: Int get() = rows.sumOf { it.height }

        override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
            rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
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
}
