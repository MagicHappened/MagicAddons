package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {

    override val hasChildren: Boolean = false
    override var childrenExpanded: Boolean = false
    override var hovered: Boolean = false

    private var shouldRenderHistory = false
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

    private val historyWidgets: MutableList<ClickableRowWidget<String>> = mutableListOf()


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
    }

    private fun rebuildHistory() {
        historyWidgets.clear()

        var currentY = textWidget.y + textWidget.height

        setting.history.forEach { value ->
            val widget = ClickableRowWidget(
                value = value,
                displayText = { value },
                onClick = { applyHistoryValue(value) },
                onRemove = { removeHistoryValue(value) }
            )

            widget.x = textWidget.x
            widget.y = currentY
            widget.width = textWidget.width
            widget.height = textWidget.height

            currentY += widget.height

            historyWidgets.add(widget)
        }
    }

    private fun applyHistoryValue(value: String) {
        val previousValue = setting.value
        setting.value = value
        textWidget.value = value
        removeHistoryValue(value)
        setting.history.add(previousValue)
        shouldRenderHistory = false
        textWidget.isFocused = false
    }

    private fun removeHistoryValue(value: String) {
        setting.history.remove(value)
        historyWidgets.removeIf { it.value == value }
        rebuildHistory()
        shouldRenderHistory = true
    }


    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(x, y, x + width, y + height, backgroundColor)


        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        textWidget.render(graphics, mouseX, mouseY, delta)

        graphics.drawString(
            Minecraft.getInstance().font,
            Component.literal("${setting.displayName}: "),
            x + textXPad + borderSize,
            y + textXPad + borderSize,
            0xFFCCCCCC.toInt(),
            false
        )

        if (shouldRenderHistory) {
            historyWidgets.forEach {
                it.render(graphics)
            }
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {

        val clickedText = textWidget.mouseClicked(mouseButtonEvent, doubled)

        if (clickedText) {
            shouldRenderHistory = true
            textWidget.isFocused = true
            rebuildHistory()
            return true
        }

        if (shouldRenderHistory) {
            historyWidgets.forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) {
                    return true
                }
            }
        }

        val wasFocused = textWidget.isFocused
        textWidget.isFocused = false
        shouldRenderHistory = false
        val childClicked = super.mouseClicked(mouseButtonEvent, doubled)

        if (wasFocused && textWidget.value != lastFocusedValue) {
            if (lastFocusedValue.isNotBlank()){
                setting.history.add(lastFocusedValue)
                lastFocusedValue = setting.value
            }
        }

        return childClicked
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

    override fun getTotalHeight(): Int = height
}