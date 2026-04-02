package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.ui.CheckboxWidget
import org.magic.magicaddons.config.ui.feature.SettingWidget
import org.magic.magicaddons.util.ScreenUtil

class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {

    private val borderSize = 2
    private val borderColor: Int = 0xFF000000.toInt()
    private val textXPad: Int = 10

    private val checkbox = CheckboxWidget(checked = setting.value)
    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    override fun init() {
        updateCheckboxLayout()
        setting.children?.forEach { child ->
            childrenWidgets.add(SettingWidgetFactory.create(child))
        }
    }

    private fun updateCheckboxLayout() {
        checkbox.x = x
        checkbox.y = y
        checkbox.width = height
        checkbox.height = height
    }

    private fun getBackgroundColor(): Int {
        return if (setting.value) 0xFF00FF00.toInt() else 0xFF555555.toInt()
    }

    private fun getTextY(): Int {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        return y + (height - textRenderer.fontHeight) / 2
    }

    private fun getTextX(): Int {
        return x + checkbox.width + textXPad
    }


    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        ctx.fill(x, y, x + width, y + height, getBackgroundColor())

        checkbox.render(ctx, mouseX, mouseY, delta)

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        val textRenderer = MinecraftClient.getInstance().textRenderer
        ctx.drawText(
            textRenderer,
            Text.literal(setting.displayName),
            getTextX(),
            getTextY(),
            0xFFFFFFFF.toInt(),
            false
        )
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        // todo change so its only in checkbox thats toggling, and same with parent on right click expand
        val inside = click.x in x.toDouble()..(x + width).toDouble() &&
                click.y in y.toDouble()..(y + height).toDouble()

        if (inside) {
            setting.value = !setting.value
            checkbox.checked = setting.value // keep UI in sync
            return true
        }

        // TODO: children handling
        return false
    }
}