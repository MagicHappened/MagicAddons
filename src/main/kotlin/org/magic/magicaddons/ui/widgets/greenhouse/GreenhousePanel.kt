package org.magic.magicaddons.ui.widgets.greenhouse

import java.time.Duration
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.CoopPlan
import org.magic.magicaddons.data.greenhouse.plot.PlanOwner
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormatType
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.modText

class GreenhousePanel(
    overlayContext: OverlayContext,
    private val onPlanPicked: (PlotLayout?) -> Unit,
    private val onNoRotateChanged: (Boolean) -> Unit,
    private val onTurnPlan: () -> Unit,
    private val onEditPreset: () -> Unit,
    private val onSaveAsPreset: () -> Unit,
    private val onRescan: (MouseButtonEvent) -> Unit,
    private val layoutToExport: () -> PlotLayout?,
    private val onRunningPlanPicked: (PlanOwner) -> Unit,
    private val onShareToCoop: (MouseButtonEvent) -> Unit,
    private val onCopyCoopPlan: () -> Unit,
    private val onClearCoopPlan: (MouseButtonEvent) -> Unit
) : ActionPanel(overlayContext) {

    private val turnButton = ClickableButtonWidget("↻")
    private val editButton = ClickableButtonWidget("Edit")
    private val shareButton = ClickableButtonWidget(SHARE_TO_COOP_LABEL)
    private val copyCoopPlanButton = ClickableButtonWidget(COPY_TO_MY_PRESETS_LABEL)
    private val clearCoopPlanButton = ClickableButtonWidget(CLEAR_COOP_PLAN_LABEL)
    private val saveButton = ClickableButtonWidget("Save as preset")
    private val rescanButton = ClickableButtonWidget("Rescan")
    private val exportButton = ClickableButtonWidget("Export")

    private val coopPlanSideButton = ClickableButtonWidget(0, ClickableButtonWidget.DEFAULT_HEIGHT) { graphics ->
        drawSideLabel(graphics, this, COOP_PLAN_SIDE_LABEL, PlanOwner.Coop)
    }
    private val myPlanSideButton = ClickableButtonWidget(0, ClickableButtonWidget.DEFAULT_HEIGHT) { graphics ->
        drawSideLabel(graphics, this, MY_PLAN_SIDE_LABEL, PlanOwner.Mine)
    }

    private val font = Minecraft.getInstance().font

    class PlanChoice(val plot: PlotLayout?, private val label: String) {
        override fun toString(): String = label
        override fun equals(other: Any?): Boolean = other is PlanChoice && other.plot === plot
        override fun hashCode(): Int = System.identityHashCode(plot)
    }

    class CoopSlot(val runs: PlanOwner, val greenhouseName: String, val plan: CoopPlan?, val isPlanInMyPresets: Boolean)

    private class CoopSideText(val headLines: List<FormattedCharSequence>, val detailLines: List<FormattedCharSequence>, val headColor: Int)

    private val planSelector = DropdownWidget(
        values = emptyList<PlanChoice>(),
        currentValue = null as PlanChoice?,
        overlayContext = overlayContext,
        onValueChanged = { onPlanPicked(it.plot) }
    )

    private val placedSelector = DropdownWidget(
        values = PlacedChoice.entries,
        currentValue = PlacedChoice.Off,
        overlayContext = overlayContext,
        isSearchable = false
    )

    val placedChoice: PlacedChoice get() = placedSelector.currentValue ?: PlacedChoice.Off

    fun dropPlacedTool() {
        placedSelector.currentValue = PlacedChoice.Off
    }

    override val dropdowns: List<DropdownWidget<*>> = listOf(placedSelector)

    override fun isShown(dropdown: DropdownWidget<*>): Boolean = isGreenhouseShown

    override fun groupOf(dropdown: DropdownWidget<*>): Int = 1

    private val noRotateCheckbox = CheckboxWidget(CHECKBOX_SIZE)
    private var isNoRotateHovered: Boolean = false

    val isNoRotateChecked: Boolean get() = noRotateCheckbox.isChecked

    private var assignedPlan: PlotLayout? = null

    private var coopSlot: CoopSlot? = null

    private var coopSlotLayoutKey: List<Any?> = emptyList()

    private var coopPlanDetailBox: ScreenRect? = null

    private var isCoopPlanDetailHovered: Boolean = false

    private val isPlanRowShown: Boolean get() = coopSlot?.runs != PlanOwner.Coop

    private val isCoopSideShown: Boolean get() = coopSlot?.runs == PlanOwner.Coop

    fun showPlans(planChoices: List<PlanChoice>, assignedChoice: PlanChoice?, isAssignedNoRotate: Boolean) {
        val choices = listOf(PlanChoice(null, NO_PLAN_LABEL)) + planChoices
        if (choices.map { it.plot } != planSelector.values.map { it.plot }) planSelector.values = choices
        planSelector.currentValue = assignedChoice ?: choices.first()
        if (assignedPlan !== assignedChoice?.plot) {
            assignedPlan = assignedChoice?.plot
            noRotateCheckbox.isChecked = assignedChoice != null && isAssignedNoRotate
            layoutIn(x, y, availableWidth)
        }
    }

    fun showCoopSlot(slot: CoopSlot?) {
        coopSlot = slot
        val copyLabel = if (slot?.isPlanInMyPresets == true) IN_MY_PRESETS_LABEL else COPY_TO_MY_PRESETS_LABEL
        if (copyCoopPlanButton.label?.string != copyLabel) {
            copyCoopPlanButton.label = Component.literal(copyLabel)
            copyCoopPlanButton.width = ClickableButtonWidget.widthFor(copyLabel)
        }
        val layoutKey = listOf(slot?.runs, slot?.plan, slot?.plan?.name, slot?.isPlanInMyPresets, headerHeight())
        if (layoutKey != coopSlotLayoutKey) {
            coopSlotLayoutKey = layoutKey
            layoutIn(x, y, availableWidth)
        }
    }

    var isGreenhouseShown: Boolean = false
        set(value) {
            if (field == value) return

            field = value
            layoutIn(x, y, availableWidth)
        }

    override val buttons: List<ClickableButtonWidget> =
        listOf(turnButton, editButton, shareButton, copyCoopPlanButton, clearCoopPlanButton, saveButton, exportButton, rescanButton)

    override fun isShown(button: ClickableButtonWidget): Boolean = when (button) {
        saveButton, exportButton, rescanButton -> isGreenhouseShown
        shareButton -> isGreenhouseShown && isPlanRowShown && assignedPlan != null && coopSlot != null
        copyCoopPlanButton, clearCoopPlanButton -> isGreenhouseShown && isCoopSideShown && coopSlot?.plan != null
        else -> isGreenhouseShown && isPlanRowShown && assignedPlan != null
    }

    override fun groupOf(button: ClickableButtonWidget): Int = when (button) {
        saveButton, exportButton, rescanButton -> 1
        else -> 0
    }

    override fun groupLabel(group: Int): String? = if (group == 1) "This greenhouse" else null

    override fun groupNotes(): Map<Int, String> {
        if (!isGreenhouseShown || isCoopSideShown) return emptyMap()
        val plan = coopSlot?.plan ?: return emptyMap()

        return mapOf(0 to "Coop plan here: ${plan.name} · ${CoopPlan.memberNameInSentence(plan.sharedBy)}")
    }

    private fun switchRowHeight(): Int = if (coopSlot != null) ClickableButtonWidget.DEFAULT_HEIGHT + Common.UI.SPACING else 0

    override fun headerHeight(): Int {
        if (!isGreenhouseShown) return 0
        val sideText = coopSideText() ?: return switchRowHeight() + ClickableButtonWidget.DEFAULT_HEIGHT + Common.UI.SPACING

        return switchRowHeight() + (sideText.headLines.size + sideText.detailLines.size) * font.lineHeight + Common.UI.SPACING
    }

    private fun coopSideText(): CoopSideText? {
        val slot = coopSlot?.takeIf { it.runs == PlanOwner.Coop } ?: return null
        val wrapWidth = availableWidth - PADDING * 2
        fun wrapped(text: String) = font.split(Component.literal(text), wrapWidth)

        val plan = slot.plan ?: return CoopSideText(
            wrapped("No coop plan on ${slot.greenhouseName}."),
            wrapped(EMPTY_SLOT_HINT),
            Common.UI.TEXT_DIM_COLOR
        )
        val steps = if (plan.stepCount == 1) "1 step" else "${plan.stepCount} steps"

        return CoopSideText(
            wrapped(plan.name),
            wrapped("$steps · shared by ${CoopPlan.memberNameInSentence(plan.sharedBy)} · ${sharedAgoText(plan.sharedAt)} ago"),
            Common.UI.TEXT_COLOR
        )
    }

    private fun sharedAgoText(sharedAt: Long): String {
        val age = Duration.ofMillis(System.currentTimeMillis() - sharedAt)

        return when {
            age.toDays() > 0 -> "${age.toDays()}d"
            age.toHours() > 0 -> "${age.toHours()}h"
            else -> "${age.toMinutes().coerceAtLeast(1)}m"
        }
    }

    private fun layoutSwitchRow() {
        val sideWidth = (availableWidth - PADDING * 2 - Common.UI.SPACING) / 2

        coopPlanSideButton.x = x + PADDING
        coopPlanSideButton.y = y + PADDING
        coopPlanSideButton.width = sideWidth

        myPlanSideButton.x = coopPlanSideButton.x + sideWidth + Common.UI.SPACING
        myPlanSideButton.y = coopPlanSideButton.y
        myPlanSideButton.width = sideWidth
    }

    private fun drawSideLabel(graphics: GuiGraphicsExtractor, button: ClickableButtonWidget, label: String, owner: PlanOwner) {
        val markedLabel = if (coopSlot?.runs == owner) "$SELECTED_SIDE_MARK$label" else label
        val shownLabel = ellipsised(font, markedLabel, button.width - Common.UI.TEXT_X_PAD * 2)
        graphics.text(
            font,
            Component.literal(shownLabel),
            button.x + (button.width - font.width(shownLabel)) / 2,
            button.y + (button.height - font.lineHeight) / 2,
            Common.UI.TEXT_COLOR or Common.UI.OPAQUE_ALPHA,
            false
        )
    }

    private fun layoutPlanRow() {
        val labelWidth = font.width(NO_ROTATE_LABEL)
        val noRotateWidth = CHECKBOX_SIZE + Common.UI.SPACING + labelWidth

        planSelector.x = x + PADDING
        planSelector.y = y + PADDING + switchRowHeight()
        planSelector.height = ClickableButtonWidget.DEFAULT_HEIGHT
        planSelector.width = availableWidth - PADDING * 2 - Common.UI.SPACING_LARGE - noRotateWidth

        noRotateCheckbox.x = planSelector.x + planSelector.width + Common.UI.SPACING_LARGE
        noRotateCheckbox.y = planSelector.y + (planSelector.height - CHECKBOX_SIZE) / 2
    }

    private fun drawCoopSideText(graphics: GuiGraphicsExtractor, sideText: CoopSideText) {
        val textX = x + PADDING
        var lineY = y + PADDING + switchRowHeight()
        sideText.headLines.forEach { line ->
            graphics.modText(font, line, textX, lineY, sideText.headColor)
            lineY += font.lineHeight
        }
        val detailTop = lineY
        sideText.detailLines.forEach { line ->
            graphics.modText(font, line, textX, lineY, Common.UI.TEXT_DIM_COLOR)
            lineY += font.lineHeight
        }
        coopPlanDetailBox = if (coopSlot?.plan != null) {
            ScreenRect(textX, detailTop, sideText.detailLines.maxOfOrNull { font.width(it) } ?: 0, lineY - detailTop)
        } else {
            null
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        coopPlanDetailBox = null
        if (isGreenhouseShown) {
            coopSlot?.let { slot ->
                layoutSwitchRow()
                coopPlanSideButton.isPressed = slot.runs == PlanOwner.Coop
                myPlanSideButton.isPressed = slot.runs == PlanOwner.Mine
                coopPlanSideButton.extractRenderState(graphics, mouseX, mouseY, delta)
                myPlanSideButton.extractRenderState(graphics, mouseX, mouseY, delta)
            }
            if (isPlanRowShown) {
                layoutPlanRow()
                planSelector.extractRenderState(graphics, mouseX, mouseY, delta)
                noRotateCheckbox.render(graphics)
                val labelX = noRotateCheckbox.x + CHECKBOX_SIZE + Common.UI.SPACING
                graphics.modText(font, NO_ROTATE_LABEL, labelX, planSelector.y + (planSelector.height - font.lineHeight) / 2 + 1, Common.UI.TEXT_COLOR)
            }
            coopSideText()?.let { drawCoopSideText(graphics, it) }
        }

        placedSelector.frameColor = when (placedChoice) {
            PlacedChoice.Off -> null
            PlacedChoice.Mark -> Common.UI.ACCENT_COLOR
            PlacedChoice.Unmark -> Common.UI.DANGER_COLOR
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        if (isGreenhouseShown && isPlanRowShown && isNoRotateHovered) graphics.drawTooltipAtCursor(NO_ROTATE_TOOLTIP, mouseX, mouseY)
        if (isGreenhouseShown && isCoopPlanDetailHovered && coopPlanDetailBox != null) {
            graphics.drawTooltipAtCursor(COOP_HARVESTS_TOOLTIP, mouseX, mouseY)
        }
    }

    fun planSelectorClicked(event: MouseButtonEvent, doubled: Boolean): Boolean =
        isGreenhouseShown && isPlanRowShown && planSelector.mouseClicked(event, doubled)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (isGreenhouseShown && coopSlot != null) {
            if (coopPlanSideButton.mouseClicked(mouseButtonEvent, doubled)) {
                onRunningPlanPicked(PlanOwner.Coop)
                return true
            }
            if (myPlanSideButton.mouseClicked(mouseButtonEvent, doubled)) {
                onRunningPlanPicked(PlanOwner.Mine)
                return true
            }
        }
        if (isGreenhouseShown && isPlanRowShown && isOverNoRotate(mouseButtonEvent.x, mouseButtonEvent.y)) {
            noRotateCheckbox.isChecked = !noRotateCheckbox.isChecked
            onNoRotateChanged(noRotateCheckbox.isChecked)
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        planSelector.mouseMoved(mouseX, mouseY)
        coopPlanSideButton.mouseMoved(mouseX, mouseY)
        myPlanSideButton.mouseMoved(mouseX, mouseY)
        isNoRotateHovered = isGreenhouseShown && isPlanRowShown && isOverNoRotate(mouseX, mouseY)
        isCoopPlanDetailHovered = coopPlanDetailBox?.let { box -> inRect(mouseX, mouseY, box.x, box.y, box.width, box.height) } == true
        super.mouseMoved(mouseX, mouseY)
    }

    private fun isOverNoRotate(mouseX: Double, mouseY: Double): Boolean {
        val right = noRotateCheckbox.x + CHECKBOX_SIZE + Common.UI.SPACING + font.width(NO_ROTATE_LABEL)
        return mouseX >= noRotateCheckbox.x && mouseX <= right &&
                mouseY >= planSelector.y && mouseY <= planSelector.y + planSelector.height
    }

    fun closePlanList() {
        planSelector.closeList()
        placedSelector.closeList()
    }

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
            shareButton -> onShareToCoop(event)
            copyCoopPlanButton -> if (coopSlot?.isPlanInMyPresets != true) onCopyCoopPlan()
            clearCoopPlanButton -> onClearCoopPlan(event)
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
        const val COOP_PLAN_SIDE_LABEL: String = "Coop plan"
        const val MY_PLAN_SIDE_LABEL: String = "My plan"
        const val SELECTED_SIDE_MARK: String = "● "
        const val SHARE_TO_COOP_LABEL: String = "Share to coop"
        const val COPY_TO_MY_PRESETS_LABEL: String = "Copy to my presets"
        const val IN_MY_PRESETS_LABEL: String = "In my presets"
        const val CLEAR_COOP_PLAN_LABEL: String = "Clear coop plan"
        const val EMPTY_SLOT_HINT: String = "Share one from My plan."
        const val COOP_HARVESTS_TOOLTIP: String = "Harvests only count for people with the mod with Discord Integration enabled."
    }
}
