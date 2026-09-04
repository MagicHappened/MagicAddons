package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.ClickableRowWidget
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.EnumSetting
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

    private val selectionOptions: MutableList<ClickableRowWidget<T>> = mutableListOf()

    /** Typing here narrows the dropdown to the values containing the text. */
    private val search = TextField(0, 0, Component.literal(AbstractSelectorContextMenu.SEARCH_HINT)).apply {
        setResponder { initDropdown(); layoutDropdown() }
    }


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

    fun initDropdown() {
        selectionOptions.clear()

        val typed = search.value.trim()
        setting.value.javaClass.enumConstants
            .filter { it.toString().contains(typed, ignoreCase = true) }
            .forEach { enumValue ->
                selectionOptions.add(
                    ClickableRowWidget(value = enumValue, onClick = { valueChanged(it.value) })
                        .apply { selected = enumValue == setting.value }
                )
            }
        selectionOptions.lastOrNull()?.dividerBelow = false
    }


    override fun layout() { //delegate more into this function in future maybe?
        height = (titleHeight() + valueHeight()).coerceAtLeast(baseHeight)
        initDropdown()
        layoutDropdown() //maybe arrow and text selection too :think:
    }

    private fun layoutDropdown() {
        // below the live line, when there is one: the dropdown opens under the whole row rather
        // than over the note explaining it
        search.x = x
        search.y = y + height + detailHeight()
        search.width = width
        search.height = rowMinHeight

        var currentY = search.y + rowMinHeight

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
            Common.UI.TEXT_COLOR
        )

        val valueTop = divider + (height - titleHeight() - valueHeight()).coerceAtLeast(0) / 2

        graphics.drawWrappedText(
            font,
            valueText,
            x + textXPad,
            valueTop + halfPad,
            valueWidth(),
            Common.UI.TEXT_COLOR
        )

        graphics.text(
            font,
            Component.literal(arrow),
            x + width - font.width(arrow) - 4,
            valueTop + halfPad,
            Common.UI.TEXT_COLOR,
            false
        )

        renderDetail(graphics)

        extractChildrenRenderStates(graphics, mouseX, mouseY, delta)

        if (selectionMenuExpanded) {
            search.render(graphics)
            selectionOptions.forEach { it.extractRenderState(graphics, mouseX, mouseY) }

            val bottom = selectionOptions.lastOrNull()?.let { it.y + it.height } ?: (search.y + search.height)
            graphics.drawBorder(x, search.y, x + width, bottom, borderSize, borderColor)
        }
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean =
        (selectionMenuExpanded && search.charTyped(characterEvent)) || super.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean =
        (selectionMenuExpanded && search.keyPressed(keyEvent)) || super.keyPressed(keyEvent)

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
            if (search.mouseClicked(mouseButtonEvent, doubled)) return true

            selectionOptions.toList().forEach {
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
                    if (selectionMenuExpanded) {
                        search.value = ""
                        search.focused = true
                        initDropdown()
                    }
                    layoutDropdown()
                    return true
                }
            }
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun getTotalHeight(): Int {
        var total = height + detailHeight()

        if (childrenExpanded) {
            total += childrenWidgets.sumOf { it.getTotalHeight() + childPadding }
        }
        return total
    }
}
