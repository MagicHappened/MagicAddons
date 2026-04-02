package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.ui.DropDownBoxWidget
import org.magic.magicaddons.util.ScreenUtil

class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>
) : SettingWidget<T>(setting) {

    override var height: Int = 80

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    var selectionMenuExpanded = false
    val selectionOptions: MutableList<DropDownBoxWidget> = mutableListOf()

    override fun init() {
        val enumValues = setting.value.javaClass.enumConstants
        enumValues.forEachIndexed { index, enumValue ->
            val dropDownBoxWidget = DropDownBoxWidget()
            dropDownBoxWidget.x = x
            dropDownBoxWidget.y = y + height + (index * (height / 2))
            dropDownBoxWidget.widgetText = enumValue.name
            selectionOptions.add(dropDownBoxWidget)
        }

        setting.children?.forEach { child ->
            childrenWidgets.add(SettingWidgetFactory.create(child))
        }
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderChildrenIfExpanded(ctx, mouseX, mouseY, delta)
        ctx.fill(x, y, x + width, y + height, backgroundColor)
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val halfHeight = height / 2

        val titleY = y + (halfHeight - textRenderer.fontHeight) / 2
        val valueY = y + halfHeight + (halfHeight - textRenderer.fontHeight) / 2

        // draw divider line
        ScreenUtil.drawLine(ctx, x + borderSize, y + halfHeight, x + width - borderSize, y + halfHeight, 1, borderColor)

        // draw option title
        ctx.drawText(
            textRenderer,
            Text.literal("${setting.displayName}:"),
            x + textXPad,
            titleY,
            0xFFFFFFFF.toInt(),
            false
        )

        // draw actual setting value
        ctx.drawText(
            textRenderer,
            Text.literal("${setting.value}: ${setting.value}"),
            x + textXPad,
            valueY,
            0xFFFFFFFF.toInt(),
            false
        )

        // draw arrow indicating drop down
        ctx.drawText(
            textRenderer,
            Text.literal("↓"),
            x + width - textRenderer.getWidth("↓") - 4,
            valueY,
            0xFFFFFFFF.toInt(),
            false
        )

        if (selectionMenuExpanded) {
            selectionOptions.forEach {
                it.render(ctx)
            }
        }
    }


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val clickX: Int = click.x.toInt()
        val clickY: Int = click.y.toInt()
        if (clickX !in x..x + width) {
            if (selectionMenuExpanded) {
                selectionMenuExpanded = false
            }
            return false
        }
        val titleEndYHeight = y+height/2
        val selectorEndYHeight = y+height
        if (clickY in y..titleEndYHeight) { // top for children expanding
            childrenExpanded = !childrenExpanded
            selectionMenuExpanded = false
            return true
        }
        if (clickY in titleEndYHeight..selectorEndYHeight) { // button for dropdown overlay
            selectionMenuExpanded = !selectionMenuExpanded
            return true
        }
        if (selectionMenuExpanded) {
            selectionOptions.forEach {
                if (it.mouseClicked(click, doubled)) return true
            }
        }
        childrenWidgets.forEach {
            if (it.mouseClicked(click, doubled)) return true
        }
        return false

    }

    override fun getActualHeight(): Int {
        if (!childrenExpanded) return height
        return height + childrenWidgets.sumOf { it.height + childPadding }
    }


}