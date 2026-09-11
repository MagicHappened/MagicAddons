package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.modText

/**
 * What the player can do to the greenhouse they are looking at: which preset plot it is running,
 * and the button that takes it off.
 */
class GreenhousePanel(
    private val onUnplan: () -> Unit,
    /** Picks the preset plot this greenhouse runs, whether or not it runs one already. */
    private val onPickPreset: (MouseButtonEvent) -> Unit,
    /** Lays the plan on this greenhouse a quarter turn further round. */
    private val onTurnPlan: () -> Unit,
    /** Shows the assigned plot in preset mode, ready to edit. */
    private val onEditPreset: () -> Unit,
    /** Keeps what is built here as a preset of its own. */
    private val onSaveAsPreset: () -> Unit
) : ActionPanel() {

    private val unplanButton = ClickableButtonWidget("Remove assigned preset")
    private val assignButton = ClickableButtonWidget("Assign preset")
    private val changeButton = ClickableButtonWidget("Change preset")
    private val turnButton = ClickableButtonWidget("Turn plan \u21bb")
    private val editButton = ClickableButtonWidget("Edit preset")
    private val saveButton = ClickableButtonWidget("Save as preset")

    private val font = Minecraft.getInstance().font

    /** The preset plot this greenhouse runs, null for one running nothing. */
    var assigned: String? = null
        set(value) {
            if (field == value) return

            // the name sits above the button, so the row moves as it comes and goes
            field = value
            layoutIn(x, y, width)
        }

    /** Whether a greenhouse is on screen at all, since the buttons are all about the one that is. */
    var showButtons: Boolean = false
        set(value) {
            if (field == value) return

            field = value
            layoutIn(x, y, width)
        }

    override val buttons: List<ClickableButtonWidget> =
        listOf(assignButton, changeButton, turnButton, editButton, saveButton, unplanButton)

    override fun isShown(button: ClickableButtonWidget): Boolean = when (button) {
        assignButton -> showButtons && assigned == null
        saveButton -> showButtons
        else -> showButtons && assigned != null
    }

    override fun headerHeight(): Int = if (assigned == null) 0 else font.lineHeight + Common.UI.SPACING

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        assigned?.let { name ->
            val room = width - PADDING * 2
            graphics.modText(font, ellipsised(font, name, room), x + PADDING, y + PADDING, Common.UI.TEXT_DIM_COLOR)
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean {
        when (button) {
            unplanButton -> onUnplan()
            assignButton, changeButton -> onPickPreset(event)
            turnButton -> onTurnPlan()
            editButton -> onEditPreset()
            saveButton -> onSaveAsPreset()
            else -> return false
        }

        return true
    }
}
