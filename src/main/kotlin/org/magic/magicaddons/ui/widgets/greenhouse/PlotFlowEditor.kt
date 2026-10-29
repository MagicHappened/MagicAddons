package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.plot.ExitCondition
import org.magic.magicaddons.data.greenhouse.plot.FlowStep
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.InputSpawns
import org.magic.magicaddons.data.greenhouse.plot.PlotFlow
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.StepExit
import org.magic.magicaddons.data.greenhouse.plot.StepPolicies
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.modText

class PlotFlowEditor(
    screenWidth: Int,
    screenHeight: Int,
    private val preset: GreenhouseLayout,
    private val plot: PlotLayout,
    private val context: OverlayContext,
    private val onSaved: () -> Unit
) : OverlayRenderable {

    override val renderPriority: Int = OverlayRenderable.MENU_PRIORITY

    override var hoveredElement: GuiEventListener? = null

    private val font = Minecraft.getInstance().font

    private enum class Kind(private val label: String, val needsMutation: Boolean, val amountLabel: String) {
        Spawned("a mutation has spawned", true, "times"),
        Stage("a crop reaches stage", false, ""),
        Harvested("a mutation was harvested", true, "times");

        override fun toString(): String = label
    }

    private inner class EditedCondition(kind: Kind, crop: CropDefinition?, amount: Int, var joinsWithOr: Boolean) {
        val kindSelector = DropdownWidget(values = Kind.entries, currentValue = kind, overlayContext = context, isSearchable = false, onValueChanged = { refreshCrops() })
        val cropSelector = DropdownWidget(values = cropsFor(kind), currentValue = crop, overlayContext = context)
        val amountField = TextField(AMOUNT_WIDTH, FIELD_HEIGHT).apply {
            isFramed = true
            value = amount.toString()
            setMaxLength(4)
        }
        val removeButton = ClickableButtonWidget(SQUARE, SQUARE, Component.literal("×"))
        var handleBox: ScreenRect = ScreenRect(0, 0, 0, 0)
        var joinBox: ScreenRect = ScreenRect(0, 0, 0, 0)

        private fun refreshCrops() {
            val kind = kindSelector.currentValue ?: Kind.Spawned
            cropSelector.values = cropsFor(kind)
            if (cropSelector.currentValue !in cropSelector.values) cropSelector.currentValue = cropSelector.values.firstOrNull()
        }

        fun toCondition(): ExitCondition? {
            val crop = cropSelector.currentValue ?: return null
            val amount = amountField.value.toIntOrNull()?.coerceAtLeast(1) ?: 1
            return when (kindSelector.currentValue ?: Kind.Spawned) {
                Kind.Spawned -> ExitCondition.MutationSpawned(crop, amount)
                Kind.Stage -> ExitCondition.HighestStageAtLeast(crop, amount)
                Kind.Harvested -> ExitCondition.MutationHarvested(crop, amount)
            }
        }
    }

    private inner class EditedStep(label: String?, val layout: PlotLayout?, policies: StepPolicies, conditions: List<EditedCondition>) {
        val nameField = TextField(NAME_WIDTH, FIELD_HEIGHT).apply {
            isFramed = true
            value = label.orEmpty()
            setMaxLength(32)
        }
        val fullClear = CheckboxWidget(CHECKBOX_SIZE, policies.fullClear)
        val keepIdentical = CheckboxWidget(CHECKBOX_SIZE, policies.keepIdentical)
        val harvestInputs = CheckboxWidget(CHECKBOX_SIZE, policies.inputSpawns == InputSpawns.Harvest)
        val upButton = ClickableButtonWidget(SQUARE, SQUARE, Component.literal("▲"))
        val downButton = ClickableButtonWidget(SQUARE, SQUARE, Component.literal("▼"))
        val deleteButton = ClickableButtonWidget(SQUARE, SQUARE, Component.literal("×"))
        val addConditionButton = ClickableButtonWidget("Add condition")
        val conditions: MutableList<EditedCondition> = conditions.toMutableList()
        var isFolded: Boolean = false
        var foldBox: ScreenRect = ScreenRect(0, 0, 0, 0)
        var headerBox: ScreenRect = ScreenRect(0, 0, 0, 0)

        fun exits(): List<StepExit> {
            val exits = mutableListOf<MutableList<ExitCondition>>()
            conditions.forEachIndexed { index, edited ->
                val condition = edited.toCondition() ?: return@forEachIndexed
                if (index == 0 || edited.joinsWithOr || exits.isEmpty()) exits.add(mutableListOf(condition)) else exits.last().add(condition)
            }
            return exits.map { StepExit(it) }
        }

        fun policies(): StepPolicies = StepPolicies(
            fullClear = fullClear.isChecked,
            keepIdentical = keepIdentical.isChecked,
            inputSpawns = if (harvestInputs.isChecked) InputSpawns.Harvest else InputSpawns.Keep
        )
    }

    private val steps: MutableList<EditedStep> = plot.steps.map { step ->
        EditedStep(step.label, step.layout, step.policies, step.exits.flatMapIndexed { exitIndex, exit ->
            exit.conditions.mapIndexed { conditionIndex, condition ->
                val (kind, amount) = when (condition) {
                    is ExitCondition.MutationSpawned -> Kind.Spawned to condition.count
                    is ExitCondition.HighestStageAtLeast -> Kind.Stage to condition.stage
                    is ExitCondition.MutationHarvested -> Kind.Harvested to condition.count
                }
                EditedCondition(kind, condition.crop, amount, joinsWithOr = conditionIndex == 0 && exitIndex > 0)
            }
        })
    }.toMutableList()

    private val loopCheckbox = CheckboxWidget(CHECKBOX_SIZE, plot.flow?.loop ?: false)
    private val addStepButton = ClickableButtonWidget("Add step")
    private val cancelButton = ClickableButtonWidget("Cancel")
    private val saveButton = ClickableButtonWidget("Save")

    override val overlayWidth: Int = (screenWidth - Common.UI.SPACING_LARGE * 4).coerceAtMost(MAX_WIDTH)
    override val overlayHeight: Int = screenHeight - Common.UI.SPACING_LARGE * 4
    override val overlayX: Int = (screenWidth - overlayWidth) / 2
    override val overlayY: Int = (screenHeight - overlayHeight) / 2

    private var scroll: Int = 0
    private var contentHeight: Int = 0

    private var draggingCondition: Pair<EditedStep, EditedCondition>? = null
    private var dragY: Int = 0

    private val title = "${preset.plotTitle(plot)} · flow"

    private fun cropsFor(kind: Kind): List<CropDefinition> =
        CropRegistry.allCrops.filter { if (kind.needsMutation) it.isMutation && it.spawnRule != null else it.isMutation || it.isBaseCrop }.sortedBy { it.name }

    private fun listTop(): Int = overlayY + PADDING + TITLE_HEIGHT + font.lineHeight + Common.UI.SPACING
    private fun listBottom(): Int = overlayY + overlayHeight - PADDING - ClickableButtonWidget.DEFAULT_HEIGHT - Common.UI.SPACING

    private fun layoutRows() {
        val left = overlayX + PADDING
        val innerWidth = overlayWidth - PADDING * 2
        var y = listTop() - scroll

        steps.forEachIndexed { index, step ->
            step.headerBox = ScreenRect(left, y, innerWidth, HEADER_HEIGHT)
            step.foldBox = ScreenRect(left + ROW_INSET, y, FOLD_WIDTH, HEADER_HEIGHT)
            var x = left + ROW_INSET + FOLD_WIDTH + font.width("$STEP_NUMBER_WIDTH") + Common.UI.SPACING
            step.nameField.x = x
            step.nameField.y = y + (HEADER_HEIGHT - FIELD_HEIGHT) / 2
            x += step.nameField.width + Common.UI.SPACING_LARGE
            listOf(step.fullClear to FULL_CLEAR_LABEL, step.keepIdentical to KEEP_IDENTICAL_LABEL, step.harvestInputs to HARVEST_INPUTS_LABEL).forEach { (checkbox, label) ->
                checkbox.x = x
                checkbox.y = y + (HEADER_HEIGHT - CHECKBOX_SIZE) / 2
                x += CHECKBOX_SIZE + Common.UI.SPACING + font.width(label) + Common.UI.SPACING_LARGE
            }
            step.deleteButton.x = left + innerWidth - ROW_INSET - SQUARE
            step.downButton.x = step.deleteButton.x - Common.UI.SPACING - SQUARE
            step.upButton.x = step.downButton.x - Common.UI.SPACING - SQUARE
            listOf(step.upButton, step.downButton, step.deleteButton).forEach { it.y = y + (HEADER_HEIGHT - SQUARE) / 2 }
            y += HEADER_HEIGHT

            if (!step.isFolded) {
                step.conditions.forEachIndexed { conditionIndex, condition ->
                    var cx = left + ROW_INSET + CONDITION_INSET
                    condition.handleBox = ScreenRect(cx, y, HANDLE_WIDTH, CONDITION_HEIGHT)
                    cx += HANDLE_WIDTH + Common.UI.SPACING
                    condition.joinBox = ScreenRect(cx, y + (CONDITION_HEIGHT - FIELD_HEIGHT) / 2, JOIN_WIDTH, FIELD_HEIGHT)
                    cx += JOIN_WIDTH + Common.UI.SPACING
                    condition.kindSelector.x = cx
                    condition.kindSelector.y = y + (CONDITION_HEIGHT - FIELD_HEIGHT) / 2
                    condition.kindSelector.width = KIND_WIDTH
                    condition.kindSelector.height = FIELD_HEIGHT
                    cx += KIND_WIDTH + Common.UI.SPACING
                    condition.cropSelector.x = cx
                    condition.cropSelector.y = condition.kindSelector.y
                    condition.cropSelector.width = CROP_WIDTH
                    condition.cropSelector.height = FIELD_HEIGHT
                    cx += CROP_WIDTH + Common.UI.SPACING
                    condition.amountField.x = cx
                    condition.amountField.y = condition.kindSelector.y
                    condition.removeButton.x = left + innerWidth - ROW_INSET - SQUARE
                    condition.removeButton.y = y + (CONDITION_HEIGHT - SQUARE) / 2
                    y += CONDITION_HEIGHT
                    if (conditionIndex == step.conditions.lastIndex) y += Common.UI.SPACING_SMALL
                }
                step.addConditionButton.x = left + ROW_INSET + CONDITION_INSET
                step.addConditionButton.y = y
                y += step.addConditionButton.height + Common.UI.SPACING
            }
            y += STEP_GAP
        }
        addStepButton.x = left
        addStepButton.y = y
        y += addStepButton.height
        contentHeight = y + scroll - listTop()

        val buttonsY = overlayY + overlayHeight - PADDING - ClickableButtonWidget.DEFAULT_HEIGHT
        saveButton.x = overlayX + overlayWidth - PADDING - saveButton.width
        saveButton.y = buttonsY
        cancelButton.x = saveButton.x - Common.UI.SPACING - cancelButton.width
        cancelButton.y = buttonsY
        loopCheckbox.x = overlayX + overlayWidth - PADDING - CHECKBOX_SIZE
        loopCheckbox.y = overlayY + PADDING + (TITLE_HEIGHT - CHECKBOX_SIZE) / 2
    }

    private fun isRowVisible(top: Int, height: Int): Boolean = top + height > listTop() && top < listBottom()

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val window = Minecraft.getInstance().window
        graphics.fill(0, 0, window.guiScaledWidth * 4, window.guiScaledHeight * 4, Common.UI.SCREEN_DIM_COLOR)
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)
        layoutRows()

        graphics.modText(font, title, overlayX + PADDING, overlayY + PADDING + (TITLE_HEIGHT - font.lineHeight) / 2 + 1, Common.UI.TEXT_COLOR)
        loopCheckbox.render(graphics)
        graphics.modText(font, LOOP_LABEL, loopCheckbox.x - Common.UI.SPACING - font.width(LOOP_LABEL), loopCheckbox.y + (CHECKBOX_SIZE - font.lineHeight) / 2 + 1, Common.UI.TEXT_DIM_COLOR)
        graphics.modText(font, HINT, overlayX + PADDING, overlayY + PADDING + TITLE_HEIGHT, Common.UI.TEXT_DIM_COLOR)

        graphics.enableScissor(overlayX, listTop(), overlayX + overlayWidth, listBottom())
        steps.forEachIndexed { index, step -> renderStep(graphics, index, step, mouseX, mouseY, delta) }
        if (isRowVisible(addStepButton.y, addStepButton.height)) {
            addStepButton.extractRenderState(graphics, mouseX, mouseY, delta)
            val last = steps.lastIndex + 1
            graphics.modText(font, "starts as a copy of step $last's layout", addStepButton.x + addStepButton.width + Common.UI.SPACING_LARGE, addStepButton.y + (addStepButton.height - font.lineHeight) / 2 + 1, Common.UI.TEXT_DIM_COLOR)
        }
        graphics.disableScissor()

        draggingCondition?.let { (_, condition) ->
            graphics.modText(font, "${condition.kindSelector.currentValue} ${condition.cropSelector.currentValue?.name.orEmpty()} ${condition.amountField.value}", overlayX + PADDING + ROW_INSET + CONDITION_INSET, dragY - font.lineHeight / 2, Common.UI.ACCENT_COLOR)
        }

        cancelButton.extractRenderState(graphics, mouseX, mouseY, delta)
        saveButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    private fun renderStep(graphics: GuiGraphicsExtractor, index: Int, step: EditedStep, mouseX: Int, mouseY: Int, delta: Float) {
        val box = step.headerBox
        val blockBottom = if (step.isFolded) box.y + box.height else step.addConditionButton.y + step.addConditionButton.height + Common.UI.SPACING
        if (!isRowVisible(box.y, blockBottom - box.y)) return
        graphics.fill(box.x, box.y, box.x + box.width, blockBottom, Common.UI.FIELD_COLOR)
        graphics.drawBorder(box.x, box.y, box.x + box.width, blockBottom, 1, Common.UI.BORDER_COLOR)

        val textY = box.y + (HEADER_HEIGHT - font.lineHeight) / 2 + 1
        graphics.modText(font, if (step.isFolded) "▸" else "▾", step.foldBox.x, textY, Common.UI.TEXT_DIM_COLOR)
        graphics.modText(font, "${index + 1}", step.foldBox.x + FOLD_WIDTH, textY, Common.UI.TEXT_DIM_COLOR)
        step.nameField.render(graphics)
        var x = step.nameField.x + step.nameField.width + Common.UI.SPACING_LARGE
        listOf(step.fullClear to FULL_CLEAR_LABEL, step.keepIdentical to KEEP_IDENTICAL_LABEL, step.harvestInputs to HARVEST_INPUTS_LABEL).forEach { (checkbox, label) ->
            checkbox.render(graphics)
            graphics.modText(font, label, x + CHECKBOX_SIZE + Common.UI.SPACING, textY, Common.UI.TEXT_DIM_COLOR)
            x += CHECKBOX_SIZE + Common.UI.SPACING + font.width(label) + Common.UI.SPACING_LARGE
        }
        if (step.isFolded) {
            val summary = if (step.conditions.isEmpty()) NO_EXIT else "${step.conditions.size} condition" + if (step.conditions.size == 1) "" else "s"
            graphics.modText(font, summary, x, textY, Common.UI.TEXT_DIM_COLOR)
        }
        step.upButton.extractRenderState(graphics, mouseX, mouseY, delta)
        step.downButton.extractRenderState(graphics, mouseX, mouseY, delta)
        step.deleteButton.extractRenderState(graphics, mouseX, mouseY, delta)
        if (step.isFolded) return

        step.conditions.forEachIndexed { conditionIndex, condition ->
            val rowTop = condition.handleBox.y
            val rowTextY = rowTop + (CONDITION_HEIGHT - font.lineHeight) / 2 + 1
            graphics.modText(font, HANDLE, condition.handleBox.x, rowTextY, Common.UI.TEXT_DIM_COLOR)
            if (conditionIndex == 0) {
                graphics.modText(font, WHEN_LABEL, condition.joinBox.x, rowTextY, Common.UI.TEXT_DIM_COLOR)
            } else {
                val label = if (condition.joinsWithOr) OR_LABEL else AND_LABEL
                val color = if (condition.joinsWithOr) Common.UI.PENDING_COLOR else Common.UI.ACCENT_COLOR
                graphics.drawBorder(condition.joinBox.x, condition.joinBox.y, condition.joinBox.x + condition.joinBox.width, condition.joinBox.y + condition.joinBox.height, 1, color)
                graphics.modText(font, label, condition.joinBox.x + (JOIN_WIDTH - font.width(label)) / 2, rowTextY, color)
            }
            condition.kindSelector.extractRenderState(graphics, mouseX, mouseY, delta)
            condition.cropSelector.extractRenderState(graphics, mouseX, mouseY, delta)
            condition.amountField.render(graphics)
            val kind = condition.kindSelector.currentValue ?: Kind.Spawned
            if (kind.amountLabel.isNotEmpty()) graphics.modText(font, kind.amountLabel, condition.amountField.x + condition.amountField.width + Common.UI.SPACING, rowTextY, Common.UI.TEXT_DIM_COLOR)
            condition.removeButton.extractRenderState(graphics, mouseX, mouseY, delta)
        }
        if (step.conditions.isEmpty()) {
            graphics.modText(font, NO_EXIT, step.addConditionButton.x + step.addConditionButton.width + Common.UI.SPACING_LARGE, step.addConditionButton.y + (step.addConditionButton.height - font.lineHeight) / 2 + 1, Common.UI.TEXT_DIM_COLOR)
        }
        step.addConditionButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        cancelButton.mouseMoved(mouseX, mouseY)
        saveButton.mouseMoved(mouseX, mouseY)
        addStepButton.mouseMoved(mouseX, mouseY)
        steps.forEach { step ->
            listOf(step.upButton, step.downButton, step.deleteButton, step.addConditionButton).forEach { it.mouseMoved(mouseX, mouseY) }
            step.conditions.forEach { condition ->
                condition.kindSelector.mouseMoved(mouseX, mouseY)
                condition.cropSelector.mouseMoved(mouseX, mouseY)
                condition.removeButton.mouseMoved(mouseX, mouseY)
            }
        }
        if (draggingCondition != null) dragY = mouseY.toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        val room = (contentHeight - (listBottom() - listTop())).coerceAtLeast(0)
        scroll = (scroll - (scrollY * Common.UI.SCROLL_STEP).toInt()).coerceIn(0, room)
        return true
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = mouseButtonEvent.x
        val mouseY = mouseButtonEvent.y
        steps.forEach { step ->
            step.conditions.forEach { condition ->
                listOf(condition.kindSelector, condition.cropSelector).forEach { if (!it.isMouseOver(mouseX, mouseY)) it.closeList() }
            }
        }
        if (cancelButton.mouseClicked(mouseButtonEvent, doubled)) {
            context.removeOverlay(this)
            return true
        }
        if (saveButton.mouseClicked(mouseButtonEvent, doubled)) {
            save()
            context.removeOverlay(this)
            return true
        }
        if (inRect(mouseX, mouseY, loopCheckbox.x - Common.UI.SPACING - font.width(LOOP_LABEL), loopCheckbox.y, CHECKBOX_SIZE + Common.UI.SPACING + font.width(LOOP_LABEL), CHECKBOX_SIZE)) {
            loopCheckbox.isChecked = !loopCheckbox.isChecked
            return true
        }
        if (mouseY < listTop() || mouseY >= listBottom()) return isMouseOver(mouseX, mouseY)

        if (addStepButton.mouseClicked(mouseButtonEvent, doubled)) {
            steps.add(EditedStep(null, null, StepPolicies(), emptyList()))
            return true
        }
        steps.toList().forEachIndexed { index, step ->
            if (step.deleteButton.mouseClicked(mouseButtonEvent, doubled)) {
                if (steps.size > 1) steps.removeAt(index)
                return true
            }
            if (step.upButton.mouseClicked(mouseButtonEvent, doubled)) {
                if (index > 0) steps.add(index - 1, steps.removeAt(index))
                return true
            }
            if (step.downButton.mouseClicked(mouseButtonEvent, doubled)) {
                if (index < steps.lastIndex) steps.add(index + 1, steps.removeAt(index))
                return true
            }
            if (inRect(mouseX, mouseY, step.foldBox.x, step.foldBox.y, step.foldBox.width, step.foldBox.height)) {
                step.isFolded = !step.isFolded
                return true
            }
            if (step.nameField.mouseClicked(mouseButtonEvent, doubled)) return true
            listOf(step.fullClear, step.keepIdentical, step.harvestInputs).forEach { checkbox ->
                if (inRect(mouseX, mouseY, checkbox.x, checkbox.y, CHECKBOX_SIZE + Common.UI.SPACING + font.width(KEEP_IDENTICAL_LABEL), CHECKBOX_SIZE)) {
                    checkbox.isChecked = !checkbox.isChecked
                    return true
                }
            }
            if (step.isFolded) return@forEachIndexed
            if (step.addConditionButton.mouseClicked(mouseButtonEvent, doubled)) {
                step.conditions.add(EditedCondition(Kind.Spawned, cropsFor(Kind.Spawned).firstOrNull(), 1, joinsWithOr = false))
                return true
            }
            step.conditions.toList().forEachIndexed { conditionIndex, condition ->
                if (condition.removeButton.mouseClicked(mouseButtonEvent, doubled)) {
                    step.conditions.removeAt(conditionIndex)
                    step.conditions.firstOrNull()?.joinsWithOr = false
                    return true
                }
                if (conditionIndex > 0 && inRect(mouseX, mouseY, condition.joinBox.x, condition.joinBox.y, condition.joinBox.width, condition.joinBox.height)) {
                    condition.joinsWithOr = !condition.joinsWithOr
                    return true
                }
                if (inRect(mouseX, mouseY, condition.handleBox.x, condition.handleBox.y, condition.handleBox.width, condition.handleBox.height)) {
                    draggingCondition = step to condition
                    dragY = mouseY.toInt()
                    return true
                }
                if (condition.kindSelector.mouseClicked(mouseButtonEvent, doubled)) return true
                if (condition.cropSelector.mouseClicked(mouseButtonEvent, doubled)) return true
                if (condition.amountField.mouseClicked(mouseButtonEvent, doubled)) return true
            }
        }
        return isMouseOver(mouseX, mouseY)
    }

    fun mouseReleased(mouseX: Double, mouseY: Double) {
        val (step, condition) = draggingCondition ?: return
        draggingCondition = null
        val from = step.conditions.indexOf(condition)
        if (from < 0) return
        val to = step.conditions.indexOfFirst { mouseY < it.handleBox.y + it.handleBox.height / 2 }.let { if (it < 0) step.conditions.lastIndex else it }
        if (to == from) return
        step.conditions.removeAt(from)
        step.conditions.add(to, condition)
        step.conditions.first().joinsWithOr = false
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = textFields().any { it.charTyped(characterEvent) }

    override fun keyPressed(keyEvent: KeyEvent): Boolean = textFields().any { it.keyPressed(keyEvent) }

    private fun textFields(): List<TextField> = steps.flatMap { step -> listOf(step.nameField) + step.conditions.map { it.amountField } }

    private fun save() {
        val firstContents = plot.freshCopy()
        var previousContents: PlotLayout = firstContents
        val newSteps = steps.mapIndexed { index, edited ->
            val contents = when {
                edited.layout === plot -> firstContents
                edited.layout != null -> edited.layout
                else -> previousContents
            }
            val layout = if (index == 0) {
                if (contents !== plot) plot.copyContentsFrom(contents)
                plot
            } else if (edited.layout != null && edited.layout !== plot) {
                edited.layout
            } else {
                PlotLayout(id = plot.stepId(index), name = edited.nameField.value.takeIf { it.isNotBlank() }).also { it.copyContentsFrom(contents) }
            }
            previousContents = contents
            if (index > 0) layout.name = edited.nameField.value.takeIf { it.isNotBlank() }
            FlowStep(edited.nameField.value.takeIf { it.isNotBlank() }, layout, edited.exits(), edited.policies())
        }
        plot.flow = if (newSteps.size == 1 && newSteps.first().exits.isEmpty()) null else PlotFlow(loopCheckbox.isChecked, newSteps.toMutableList())
        onSaved()
    }

    companion object {
        private const val MAX_WIDTH: Int = 780
        private const val PADDING: Int = 10
        private const val TITLE_HEIGHT: Int = 14
        private const val HEADER_HEIGHT: Int = 24
        private const val CONDITION_HEIGHT: Int = 22
        private const val FIELD_HEIGHT: Int = 16
        private const val SQUARE: Int = 16
        private const val ROW_INSET: Int = 4
        private const val CONDITION_INSET: Int = 12
        private const val FOLD_WIDTH: Int = 10
        private const val STEP_NUMBER_WIDTH: String = "99"
        private const val NAME_WIDTH: Int = 110
        private const val HANDLE_WIDTH: Int = 10
        private const val JOIN_WIDTH: Int = 30
        private const val KIND_WIDTH: Int = 150
        private const val CROP_WIDTH: Int = 130
        private const val AMOUNT_WIDTH: Int = 36
        private const val STEP_GAP: Int = 6
        private const val CHECKBOX_SIZE: Int = 10
        private const val LOOP_LABEL: String = "Loop back to step 1"
        private const val HINT: String = "Layouts are painted in the grid; this edits the order, the names, the conditions and the policies."
        private const val FULL_CLEAR_LABEL: String = "full clear"
        private const val KEEP_IDENTICAL_LABEL: String = "keep identical"
        private const val HARVEST_INPUTS_LABEL: String = "harvest input spawns"
        private const val NO_EXIT: String = "no exit"
        private const val WHEN_LABEL: String = "when"
        private const val AND_LABEL: String = "and"
        private const val OR_LABEL: String = "or"
        private const val HANDLE: String = "⋮⋮"
    }
}
