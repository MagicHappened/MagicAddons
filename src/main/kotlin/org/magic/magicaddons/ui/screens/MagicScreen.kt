package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.util.ErrorReporter

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

    final override fun init() = caught(Unit) { onInit() }
    open fun onInit() = super.init()

    final override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) =
        caught(Unit) { onRender(graphics, mouseX, mouseY, delta) }
    open fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = super.extractRenderState(graphics, mouseX, mouseY, delta)

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
