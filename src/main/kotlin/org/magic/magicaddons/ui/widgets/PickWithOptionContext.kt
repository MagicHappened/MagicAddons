package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import kotlin.math.max

/**
 * A [PickContext] with a checkbox under the rows. Picking a value closes the list and hands it to
 * [onPick] with whether the box was checked.
 */
class PickWithOptionContext<T>(
    x: Int,
    y: Int,
    title: String,
    values: List<T>,
    private val optionLabel: String,
    optionChecked: Boolean,
    /** Shown while the cursor is on the checkbox row; none when null. */
    private val optionTooltip: String?,
    private val context: OverlayContext,
    private val onPick: (T, Boolean) -> Unit
) : AbstractSelectorContextMenu<T>(x, y, values, title, withSearch = false) {

    private val checkbox = CheckboxWidget(CHECKBOX_SIZE, optionChecked)

    override val footerHeight: Int get() = rowHeight

    override val overlayWidth: Int
        get() = max(super.overlayWidth, font.width(optionLabel) + CHECKBOX_SIZE + Common.UI.TEXT_X_PAD * 3)

    override fun renderFooter(graphics: GuiGraphicsExtractor, footerTop: Int, mouseX: Int, mouseY: Int) {
        graphics.text(
            font,
            Component.literal(optionLabel),
            overlayX + Common.UI.TEXT_X_PAD,
            footerTop + (footerHeight - font.lineHeight) / 2 + 1,
            Common.UI.TEXT_COLOR,
            false
        )
        checkbox.x = overlayX + overlayWidth - CHECKBOX_SIZE - Common.UI.TEXT_X_PAD
        checkbox.y = footerTop + (footerHeight - CHECKBOX_SIZE) / 2
        checkbox.render(graphics)
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderOverlay(graphics, mouseX, mouseY, delta)

        val footerTop = overlayY + overlayHeight - footerHeight
        val onFooter = mouseX in overlayX until overlayX + overlayWidth && mouseY in footerTop until footerTop + footerHeight
        if (onFooter && optionTooltip != null) graphics.drawTooltipAtCursor(optionTooltip, mouseX, mouseY)
    }

    override fun footerClicked() {
        checkbox.checked = !checkbox.checked
    }

    override fun onValueSelected(value: T) {
        context.removeOverlay(this)
        onPick(value, checkbox.checked)
    }

    private companion object {
        const val CHECKBOX_SIZE: Int = 10
    }
}
