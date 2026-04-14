package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
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

    override fun init() {
        rows.clear()

        var currentY = y

        listSetting.value.forEach { entry ->
            val row = ToggleRowWidget(
                value = entry,
                displayText = { "${entry.name}: ${entry.value}" },
                onRemove = { removeEntry(it.value) },
                onClick = { toggleEntry(it.value) },
                isEnabled = { entry.enabled }
            )

            row.x = x
            row.y = currentY
            row.width = width
            row.height = rowHeight

            rows.add(row)
            currentY += rowHeight
        }

        val inputWidths = width - (inputPadding * 3)


        nameInputField.x = x + inputPadding
        nameInputField.y = currentY + 10
        nameInputField.width = (inputWidths*0.6).toInt()

        valueInputField.x = x + nameInputField.width + inputPadding
        valueInputField.y = currentY + 10
        valueInputField.width = inputWidths - nameInputField.width

        super.init()
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        ctx.fill(x, y, x + width, y + height, backgroundColor)
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        var currentY = y

        rows.forEach {
            it.y = currentY
            it.render(ctx)
            currentY += it.height

            ScreenUtil.drawLine(ctx, x, currentY, x + width, currentY, 1, 0xFF222222.toInt())
        }

        currentY += 5

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
    }

    private fun toggleEntry(entry: ListEntry) {
        entry.enabled = !entry.enabled
    }

    private fun removeEntry(entry: ListEntry) {
        listSetting.value.remove(entry)
        init()
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {

        if (nameInputField.mouseClicked(click, doubled)) return true
        if (valueInputField.mouseClicked(click, doubled)) return true

        rows.forEach {
            if (it.mouseClicked(click, doubled)) {
                listSetting.value = listSetting.value // persist
                return true
            }
        }

        return false
    }

    override fun getTotalHeight(): Int = height //todo change this to actual height based on rows etc.
}