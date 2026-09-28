package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.PresetLibrarySetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ButtonPairContext
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.UnsavedChangesContext

class PresetLibrarySettingWidget(
    private val setting: PresetLibrarySetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    private val presetSelector = DropdownWidget(
        values = setting.presetNames(),
        currentValue = setting.value,
        overlayContext = overlays,
        onValueChanged = { picked -> pickPreset(picked) }
    ).apply {
        height = FIELD_HEIGHT
        fitToValues(SELECTOR_WIDTH)
    }

    private val nameBox = TextField(NAME_WIDTH, FIELD_HEIGHT, Component.literal("Name…")).apply {
        value = setting.value
        setMaxLength(NAME_MAX_LENGTH)
    }

    private val saveButton = ClickableButtonWidget(BUTTON_WIDTH, FIELD_HEIGHT, Component.literal("Save"))
    private val deleteButton = ClickableButtonWidget(BUTTON_WIDTH, FIELD_HEIGHT, Component.literal("Delete"))

    private fun pickPreset(picked: String) {
        if (!setting.settingsDirty()) {
            switchToPreset(picked)
            return
        }

        presetSelector.currentValue = "${setting.value}$EDITED_MARK"

        val saveTarget = setting.savePreset()
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            presetSelector.x,
            presetSelector.y + presetSelector.height,
            UnsavedChangesContext.widthFor(saveTarget),
            ButtonPairContext.ONE_LINE_PANEL_HEIGHT
        )
        overlays.addContext(
            UnsavedChangesContext(
                menuX, menuY, saveTarget, overlays,
                onDiscard = { switchToPreset(picked) },
                onSave = {
                    setting.save(saveTarget)
                    switchToPreset(picked)
                }
            )
        )
    }

    private fun switchToPreset(picked: String) {
        setting.applyPreset(picked)
        nameBox.value = picked
    }

    override val controlWidth: Int
        get() = presetSelector.width + nameBox.width + BUTTON_WIDTH * 2 + Common.UI.SPACING * 3

    override val controlHeight: Int = FIELD_HEIGHT

    override fun layoutControl() {
        presetSelector.values = setting.presetNames()
        presetSelector.currentValue = if (setting.settingsDirty()) "${setting.value}$EDITED_MARK" else setting.value

        presetSelector.x = controlLeft()
        presetSelector.y = controlTop()

        nameBox.x = presetSelector.x + presetSelector.width + Common.UI.SPACING
        nameBox.y = controlTop()

        saveButton.x = nameBox.x + nameBox.width + Common.UI.SPACING
        saveButton.y = controlTop()

        deleteButton.x = saveButton.x + BUTTON_WIDTH + Common.UI.SPACING
        deleteButton.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        presetSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        nameBox.render(graphics)
        saveButton.extractRenderState(graphics, mouseX, mouseY, delta)
        deleteButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (presetSelector.mouseClicked(event, doubled)) return true
        if (nameBox.mouseClicked(event, doubled)) return true

        if (saveButton.mouseClicked(event, doubled)) {
            val typedName = nameBox.value.trim()

            if (typedName in setting.configSettingPresets) {
                askToConfirm(event, "Overwrite $typedName?") { setting.save(typedName) }
            } else {
                setting.save(typedName)
            }
            return true
        }

        if (deleteButton.mouseClicked(event, doubled)) {
            val picked = setting.value

            askToConfirm(event, "Delete $picked?") {
                setting.delete(picked)
                nameBox.value = setting.value
            }
            return true
        }

        return false
    }

    private fun askToConfirm(event: MouseButtonEvent, question: String, onYes: () -> Unit) {
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            event.x.toInt(),
            event.y.toInt(),
            ConfirmContext.widthFor(question),
            ConfirmContext.heightFor()
        )

        overlays.addContext(ConfirmContext(menuX, menuY, question, overlays, onConfirm = onYes))
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        presetSelector.mouseMoved(mouseX, mouseY)
        saveButton.mouseMoved(mouseX, mouseY)
        deleteButton.mouseMoved(mouseX, mouseY)
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        presetSelector.list.charTyped(event) || nameBox.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean =
        presetSelector.list.keyPressed(event) || nameBox.keyPressed(event) || super.keyPressed(event)

    override fun dropFocus() {
        super.dropFocus()
        nameBox.isFocused = false
    }

    private companion object {
        const val SELECTOR_WIDTH: Int = 90
        const val NAME_WIDTH: Int = 70
        const val BUTTON_WIDTH: Int = 44
        const val NAME_MAX_LENGTH: Int = 24

        const val EDITED_MARK: String = " *"
    }
}
