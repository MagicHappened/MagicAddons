package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.ui.ClickableRowWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawLine


class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>
) : SettingWidget<T>(setting) {

    var selectionMenuExpanded = false

    private val selectionOptions: MutableList<ClickableRowWidget<T>> = mutableListOf()

    override fun init() {
        selectionOptions.clear()
        childrenWidgets.clear()

        val enumValues = setting.value.javaClass.enumConstants
        enumValues.forEach { enumValue ->
            val dropDown = ClickableRowWidget(
                value = enumValue,
                displayText = { enumValue.toString() },
                onClick = { valueChanged(it.value) }
            )
            selectionOptions.add(dropDown)
        }

        setting.childrenProvider?.invoke(setting.value)?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it))
        } ?: throw IllegalStateException("Enum factory must not be null")

        super.init()
    }

    private fun layoutDropdown() {
        var currentY = y + height

        selectionOptions.forEach {
            it.x = x
            it.y = currentY
            it.width = width
            it.height = height / 2
            currentY += it.height
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val font = Minecraft.getInstance().font
        val halfHeight = height / 2

        val titleY = y + (halfHeight - font.lineHeight) / 2
        val valueY = y + halfHeight + (halfHeight - font.lineHeight) / 2

        graphics.fill(x, y, x + width, y + height, backgroundColor)
        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        graphics.drawLine(
            x + borderSize,
            y + halfHeight,
            x + width - borderSize,
            y + halfHeight,
            1,
            borderColor
        )

        graphics.drawString(
            font,
            Component.literal("${setting.displayName}:"),
            x + textXPad,
            titleY,
            0xFFFFFFFF.toInt(),
            false
        )

        graphics.drawString(
            font,
            Component.literal(setting.value.toString()),
            x + textXPad,
            valueY,
            0xFFFFFFFF.toInt(),
            false
        )

        graphics.drawString(
            font,
            Component.literal("↓"),
            x + width - font.width("↓") - 4,
            valueY,
            0xFFFFFFFF.toInt(),
            false
        )

        renderChildren(graphics, mouseX, mouseY, delta)

        if (selectionMenuExpanded) {
            layoutDropdown()
            selectionOptions.forEach { it.render(graphics) }
        }
    }

    private fun valueChanged(selectedValue: T) {
        val changed = setting.value != selectedValue
        selectionMenuExpanded = false

        if (changed) {
            setting.value = selectedValue
            // rebuild children
            childrenWidgets.clear()
            setting.children?.forEach {
                childrenWidgets.add(SettingWidgetFactory.create(it))
            }

            init()
            childrenExpanded = true
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (selectionMenuExpanded) {
            selectionOptions.forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) return true
            }

        }

        val inside = isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

        if (inside) {
            when (mouseButtonEvent.button()) {
                1 -> { // right click for children
                    childrenExpanded = !childrenExpanded
                    selectionMenuExpanded = false
                    return true
                }

                0 -> { // left click for dropdown
                    selectionMenuExpanded = !selectionMenuExpanded
                    return true
                }
            }
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun getTotalHeight(): Int {
        var total = height

        if (childrenExpanded) {
            total += childrenWidgets.sumOf { it.getTotalHeight() + childPadding }
        }
        return total
    }
}