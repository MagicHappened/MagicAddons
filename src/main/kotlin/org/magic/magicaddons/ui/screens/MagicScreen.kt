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


abstract class MagicScreen(title: Component, private val where: String) : Screen(title) {

    private inline fun <T> caught(fallback: T, block: () -> T): T =
        try {
            block()
        } catch (error: Throwable) {
            ErrorReporter.report(where, error)
            fallback
        }

    private var openedAt: Long = 0L
    private var closingSince: Long = 0L

    private fun openFraction(): Float =
        if (closingSince != 0L) 1f - eased(closingSince, CLOSE_MS) else eased(openedAt, OPEN_MS)

    final override fun init() = caught(Unit) {
        if (openedAt == 0L) openedAt = System.currentTimeMillis()
        onInit()
    }
    open fun onInit() = super.init()

    final override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) =
        caught(Unit) {
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

    final override fun onClose() {
        if (closingSince == 0L) closingSince = System.currentTimeMillis()
    }

    /** What closing actually does, once the closing animation has run. */
    open fun finishClose() = super.onClose()

    /**
     the name for the background image selector in the config screen
     */
    open val backgroundName: String? = null

    /** The panorama when there is no world, then the world dimmed; subtitles keep drawing over it. */
    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, delta)
        }
        // the dim comes up with the panels and disappears with them
        graphics.fill(0, 0, width, height, withAlpha(Common.UI.SCREEN_DIM_COLOR, openFraction()))

        backgroundName?.takeIf { Customization.backgroundShowsOn(it) }?.let {
            ConfigBackground.draw(graphics, 0, 0, width, height)
        }

        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    private companion object {
        const val OPEN_MS: Long = 150
        const val CLOSE_MS: Long = 150

        const val SCALE_FROM: Float = 0.75f
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
