package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.ui.ClickableRowWidget
import org.magic.magicaddons.util.ScreenUtil


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


        setting.children?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it))
        }

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

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val halfHeight = height / 2

        val titleY = y + (halfHeight - textRenderer.fontHeight) / 2
        val valueY = y + halfHeight + (halfHeight - textRenderer.fontHeight) / 2

        ctx.fill(x, y, x + width, y + height, backgroundColor)
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        ScreenUtil.drawLine(
            ctx,
            x + borderSize,
            y + halfHeight,
            x + width - borderSize,
            y + halfHeight,
            1,
            borderColor
        )

        ctx.drawText(
            textRenderer,
            Text.literal("${setting.displayName}:"),
            x + textXPad,
            titleY,
            0xFFFFFFFF.toInt(),
            false
        )

        ctx.drawText(
            textRenderer,
            Text.literal(setting.value.toString()),
            x + textXPad,
            valueY,
            0xFFFFFFFF.toInt(),
            false
        )

        ctx.drawText(
            textRenderer,
            Text.literal("↓"),
            x + width - textRenderer.getWidth("↓") - 4,
            valueY,
            0xFFFFFFFF.toInt(),
            false
        )

        renderChildren(ctx, mouseX, mouseY, delta)

        if (selectionMenuExpanded) {
            layoutDropdown()
            selectionOptions.forEach { it.render(ctx) }
        }



        renderTooltip(ctx, mouseX, mouseY)
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

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (selectionMenuExpanded) {
            selectionOptions.forEach {
                if (it.mouseClicked(click, doubled)) return true
            }
        }

        val inside = isMouseOver(click.x, click.y)

        if (inside) {
            when (click.button()) {
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

        return super.mouseClicked(click, doubled)
    }

    override fun getTotalHeight(): Int {
        var total = height

        if (childrenExpanded) {
            total += childrenWidgets.sumOf { it.getTotalHeight() + childPadding }
        }
        return total
    }
}