package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.ui.widgets.BaseRowWidget
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawLine
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.style

class TextListSettingWidget(
    val listSetting: ToggleListSetting
) : SettingWidget<MutableList<ListEntry>>(listSetting) {

    override val hasChildren: Boolean = false
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

    private val nameInputField = EditBox(
        Minecraft.getInstance().font,
        100, 20, Component.literal("")
    )

    private val valueInputField = EditBox(
        Minecraft.getInstance().font,
        150, 20, Component.literal("")
    )

    private val submitButton = ClickableButtonWidget(
        x, y,
        width = 18,
        height = 20,
        message = Component.literal("+").style { Style.EMPTY.withColor(0x00FF00) }
    )


    override fun layout() {
        val font = Minecraft.getInstance().font
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

        currentY += font.lineHeight + inputYPadding * 2
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
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val font = Minecraft.getInstance().font

        graphics.fill(x, y, x + width, y + height, backgroundColor)

        titleRow.render(graphics)

        graphics.drawLine(
            x, rowY, x+width, rowY,
            2,
            borderColor)

        rows.forEach { it.render(graphics) }

        seperatorYs.forEach {
            graphics.drawLine(
                x, it, x + width, it,
                1,
                borderColor
            )
        }


        graphics.drawLine(
            x, addLabelY, x+width, addLabelY,
            2,
            borderColor)

        graphics.drawString(
            font,
            Component.literal("Add new entry:"),
            x + textXPad,
            addLabelY + inputYPadding,
            0xFFFFFFFF.toInt(),
            false
        )

        nameInputField.render(graphics, mouseX, mouseY, delta)
        valueInputField.render(graphics, mouseX, mouseY, delta)
        submitButton.render(graphics, mouseX, mouseY, delta)

        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

    }

    private fun toggleEntry(entry: ListEntry) {
        entry.enabled = !entry.enabled
    }

    private fun removeEntry(entry: ListEntry) {
        listSetting.value.remove(entry)
        ChatUtils.sendWithPrefix("Removed: ${entry.name} Value:\n${entry.value}")
        initChildren()
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
        initChildren()
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (submitButton.mouseClicked(mouseButtonEvent,doubled)){
            addEntry(nameInputField.value, valueInputField.value)
            return true
        }

        if (nameInputField.mouseClicked(mouseButtonEvent, doubled)) {
            nameInputField.isFocused = true
            valueInputField.isFocused = false
            return true
        }
        if (valueInputField.mouseClicked(mouseButtonEvent, doubled)) {
            valueInputField.isFocused = true
            nameInputField.isFocused = false
            return true
        }

        valueInputField.isFocused = false
        nameInputField.isFocused = false

        rows.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) {
                return true
            }
        }

        return false
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (nameInputField.isFocused) {
            nameInputField.charTyped(characterEvent)
            return true
        }
        if (valueInputField.isFocused){
            valueInputField.charTyped(characterEvent)
            return true
        }
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (nameInputField.isFocused) {
            nameInputField.keyPressed(keyEvent)
            return true
        }
        if (valueInputField.isFocused){
            valueInputField.keyPressed(keyEvent)
            return true
        }
        return super.keyPressed(keyEvent)
    }


    override fun getTotalHeight(): Int {
        val font = Minecraft.getInstance().font

        val rowsHeight = rows.size * rowHeight

        val inputHeight = valueInputField.height

        val addLabelHeight = font.lineHeight
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