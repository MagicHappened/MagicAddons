package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.config.ui.ClickableRowWidget
import org.magic.magicaddons.util.ScreenUtil

class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {

    override var childrenExpanded: Boolean = false
    override var hovered: Boolean = false

    private var shouldRenderHistory = false
    private var lastFocusedValue: String = setting.value

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    val textFieldPadding: Int = 1

    private val textWidget by lazy {
        TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            width - (borderSize + textFieldPadding) * 2,
            20,
            Text.literal("")
        )
    }

    private val historyWidgets: MutableList<ClickableRowWidget<String>> = mutableListOf()

    override fun init() {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        textWidget.x = x + borderSize + textFieldPadding
        textWidget.y = y + borderSize + textFieldPadding + textRenderer.fontHeight + textXPad * 2
        textWidget.height = height - (borderSize + textFieldPadding + textXPad) * 2 - textRenderer.fontHeight
        textWidget.setMaxLength(256)

        textWidget.text = setting.value

        textWidget.setChangedListener {
            setting.value = it
        }

        super.init()
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
        setting.value = value
        textWidget.text = value
        removeHistoryValue(value)
        shouldRenderHistory = false
        textWidget.isFocused = false
    }

    private fun removeHistoryValue(value: String) {
        setting.history.remove(value)
        historyWidgets.removeIf { it.value == value }
    }


    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ctx.fill(x, y, x + width, y + height, backgroundColor)

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        textWidget.render(ctx, mouseX, mouseY, delta)

        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("${setting.displayName}: "),
            x + textXPad + borderSize,
            y + textXPad + borderSize,
            0xFFCCCCCC.toInt(),
            false
        )

        if (shouldRenderHistory) {
            historyWidgets.forEach {
                it.render(ctx)
            }
        }

        renderTooltip(ctx, mouseX, mouseY)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {

        val clickedText = textWidget.mouseClicked(click, doubled)

        if (clickedText) {
            shouldRenderHistory = true
            rebuildHistory()
            return true
        }


        if (shouldRenderHistory) {
            historyWidgets.forEach {
                if (it.mouseClicked(click, doubled)) {
                    shouldRenderHistory = false
                    return true
                }
            }
        }

        val wasFocused = textWidget.isFocused

        val result = super.mouseClicked(click, doubled)

        if (wasFocused && !textWidget.isFocused) {
            commitHistory()
        }

        return result
    }

    private fun commitHistory() {
        if (lastFocusedValue.isNotBlank()) {
            setting.history.add(lastFocusedValue)
        }
        lastFocusedValue = setting.value
    }

    override fun charTyped(input: CharInput): Boolean {
        if (textWidget.isFocused) {
            return textWidget.charTyped(input)
        }
        return false
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (textWidget.isFocused) {
            return textWidget.keyPressed(input)
        }
        return false
    }

    override fun getTotalHeight(): Int = height
}