package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget

/**
 * What the player can do to the greenhouse they are looking at.
 *
 * Only one thing so far, and a panel holding one button is barely a panel. It is one anyway because
 * the alternative is what was there before: a button laid out, drawn and clicked in three separate
 * corners of the screen, which is how the last one ended up sitting on top of the selector.
 */
class GreenhousePanel(
    private val onUnplan: () -> Unit
) : ActionPanel() {

    private val unplanButton = ClickableButtonWidget(
        70,
        26,
        Component.literal("Unplan")
    )

    /** Whether there is anything to do here, which today is whether a planner is running. */
    var showUnplan: Boolean = false

    override val buttons: List<ClickableButtonWidget> = listOf(unplanButton)

    override fun isShown(button: ClickableButtonWidget): Boolean = showUnplan

    override fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean {
        if (button === unplanButton) {
            onUnplan()
            return true
        }

        return false
    }
}
