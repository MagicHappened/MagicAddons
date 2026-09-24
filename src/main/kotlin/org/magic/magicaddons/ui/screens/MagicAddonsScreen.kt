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
import org.magic.magicaddons.ui.fonts.ModFont
import org.magic.magicaddons.util.ErrorReporter
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.withAlpha
import org.magic.magicaddons.util.compat.McCompat

abstract class MagicAddonsScreen(title: Component, private val errorLocation: String) : Screen(title) {

    private inline fun <T> reportErrorsInModFont(fallback: T, block: () -> T): T =
        try {
            ModFont.replaceDefaultFont(block)
        } catch (error: Throwable) {
            ErrorReporter.report(errorLocation, error)
            fallback
        }

    private var openedAt: Long = 0L
    private var closingSince: Long = 0L

    private fun animationProgress(): Float =
        if (closingSince != 0L) 1f - eased(closingSince, ANIMATION_MS) else eased(openedAt, ANIMATION_MS)

    final override fun init() = reportErrorsInModFont(Unit) {
        if (openedAt == 0L) openedAt = System.currentTimeMillis()
        onInit()
    }
    open fun onInit() = super.init()

    final override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) =
        reportErrorsInModFont(Unit) {
            if (closingSince != 0L && System.currentTimeMillis() - closingSince >= ANIMATION_MS) {
                onCloseFinished()
                return@reportErrorsInModFont
            }

            val animationScale = SCALE_AT_OPEN + (1f - SCALE_AT_OPEN) * animationProgress()
            graphics.pose().pushMatrix()
            graphics.pose().translate(width / 2f, height / 2f)
            graphics.pose().scale(animationScale, animationScale)
            graphics.pose().translate(-width / 2f, -height / 2f)
            onRender(graphics, mouseX, mouseY, delta)
            graphics.pose().popMatrix()
        }

    open fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = super.extractRenderState(graphics, mouseX, mouseY, delta)

    final override fun onClose() {
        if (closingSince == 0L) closingSince = System.currentTimeMillis()
    }

    open fun onCloseFinished() = super.onClose()

    open val backgroundImageName: String? = null

    final override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) =
        reportErrorsInModFont(Unit) { onExtractBackground(graphics, mouseX, mouseY, delta) }

    open fun onExtractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (minecraft.level == null) {
            extractPanorama(graphics, delta)
        }
        graphics.fill(0, 0, width, height, withAlpha(Common.UI.SCREEN_DIM_COLOR, animationProgress()))

        backgroundImageName?.takeIf { Customization.backgroundShowsOn(it) }?.let {
            ConfigBackground.draw(graphics, 0, 0, width, height)
        }

        McCompat.extractDeferredSubtitles(minecraft)
    }

    final override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = reportErrorsInModFont(false) { onMouseClicked(event, doubled) }
    open fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = super.mouseClicked(event, doubled)

    final override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean = reportErrorsInModFont(false) { onMouseDragged(event, dragX, dragY) }
    open fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean = super.mouseDragged(event, dragX, dragY)

    final override fun mouseReleased(event: MouseButtonEvent): Boolean = reportErrorsInModFont(false) { onMouseReleased(event) }
    open fun onMouseReleased(event: MouseButtonEvent): Boolean = super.mouseReleased(event)

    final override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        reportErrorsInModFont(false) { onMouseScrolled(mouseX, mouseY, scrollX, scrollY) }
    open fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)

    final override fun mouseMoved(mouseX: Double, mouseY: Double) = reportErrorsInModFont(Unit) { onMouseMoved(mouseX, mouseY) }
    open fun onMouseMoved(mouseX: Double, mouseY: Double) = super.mouseMoved(mouseX, mouseY)

    final override fun charTyped(characterEvent: CharacterEvent): Boolean = reportErrorsInModFont(false) { onCharTyped(characterEvent) }
    open fun onCharTyped(event: CharacterEvent): Boolean = super.charTyped(event)

    final override fun keyPressed(keyEvent: KeyEvent): Boolean = reportErrorsInModFont(false) { onKeyPressed(keyEvent) }
    open fun onKeyPressed(event: KeyEvent): Boolean = super.keyPressed(event)

    private companion object {
        const val ANIMATION_MS: Long = 150

        const val SCALE_AT_OPEN: Float = 0.75f
    }
}
