package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()
    override val hasChildren: Boolean = true
    private val checkbox = CheckboxWidget(checked = setting.value)

    override fun layout() {
        layoutCheckbox()
    }

    private fun layoutCheckbox() {
        checkbox.x = x
        checkbox.y = y
        checkbox.size = height
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val font = Minecraft.getInstance().font

        graphics.fill(x, y, x + width, y + height, backgroundColor)

        checkbox.checked = setting.value
        checkbox.render(graphics)

        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        graphics.drawString(
            font,
            Component.literal(setting.displayName),
            x + checkbox.size + textXPad,
            y + (height - font.lineHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        renderChildren(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(mouseButtonEvent, doubled)) {
            setting.value = !setting.value
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }
}