package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()
    override val hasChildren: Boolean = true
    private val checkbox = CheckboxWidget(checked = setting.value)

    private val name: Component get() = Component.literal(setting.displayName)

    private fun textWidth(): Int = width - checkbox.size - textXPad * 2

    override fun layout() {
        checkbox.size = baseHeight
        height = (wrappedHeight(font, name, textWidth()) + textYPad * 2).coerceAtLeast(baseHeight)
        layoutCheckbox()
    }

    private fun layoutCheckbox() {
        checkbox.x = x
        checkbox.y = y + (height - checkbox.size) / 2
        checkbox.checked = setting.value
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(x, y, x + width, y + height, backgroundColor)

        checkbox.render(graphics)

        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        val textHeight = wrappedHeight(font, name, textWidth())

        graphics.drawWrappedText(
            font,
            name,
            x + checkbox.size + textXPad,
            y + (height - textHeight) / 2,
            textWidth(),
            0xFFFFFFFF.toInt()
        )

        extractChildrenRenderStates(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(mouseButtonEvent, doubled)) {
            setting.value = !setting.value
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }
}
