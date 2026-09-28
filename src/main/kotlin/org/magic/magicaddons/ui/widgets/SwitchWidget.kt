package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.easedProgress
import org.magic.magicaddons.util.ScreenUtil.fillPill
import org.magic.magicaddons.util.ScreenUtil.inRect

class SwitchWidget(isOn: Boolean, val width: Int = WIDTH, val height: Int = HEIGHT) {

    var x: Int = 0
    var y: Int = 0

    var isOn: Boolean = isOn
        private set

    var isHovered: Boolean = false

    private var flippedAt: Long = 0

    fun set(value: Boolean) {
        if (value == isOn) return
        isOn = value
        flippedAt = System.currentTimeMillis()
    }

    fun render(graphics: GuiGraphicsExtractor) {
        graphics.fillPill(x, y, x + width, y + height, if (isOn) Common.UI.ACCENT_COLOR else Common.UI.SWITCH_OFF_COLOR)
        if (isHovered) graphics.fillPill(x, y, x + width, y + height, Common.UI.HOVER_WASH)

        val knobSize = height - KNOB_INSET * 2
        val knobTravel = width - KNOB_INSET * 2 - knobSize
        val slideProgress = easedProgress(flippedAt, SLIDE_MS)
        val fraction = if (isOn) slideProgress else 1f - slideProgress
        val knobX = x + KNOB_INSET + kotlin.math.round(knobTravel * fraction).toInt()

        graphics.fillPill(knobX, y + KNOB_INSET, knobX + knobSize, y + KNOB_INSET + knobSize, if (isOn) Common.UI.TEXT_COLOR else Common.UI.DISABLED_TEXT_COLOR)
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, width, height)

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        isHovered = isMouseOver(mouseX, mouseY)
    }

    companion object {
        const val WIDTH: Int = 22
        const val HEIGHT: Int = 12
        private const val KNOB_INSET: Int = 2
        private const val SLIDE_MS: Long = 150
    }
}
