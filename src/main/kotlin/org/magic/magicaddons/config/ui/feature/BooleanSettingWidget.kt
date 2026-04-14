package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.ui.CheckboxWidget
import org.magic.magicaddons.util.ScreenUtil

class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {

    private val checkbox = CheckboxWidget(checked = setting.value)

    override fun init() {
        childrenWidgets.clear()

        setting.children?.forEach {
            childrenWidgets.add(SettingWidgetFactory.create(it))
        }
        layoutCheckbox()

        super.init()
    }

    private fun layoutCheckbox() {
        checkbox.x = x
        checkbox.y = y
        checkbox.size = height
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        ctx.fill(x, y, x + width, y + height, backgroundColor)

        checkbox.checked = setting.value
        checkbox.render(ctx)

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        ctx.drawText(
            textRenderer,
            Text.literal(setting.displayName),
            x + checkbox.size + textXPad,
            y + (height - textRenderer.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        renderChildren(ctx, mouseX, mouseY, delta)

        renderTooltip(ctx, mouseX, mouseY)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(click, doubled)) {
            setting.value = !setting.value
            return true
        }

        val inside = isMouseOver(click.x, click.y)

        if (inside && click.button() == 1) {
            childrenExpanded = !childrenExpanded
            return true
        }

        return super.mouseClicked(click, doubled)
    }
}