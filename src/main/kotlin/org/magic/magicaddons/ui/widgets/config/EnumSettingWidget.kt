package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.EnumWidget

/**
 * A value picked from a fixed set: a selector on the right of the row. The settings under it are
 * its fixed ones and whatever the picked value brings, rebuilt when the value changes.
 */
class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>,
    overlays: OverlayContext
) : SettingWidget<T>(setting, overlays) {

    private val selector = EnumWidget(
        values = setting.value.javaClass.enumConstants.toList(),
        currentValue = setting.value,
        overlayContext = overlays,
        valueChanged = { picked ->
            if (setting.value != picked) {
                setting.value = picked
                if (childrenWidgets.isNotEmpty()) buildChildren()
                if (hasChildren()) unfold(true)
            }
        },
        searchable = setting.value.javaClass.enumConstants.size > SEARCH_FROM
    ).apply {
        height = FIELD_HEIGHT
        fitToValues(MAX_WIDTH)
    }

    override val controlWidth: Int get() = selector.width
    override val controlHeight: Int = FIELD_HEIGHT

    override fun childNodes(): List<SettingNode<*>> = setting.children.orEmpty() + setting.providedChildren

    override fun layoutControl() {
        selector.x = controlLeft()
        selector.y = controlTop()
        selector.currentValue = setting.value
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

        /** A list of fewer values than this has nothing worth searching. */
        const val SEARCH_FROM: Int = 8
    }
}
