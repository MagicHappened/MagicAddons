package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.ChoiceSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.DropdownWidget

class ChoiceSettingWidget(
    private val setting: ChoiceSetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    private val optionSelector = DropdownWidget(
        values = setting.options(),
        currentValue = setting.value.takeIf { it.isNotBlank() },
        overlayContext = overlays,
        onValueChanged = { picked -> pickOption(picked) },
        isSearchable = true
    ).apply {
        height = FIELD_HEIGHT
        fitToValues(SELECTOR_MAX_WIDTH)
    }

    private fun refreshOptions() {
        optionSelector.values = setting.options()
        optionSelector.fitToValues(SELECTOR_MAX_WIDTH)
    }

    private fun pickOption(picked: String) {
        val confirmation = setting.confirm?.invoke(picked)

        if (confirmation == null) {
            applyOption(picked)
            return
        }

        optionSelector.currentValue = setting.value.takeIf { it.isNotBlank() }

        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            optionSelector.x,
            optionSelector.y + optionSelector.height,
            ConfirmContext.widthFor(confirmation.question, confirmation.warning),
            ConfirmContext.heightFor(confirmation.warning)
        )
        overlays.addContext(ConfirmContext(menuX, menuY, confirmation.question, overlays, confirmation.warning) { applyOption(picked) })
    }

    private fun applyOption(picked: String) {
        setting.value = picked
        optionSelector.currentValue = picked
        setting.onChosen?.invoke(setting)
    }

    override val controlWidth: Int get() = optionSelector.width
    override val controlHeight: Int = FIELD_HEIGHT

    override fun onGroupOpened() {
        refreshOptions()
    }

    override fun layoutControl() {
        optionSelector.currentValue = setting.value.takeIf { it.isNotBlank() }
        optionSelector.x = controlLeft()
        optionSelector.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        optionSelector.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (optionSelector.isMouseOver(event.x, event.y)) optionSelector.values = setting.options()
        return optionSelector.mouseClicked(event, doubled)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        optionSelector.mouseMoved(mouseX, mouseY)
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        optionSelector.list.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean =
        optionSelector.list.keyPressed(event) || super.keyPressed(event)

    private companion object {
        const val SELECTOR_MAX_WIDTH: Int = 120
    }
}
