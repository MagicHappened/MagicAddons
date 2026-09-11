package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.ChoiceSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.EnumWidget

/** A value picked from a list that changes while the game runs, such as the files in a folder. */
class ChoiceSettingWidget(
    private val setting: ChoiceSetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    private val selector = EnumWidget(
        values = setting.options(),
        currentValue = setting.value.takeIf { it.isNotBlank() },
        overlayContext = overlays,
        valueChanged = { picked -> pick(picked) },
        searchable = true
    ).apply {
        height = FIELD_HEIGHT
        fitToValues(MAX_WIDTH)
    }

    /** Takes the value, or asks first when the setting says this one needs asking about. */
    private fun pick(picked: String) {
        val asked = setting.confirm?.invoke(picked)

        if (asked == null) {
            take(picked)
            return
        }

        // the list has already shown the pick, so it is put back until the question is answered
        selector.currentValue = setting.value.takeIf { it.isNotBlank() }

        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            selector.x,
            selector.y + selector.height,
            ConfirmContext.widthFor(asked.question, asked.warning),
            ConfirmContext.heightFor(asked.question, asked.warning)
        )
        overlays.addContext(ConfirmContext(menuX, menuY, asked.question, overlays, asked.warning) { take(picked) })
    }

    private fun take(picked: String) {
        setting.value = picked
        selector.currentValue = picked
        setting.onChosen?.invoke(setting)
    }

    override val controlWidth: Int get() = selector.width
    override val controlHeight: Int = FIELD_HEIGHT

    override fun layoutControl() {
        // what the list holds is asked for again every layout, so a file added just now shows up
        selector.values = setting.options()
        selector.currentValue = setting.value.takeIf { it.isNotBlank() }
        selector.fitToValues(MAX_WIDTH)
        selector.x = controlLeft()
        selector.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        selector.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean =
        selector.mouseClicked(event, doubled)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        selector.mouseMoved(mouseX, mouseY)
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        selector.overlay.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean =
        selector.overlay.keyPressed(event) || super.keyPressed(event)

    private companion object {
        const val MAX_WIDTH: Int = 120
    }
}
