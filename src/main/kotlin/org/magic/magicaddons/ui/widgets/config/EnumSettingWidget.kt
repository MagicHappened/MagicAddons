package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.ui.widgets.RemovableRowWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight


class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>
) : SettingWidget<T>(setting) {

    var selectionMenuExpanded = false

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()
    override val hasChildren: Boolean = true

    private val selectionOptions: MutableList<RemovableRowWidget<T>> = mutableListOf()

    private val arrow = "↓"

    /** Each half keeps this much above and below its text, so one line of text fills half the base height. */
    private val halfPad = (baseHeight / 2 - font.lineHeight) / 2

    /** A dropdown row is at least this tall. */
    private val rowMinHeight = baseHeight / 2

    private val title: Component get() = Component.literal("${setting.displayName}:")
    private val valueText: Component get() = Component.literal(setting.value.toString())

    private fun titleWidth(): Int = width - textXPad * 2
    private fun valueWidth(): Int = width - textXPad * 2 - font.width(arrow) - 4

    /** The top half holds the name, the bottom half the value; both grow with their wrapped text. */
    private fun titleHeight(): Int = wrappedHeight(font, title, titleWidth()) + halfPad * 2
    private fun valueHeight(): Int = wrappedHeight(font, valueText, valueWidth()) + halfPad * 2

    override fun initChildren() {
        childrenWidgets.clear()
        setting.childrenProvider?.invoke(setting.value)?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it).apply {
                selectionMenuExpanded = true
            }
            )
        }

    }

    fun initDropdown(){ //call on widget creation (dont need to recreate dropdown option widgets)
        selectionOptions.clear()

        val enumValues = setting.value.javaClass.enumConstants
        enumValues.forEach { enumValue ->
            val dropDown = RemovableRowWidget(
                value = enumValue,
                onClick = { valueChanged(it.value) }
            )
            selectionOptions.add(dropDown)
        }
    }


    override fun layout() { //delegate more into this function in future maybe?
        height = (titleHeight() + valueHeight()).coerceAtLeast(baseHeight)
        initDropdown()
        layoutDropdown() //maybe arrow and text selection too :think:
    }

    private fun layoutDropdown() {
        var currentY = y + height

        selectionOptions.forEach {
            it.x = x
            it.y = currentY
            it.width = width
            it.fitHeight(rowMinHeight)
            currentY += it.height
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val divider = y + titleHeight()

        graphics.fill(x, y, x + width, y + height, backgroundColor)
        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        graphics.drawLine(
            x + borderSize,
            divider,
            x + width - borderSize,
            divider,
            1,
            borderColor
        )

        graphics.drawWrappedText(
            font,
            title,
            x + textXPad,
            y + halfPad,
            titleWidth(),
            0xFFFFFFFF.toInt()
        )

        val valueTop = divider + (height - titleHeight() - valueHeight()).coerceAtLeast(0) / 2

        graphics.drawWrappedText(
            font,
            valueText,
            x + textXPad,
            valueTop + halfPad,
            valueWidth(),
            0xFFFFFFFF.toInt()
        )

        graphics.text(
            font,
            Component.literal(arrow),
            x + width - font.width(arrow) - 4,
            valueTop + halfPad,
            0xFFFFFFFF.toInt(),
            false
        )

        extractChildrenRenderStates(graphics, mouseX, mouseY, delta)

        if (selectionMenuExpanded) {
            selectionOptions.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
        }
    }

    private fun valueChanged(selectedValue: T) {
        val changed = setting.value != selectedValue
        selectionMenuExpanded = false

        if (changed) {
            setting.value = selectedValue

            initChildren()
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
                    layoutDropdown()
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
