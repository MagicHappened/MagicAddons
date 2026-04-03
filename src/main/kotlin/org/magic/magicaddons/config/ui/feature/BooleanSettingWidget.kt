package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.ui.CheckboxWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {

    private val checkbox = CheckboxWidget(checked = setting.value)
    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()
    override var childrenExpanded: Boolean = false

    override fun init() {

        checkbox.x = x
        checkbox.y = y
        checkbox.size = height

        setting.children?.forEach {
            val widget = SettingWidgetFactory.create(it)
            childrenWidgets.add(widget)
        }

        super.init()
    }


    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ctx.fill(x, y, x + width, y + height, backgroundColor)
        checkbox.render(ctx)
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        renderChildrenIfExpanded(ctx, mouseX, mouseY, delta)


        val textRenderer = MinecraftClient.getInstance().textRenderer

        ctx.drawText(
            textRenderer,
            Text.literal(setting.displayName),
            x + checkbox.size + textXPad,
            y + (height - textRenderer.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        renderChildrenIfExpanded(ctx, mouseX, mouseY, delta)
    }

    fun renderChildrenIfExpanded(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float){
        if (childrenExpanded) {
            childrenWidgets.forEach {
                it.render(ctx, mouseX, mouseY, delta)
            }
        }
    }


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (childrenExpanded){
            childrenWidgets.forEach {
                if (it.mouseClicked(click, doubled)) return true
            }
        }
        if (!super.mouseClicked(click, doubled)) return false
        if (checkbox.mouseClicked(click, doubled)) {
            setting.value = !setting.value
            return true
        }
        if (click.button() == 1){
            childrenExpanded = !childrenExpanded
        }


        return false
    }

    override fun getActualHeight(): Int {
        if (!childrenExpanded) return height
        return height + childrenWidgets.sumOf { it.height + childPadding }
    }


}