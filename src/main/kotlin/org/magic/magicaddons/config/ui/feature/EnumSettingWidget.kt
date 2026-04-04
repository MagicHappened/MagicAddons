package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.ui.DropDownBoxWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>
) : SettingWidget<T>(setting) {

    override var height: Int = 80

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    override var childrenExpanded: Boolean = false
    var selectionMenuExpanded = false

    val selectionOptions: MutableList<DropDownBoxWidget<T>> = mutableListOf()

    override fun init() {

        val enumValues = setting.value.javaClass.enumConstants
        enumValues.forEachIndexed { index, enumValue ->

            val dropDown = DropDownBoxWidget(enumValue) { selectedValue ->
                val valueChanged = setting.value != selectedValue
                setting.value = selectedValue
                selectionMenuExpanded = false
                if (valueChanged) {
                    childrenExpanded = false
                    childrenWidgets.clear()
                    setting.children?.forEach {
                        childrenWidgets.add(SettingWidgetFactory.create(it))
                    }
                    super.init()
                }

            }

            dropDown.x = x
            dropDown.y = y + height + (index * (height / 2))
            dropDown.width = width
            dropDown.height = height / 2

            selectionOptions.add(dropDown)
        }

        setting.children?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it))
        }

        super.init()
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderChildrenIfExpanded(ctx, mouseX, mouseY, delta)
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
            Text.literal("${setting.value}"),
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
        if (childrenExpanded){
            childrenWidgets.forEach {
                it.render(ctx, mouseX, mouseY, delta)
            }
        }

        if (selectionMenuExpanded) {
            selectionOptions.forEach {
                it.render(ctx)
            }
        }
    }

    fun renderChildrenIfExpanded(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float){
        if (childrenExpanded) {
            childrenWidgets.forEach {
                it.render(ctx, mouseX, mouseY, delta)
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
        if (clickY in y..y+height && click.button() == 1) { // top for children expanding
            childrenExpanded = !childrenExpanded
            selectionMenuExpanded = false
            return true
        }
        if (clickY in y..y+height && click.button() == 0) { // button for dropdown overlay
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