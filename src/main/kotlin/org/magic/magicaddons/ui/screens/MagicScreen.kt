package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.ui.background.ConfigBackground
import org.magic.magicaddons.util.ErrorReporter
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.withAlpha
import org.magic.magicaddons.util.compat.McCompat

/**
 * A screen of this mod. The game's calls into it are taken here and handed on to the `on` methods,
 * so an error in a screen is reported in chat instead of taking the game down.
 */
abstract class MagicScreen(title: Component, private val where: String) : Screen(title) {

    private inline fun <T> caught(fallback: T, block: () -> T): T =
        try {
            block()
        } catch (error: Throwable) {
            ErrorReporter.report(where, error)
            fallback
        }

    /** When the screen came up, and when it was asked to go, for its panels to grow in and shrink out. */
    private var openedAt: Long = 0L
    private var closingSince: Long = 0L

    /** How far the panels have come in: from a little small on opening, back down again on closing. */
    private fun openFraction(): Float =
        if (closingSince != 0L) 1f - eased(closingSince, CLOSE_MS) else eased(openedAt, OPEN_MS)

    final override fun init() = caught(Unit) {
        if (openedAt == 0L) openedAt = System.currentTimeMillis()
        onInit()
    }
    open fun onInit() = super.init()

    final override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) =
        caught(Unit) {
            // asked to close, the screen goes after its panels have shrunk away
            if (closingSince != 0L && System.currentTimeMillis() - closingSince >= CLOSE_MS) {
                finishClose()
                return@caught
            }

            val grow = SCALE_FROM + (1f - SCALE_FROM) * openFraction()
            graphics.pose().pushMatrix()
            graphics.pose().translate(width / 2f, height / 2f)
            graphics.pose().scale(grow, grow)
            graphics.pose().translate(-width / 2f, -height / 2f)
            onRender(graphics, mouseX, mouseY, delta)
            graphics.pose().popMatrix()
        }
    open fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = super.extractRenderState(graphics, mouseX, mouseY, delta)

    /** Starts the screen closing; it goes for real once its panels have shrunk away. */
    final override fun onClose() {
        if (closingSince == 0L) closingSince = System.currentTimeMillis()
    }

    /** What closing actually does, once the closing look has run: back to the game, or to a parent. */
    open fun finishClose() = super.onClose()

    /**
     * Which name this screen goes by in the background image location list, or null for one the
     * picture is never drawn behind. The config screen draws its own, inside its settings panel.
     */
    open val backgroundName: String? = null

    /** The panorama when there is no world, then the world dimmed; subtitles keep drawing over it. */
    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, delta)
        }
        // the dim comes up with the panels and goes with them
        graphics.fill(0, 0, width, height, withAlpha(Common.UI.SCREEN_DIM_COLOR, openFraction()))

        backgroundName?.takeIf { Customization.backgroundShowsOn(it) }?.let {
            ConfigBackground.draw(graphics, 0, 0, width, height)
        }

        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    private companion object {
        /** How long the panels take to come in, and to go. */
        const val OPEN_MS: Long = 150
        const val CLOSE_MS: Long = 120

        /** How small the panels start from, as a share of their full size. */
        const val SCALE_FROM: Float = 0.96f
    }

    final override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = caught(false) { onMouseClicked(event, doubled) }
    open fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = super.mouseClicked(event, doubled)

    final override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean = caught(false) { onMouseDragged(event, dragX, dragY) }
    open fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean = super.mouseDragged(event, dragX, dragY)

    final override fun mouseReleased(event: MouseButtonEvent): Boolean = caught(false) { onMouseReleased(event) }
    open fun onMouseReleased(event: MouseButtonEvent): Boolean = super.mouseReleased(event)

    final override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        caught(false) { onMouseScrolled(mouseX, mouseY, scrollX, scrollY) }
    open fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)

    final override fun mouseMoved(mouseX: Double, mouseY: Double) = caught(Unit) { onMouseMoved(mouseX, mouseY) }
    open fun onMouseMoved(mouseX: Double, mouseY: Double) = super.mouseMoved(mouseX, mouseY)

    final override fun charTyped(characterEvent: CharacterEvent): Boolean = caught(false) { onCharTyped(characterEvent) }
    open fun onCharTyped(characterEvent: CharacterEvent): Boolean = super.charTyped(characterEvent)

    final override fun keyPressed(keyEvent: KeyEvent): Boolean = caught(false) { onKeyPressed(keyEvent) }
    open fun onKeyPressed(keyEvent: KeyEvent): Boolean = super.keyPressed(keyEvent)
}
