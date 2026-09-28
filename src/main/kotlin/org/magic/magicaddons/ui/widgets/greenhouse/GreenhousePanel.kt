package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormatType
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.modText

class GreenhousePanel(
    overlayContext: OverlayContext,
    private val onPlanPicked: (PlotLayout?) -> Unit,
    private val onNoRotateChanged: (Boolean) -> Unit,
    private val onTurnPlan: () -> Unit,
    private val onEditPreset: () -> Unit,
    private val onSaveAsPreset: () -> Unit,
    private val onRescan: (MouseButtonEvent) -> Unit,
    private val layoutToExport: () -> PlotLayout?
) : ActionPanel(overlayContext) {

    private val turnButton = ClickableButtonWidget("↻")
    private val editButton = ClickableButtonWidget("Edit")
    private val saveButton = ClickableButtonWidget("Save as preset")
    private val rescanButton = ClickableButtonWidget("Rescan")
    private val exportButton = ClickableButtonWidget("Export")

    private val font = Minecraft.getInstance().font

    class PlanChoice(val plot: PlotLayout?, private val label: String) {
        override fun toString(): String = label
        override fun equals(other: Any?): Boolean = other is PlanChoice && other.plot === plot
        override fun hashCode(): Int = System.identityHashCode(plot)
    }

    private val planSelector = DropdownWidget(
        values = emptyList<PlanChoice>(),
        currentValue = null as PlanChoice?,
        overlayContext = overlayContext,
        onValueChanged = { onPlanPicked(it.plot) }
    )

    private val noRotateCheckbox = CheckboxWidget(CHECKBOX_SIZE)
    private var isNoRotateHovered: Boolean = false

    val isNoRotateChecked: Boolean get() = noRotateCheckbox.isChecked

    private var assignedPlan: PlotLayout? = null

    fun showPlans(planChoices: List<PlanChoice>, assignedChoice: PlanChoice?, isAssignedNoRotate: Boolean) {
        val choices = listOf(PlanChoice(null, NO_PLAN_LABEL)) + planChoices
        if (choices.map { it.plot } != planSelector.values.map { it.plot }) planSelector.values = choices
        planSelector.currentValue = assignedChoice ?: choices.first()
        if (assignedPlan !== assignedChoice?.plot) {
            assignedPlan = assignedChoice?.plot
            noRotateCheckbox.isChecked = assignedChoice != null && isAssignedNoRotate
        }
    }

    var isGreenhouseShown: Boolean = false
        set(value) {
            if (field == value) return

            field = value
            layoutIn(x, y, availableWidth)
        }

    override val buttons: List<ClickableButtonWidget> =
        listOf(turnButton, editButton, saveButton, exportButton, rescanButton)

    override fun isShown(button: ClickableButtonWidget): Boolean = when (button) {
        saveButton, exportButton, rescanButton -> isGreenhouseShown
        else -> isGreenhouseShown && assignedPlan != null
    }

    override fun groupOf(button: ClickableButtonWidget): Int = when (button) {
        saveButton, exportButton, rescanButton -> 1
        else -> 0
    }

    override fun groupLabel(group: Int): String? = if (group == 1) "This greenhouse" else null

    override fun headerHeight(): Int = if (isGreenhouseShown) ClickableButtonWidget.DEFAULT_HEIGHT + Common.UI.SPACING else 0

    private fun layoutPlanRow() {
        val labelWidth = font.width(NO_ROTATE_LABEL)
        val noRotateWidth = CHECKBOX_SIZE + Common.UI.SPACING + labelWidth

        planSelector.x = x + PADDING
        planSelector.y = y + PADDING
        planSelector.height = ClickableButtonWidget.DEFAULT_HEIGHT
        planSelector.width = availableWidth - PADDING * 2 - Common.UI.SPACING_LARGE - noRotateWidth

        noRotateCheckbox.x = planSelector.x + planSelector.width + Common.UI.SPACING_LARGE
        noRotateCheckbox.y = planSelector.y + (planSelector.height - CHECKBOX_SIZE) / 2
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (isGreenhouseShown) {
            layoutPlanRow()
            planSelector.extractRenderState(graphics, mouseX, mouseY, delta)
            noRotateCheckbox.render(graphics)
            val labelX = noRotateCheckbox.x + CHECKBOX_SIZE + Common.UI.SPACING
            graphics.modText(font, NO_ROTATE_LABEL, labelX, planSelector.y + (planSelector.height - font.lineHeight) / 2 + 1, Common.UI.TEXT_COLOR)
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)

        if (isGreenhouseShown && isNoRotateHovered) graphics.drawTooltipAtCursor(NO_ROTATE_TOOLTIP, mouseX, mouseY)
    }

    fun planSelectorClicked(event: MouseButtonEvent, doubled: Boolean): Boolean =
        isGreenhouseShown && planSelector.mouseClicked(event, doubled)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (isGreenhouseShown && isOverNoRotate(mouseButtonEvent.x, mouseButtonEvent.y)) {
            noRotateCheckbox.isChecked = !noRotateCheckbox.isChecked
            onNoRotateChanged(noRotateCheckbox.isChecked)
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        planSelector.mouseMoved(mouseX, mouseY)
        isNoRotateHovered = isGreenhouseShown && isOverNoRotate(mouseX, mouseY)
        super.mouseMoved(mouseX, mouseY)
    }

    private fun isOverNoRotate(mouseX: Double, mouseY: Double): Boolean {
        val right = noRotateCheckbox.x + CHECKBOX_SIZE + Common.UI.SPACING + font.width(NO_ROTATE_LABEL)
        return mouseX >= noRotateCheckbox.x && mouseX <= right &&
                mouseY >= planSelector.y && mouseY <= planSelector.y + planSelector.height
    }

    fun closePlanList() = planSelector.closeList()

    private fun openExportMenu(event: MouseButtonEvent) {
        openMenu(event, "Format:", LayoutFormatType.entries) { type ->
            val layout = layoutToExport() ?: return@openMenu
            copyExportToClipboard(type.format.export(layout), type.format, layout.displayName())
        }
    }

    override fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean {
        when (button) {
            turnButton -> onTurnPlan()
            editButton -> onEditPreset()
            saveButton -> onSaveAsPreset()
            rescanButton -> onRescan(event)
            exportButton -> openExportMenu(event)
            else -> return false
        }

        return true
    }

    private companion object {
        const val CHECKBOX_SIZE: Int = 10
        const val NO_PLAN_LABEL: String = "None"
    }
}
