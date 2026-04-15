package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.ui.ClickableButtonWidget
import org.magic.magicaddons.config.data.ToggleListSetting
import org.magic.magicaddons.config.ui.BaseRowWidget
import org.magic.magicaddons.config.ui.ToggleRowWidget
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawLine

class TextListSettingWidget(
    val listSetting: ToggleListSetting
) : SettingWidget<MutableList<ListEntry>>(listSetting) {


    override var childrenExpanded: Boolean = true
    override var hovered: Boolean = false

    private val titleYPadding = 2
    private val rowHeight = 20
    private val inputYPadding = 2
    private val inputXPadding = 4

    private var addLabelY: Int = 0
    private var rowY: Int = 0

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    private val rows = mutableListOf<ToggleRowWidget<ListEntry>>()
    private val seperatorYs = mutableListOf<Int>()

    private val titleRow = BaseRowWidget(
        listSetting.displayName,
        {listSetting.displayName}
    )

    private val nameInputField = TextFieldWidget(
        MinecraftClient.getInstance().textRenderer,
        100, 20, Text.literal("")
    )

    private val valueInputField = TextFieldWidget(
        MinecraftClient.getInstance().textRenderer,
        150, 20, Text.literal("")
    )

    private val submitButton = ClickableButtonWidget(
        x, y,
        width = 18,
        height = 20,
        message = Text.literal("+").styled { it.withColor(0x00FF00) }
    )


    override fun init() {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        titleRow.y = y + borderSize
        titleRow.x = x + borderSize

        nameInputField.setMaxLength(256)
        valueInputField.setMaxLength(256)

        rows.clear()

        var currentY = y + borderSize + titleRow.height
        rowY = currentY

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
            row.width = width - borderSize
            row.height = rowHeight

            rows.add(row)

            currentY += rowHeight
            seperatorYs.add(currentY)

        }

        seperatorYs.removeLastOrNull()

        // after rows y level label y
        this.addLabelY = currentY

        currentY += textRenderer.fontHeight + inputYPadding * 2
        // add padding on both top and bottom for input Y padding

        val inputWidths = width - (inputXPadding * 4) - 18

        nameInputField.x = x + inputXPadding
        nameInputField.y = currentY
        nameInputField.width = (inputWidths * 0.4).toInt()

        valueInputField.x = x + nameInputField.width + inputXPadding * 2
        valueInputField.y = currentY
        valueInputField.width = inputWidths - nameInputField.width

        submitButton.x = x + nameInputField.width + valueInputField.width + inputXPadding * 3
        submitButton.y = currentY
        currentY += valueInputField.height + inputYPadding
        currentY += borderSize
        height = currentY - y
        super.init()
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        ctx.fill(x, y, x + width, y + height, backgroundColor)

        titleRow.render(ctx)

        ctx.state.drawLine(
            x, rowY, x+width, rowY,
            2,
            borderColor)

        rows.forEach { it.render(ctx) }

        seperatorYs.forEach {
            ctx.state.drawLine(
                x, it, x + width, it,
                1,
                borderColor
            )
        }


        ctx.state.drawLine(
            x, addLabelY, x+width, addLabelY,
            2,
            borderColor)

        ctx.drawText(
            textRenderer,
            Text.literal("Add new entry:"),
            x + textXPad,
            addLabelY + inputYPadding,
            0xFFFFFFFF.toInt(),
            false
        )

        nameInputField.render(ctx, mouseX, mouseY, delta)
        valueInputField.render(ctx, mouseX, mouseY, delta)
        submitButton.render(ctx, mouseX, mouseY, delta)

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        renderTooltip(ctx, mouseX, mouseY)
    }

    private fun toggleEntry(entry: ListEntry) {
        entry.enabled = !entry.enabled
    }

    private fun removeEntry(entry: ListEntry) {
        listSetting.value.remove(entry)
        init()
    }

    private fun addEntry(name: String, value: String){
        if (value.isBlank()) {
            ChatUtils.sendWithPrefix("Value is a required field.")
            return
        }
        if (listSetting.value.map { it.value }.contains(name)) {
            ChatUtils.sendWithPrefix("Cannot add a duplicate value.")
        }
        listSetting.value.add(ListEntry(name, value, true))
        init()
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (submitButton.mouseClicked(click,doubled)){
            addEntry(nameInputField.text, valueInputField.text)
            return true
        }

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
        val tr = MinecraftClient.getInstance().textRenderer

        val rowsHeight = rows.size * rowHeight

        val inputHeight = valueInputField.height

        val addLabelHeight = tr.fontHeight
        val totalPadding = inputYPadding * 3 // 2 widgets so 3 spacing top bottom and middle

        return borderSize +
                titleRow.height +
                rowsHeight +
                totalPadding +
                addLabelHeight +
                inputHeight +
                borderSize
    }
}