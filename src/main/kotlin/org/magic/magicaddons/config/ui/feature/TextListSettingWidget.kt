package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.ClickableButtonWidget
import org.magic.magicaddons.config.data.ToggleListSetting
import org.magic.magicaddons.config.ui.ToggleRowWidget
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.util.ScreenUtil

class TextListSettingWidget(
    val listSetting: ToggleListSetting
) : SettingWidget<MutableList<ListEntry>>(listSetting) {


    override var childrenExpanded: Boolean = true
    override var hovered: Boolean = false

    private val rowHeight = 20
    private val inputPadding = 4

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    private val rows = mutableListOf<ToggleRowWidget<ListEntry>>()

    private val nameInputField = TextFieldWidget(
        MinecraftClient.getInstance().textRenderer,
        100, 20, Text.literal("")
    )

    private val valueInputField = TextFieldWidget(
        MinecraftClient.getInstance().textRenderer,
        150, 20, Text.literal("")
    )
    private val submitButton = ClickableButtonWidget(
        x,
        y,
        18,
        20,
        Text.literal("+").styled { it.withColor(0x00FF00) },
    ) { addEntry(nameInputField.text, valueInputField.text) }

    override fun init() {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        rows.clear()

        var currentY = y

        listSetting.value.forEach { entry ->
            val row = ToggleRowWidget(
                value = entry,
                displayText = { "${entry.name}: ${entry.value}" },
                onRemove = { removeEntry(it.value) },
                onClick = { toggleEntry(it.value) },
                isEnabled = { entry.enabled },
                onToggle = { entry.enabled = !entry.enabled }
            )

            row.x = x
            row.y = currentY
            row.width = width
            row.height = rowHeight

            rows.add(row)
            currentY += rowHeight
        }

        val inputWidths = width - (inputPadding * 4) - 18

        val textHeight = + textRenderer.fontHeight + inputPadding * 2
        currentY += textHeight

        nameInputField.x = x + inputPadding
        nameInputField.y = currentY
        nameInputField.width = (inputWidths*0.4).toInt()

        valueInputField.x = x + nameInputField.width + inputPadding * 2
        valueInputField.y = currentY
        valueInputField.width = inputWidths - nameInputField.width

        submitButton.x = x + nameInputField.width + valueInputField.width + inputPadding * 3
        submitButton.y = currentY
        super.init()
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        ctx.fill(x, y, x + width, y + getTotalHeight(), backgroundColor)
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + getTotalHeight(), borderSize, borderColor)

        var currentY = y

        rows.forEach {
            it.y = currentY
            it.render(ctx)
            currentY += it.height

            ScreenUtil.drawLine(ctx, x, currentY, x + width, currentY, 1, 0xFF222222.toInt())
        }

        currentY += inputPadding

        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("Add new entry:"),
            x + textXPad,
            currentY,
            0xFFFFFFFF.toInt(),
            false
        )

        nameInputField.render(ctx, mouseX, mouseY, delta)
        valueInputField.render(ctx, mouseX, mouseY, delta)
        submitButton.render(ctx, mouseX, mouseY, delta)
    }

    private fun toggleEntry(entry: ListEntry) {
        entry.enabled = !entry.enabled
    }

    private fun removeEntry(entry: ListEntry) {
        listSetting.value.remove(entry)
        init()
    }

    private fun addEntry(name: String, value: String){
        listSetting.value.add(ListEntry(name, value, true))
        init()
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {

        if (nameInputField.mouseClicked(click, doubled)) {
            nameInputField.isFocused = true
            valueInputField.isFocused = false
            return true
        }
        if (valueInputField.mouseClicked(click, doubled)) {
            valueInputField.isFocused = true
            nameInputField.isFocused = false
            return true
        }

        valueInputField.isFocused = false
        nameInputField.isFocused = false

        rows.forEach {
            if (it.mouseClicked(click, doubled)) {
                return true
            }
        }

        return false
    }

    override fun charTyped(input: CharInput): Boolean {
        if (nameInputField.isFocused) {
            nameInputField.charTyped(input)
            return true
        }
        if (valueInputField.isFocused){
            valueInputField.charTyped(input)
            return true
        }
        return super.charTyped(input)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (nameInputField.isFocused) {
            nameInputField.keyPressed(input)
            return true
        }
        if (valueInputField.isFocused){
            valueInputField.keyPressed(input)
            return true
        }
        return super.keyPressed(input)
    }


    override fun getTotalHeight(): Int {
        return height + rowHeight * rows.size
    }
}