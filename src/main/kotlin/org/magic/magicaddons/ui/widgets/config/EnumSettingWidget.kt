package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.DropdownWidget

class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>,
    overlays: OverlayContext
) : SettingWidget<T>(setting, overlays) {

    private val valueSelector = DropdownWidget(
        values = setting.value.javaClass.enumConstants.toList(),
        currentValue = setting.value,
        overlayContext = overlays,
        onValueChanged = { picked ->
            if (setting.value != picked) {
                setting.value = picked
                if (isExpanded || childWidgets.isNotEmpty()) buildChildWidgets()
                if (hasChildren()) unfold(true)
            }
        },
        isSearchable = setting.value.javaClass.enumConstants.size >= MIN_VALUES_FOR_SEARCH,
        rowTooltip = setting.optionDescriptions
    ).apply {
        height = FIELD_HEIGHT
        fitToValues(SELECTOR_MAX_WIDTH)
    }

    override val controlWidth: Int get() = valueSelector.width
    override val controlHeight: Int = FIELD_HEIGHT

    override fun childNodes(): List<SettingNode<*>> = setting.availableChildren + setting.providedChildren

    override fun layoutControl() {
        valueSelector.x = controlLeft()
        valueSelector.y = controlTop()
        valueSelector.currentValue = setting.value
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        valueSelector.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean =
        valueSelector.mouseClicked(event, doubled)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        valueSelector.mouseMoved(mouseX, mouseY)
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        valueSelector.list.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean =
        valueSelector.list.keyPressed(event) || super.keyPressed(event)

    private companion object {
        const val SELECTOR_MAX_WIDTH: Int = 120
        const val MIN_VALUES_FOR_SEARCH: Int = 9
    }
}
