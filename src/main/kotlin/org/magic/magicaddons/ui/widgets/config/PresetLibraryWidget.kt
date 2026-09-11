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
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.UnsavedChangesContext

/**
 * The saved looks: a list to pick one from, a box to name one, and buttons to keep or drop it. The
 * name box shows the picked preset, so saving over it is typing nothing and pressing Save.
 */
class PresetLibraryWidget(
    private val setting: PresetLibrarySetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    private val selector = EnumWidget(
        values = setting.names(),
        currentValue = setting.value,
        overlayContext = overlays,
        valueChanged = { picked -> pick(picked) }
    ).apply {
        height = FIELD_HEIGHT
        fitToValues(SELECTOR_WIDTH)
    }

    private val name = TextField(NAME_WIDTH, FIELD_HEIGHT, Component.literal("Name…")).apply {
        value = setting.value
        setMaxLength(NAME_LIMIT)
    }

    private val saveButton = ClickableButtonWidget(BUTTON_WIDTH, FIELD_HEIGHT, Component.literal("Save"))
    private val deleteButton = ClickableButtonWidget(BUTTON_WIDTH, FIELD_HEIGHT, Component.literal("Delete"))

    /** Switches preset, asking what to do with edits to the current one first. */
    private fun pick(picked: String) {
        if (!setting.edited()) {
            switchTo(picked)
            return
        }

        // the list has already shown the pick, so it is put back until the question is answered
        selector.currentValue = "${setting.value}$EDITED"

        val saveTo = setting.saveTarget()
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            selector.x,
            selector.y + selector.height,
            UnsavedChangesContext.widthFor(saveTo),
            UnsavedChangesContext.HEIGHT
        )
        overlays.addContext(
            UnsavedChangesContext(
                menuX, menuY, saveTo, overlays,
                onDiscard = { switchTo(picked) },
                onSave = {
                    setting.save(saveTo)
                    switchTo(picked)
                }
            )
        )
    }

    private fun switchTo(picked: String) {
        setting.apply(picked)
        name.value = picked
    }

    override val controlWidth: Int
        get() = selector.width + name.width + BUTTON_WIDTH * 2 + Common.UI.SPACING * 3

    override val controlHeight: Int = FIELD_HEIGHT

    override fun layoutControl() {
        setting.rememberShipped()
        selector.values = setting.names()

        // the list says which look is on, and an edited one is marked so it is not mistaken for saved
        selector.currentValue = if (setting.edited()) "${setting.value}$EDITED" else setting.value

        selector.x = controlLeft()
        selector.y = controlTop()

        name.x = selector.x + selector.width + Common.UI.SPACING
        name.y = controlTop()

        saveButton.x = name.x + name.width + Common.UI.SPACING
        saveButton.y = controlTop()

        deleteButton.x = saveButton.x + BUTTON_WIDTH + Common.UI.SPACING
        deleteButton.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        selector.extractRenderState(graphics, mouseX, mouseY, delta)
        name.render(graphics)
        saveButton.extractRenderState(graphics, mouseX, mouseY, delta)
        deleteButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (selector.mouseClicked(event, doubled)) return true
        if (name.mouseClicked(event, doubled)) return true

        if (saveButton.mouseClicked(event, doubled)) {
            val wanted = name.value.trim()

            // writing over one already saved is worth asking about; a new name is not
            if (wanted in setting.presets) {
                ask(event, "Overwrite $wanted?") { setting.save(wanted) }
            } else {
                setting.save(wanted)
            }
            return true
        }

        if (deleteButton.mouseClicked(event, doubled)) {
            val picked = setting.value

            ask(event, "Delete $picked?") {
                setting.delete(picked)
                name.value = setting.value
            }
            return true
        }

        return false
    }

    /** The yes or no put where the button was pressed, so it is read next to what it is about. */
    private fun ask(event: MouseButtonEvent, question: String, onYes: () -> Unit) {
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            event.x.toInt(),
            event.y.toInt(),
            ConfirmContext.widthFor(question),
            ConfirmContext.HEIGHT
        )

        overlays.addContext(ConfirmContext(menuX, menuY, question, overlays, onYes))
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        selector.mouseMoved(mouseX, mouseY)
        saveButton.mouseMoved(mouseX, mouseY)
        deleteButton.mouseMoved(mouseX, mouseY)
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        selector.overlay.charTyped(event) || name.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean =
        selector.overlay.keyPressed(event) || name.keyPressed(event) || super.keyPressed(event)

    override fun dropFocus() {
        super.dropFocus()
        name.focused = false
    }

    private companion object {
        const val SELECTOR_WIDTH: Int = 90
        const val NAME_WIDTH: Int = 70
        const val BUTTON_WIDTH: Int = 44
        const val NAME_LIMIT: Int = 24

        /** Put after the name of a preset whose settings have been changed since it was saved. */
        const val EDITED: String = " *"
    }
}
