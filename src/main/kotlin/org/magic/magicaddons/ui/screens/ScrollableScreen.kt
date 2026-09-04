package org.magic.magicaddons.ui.screens

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.roundToInt

/**
 * A screen whose content may be larger than the window. The content is drawn shifted by the scroll
 * and every mouse event reaches it in content coordinates, so widgets never know about scrolling.
 */
abstract class ScrollableScreen(title: Component) : Screen(title) {

    var scrollX: Int = 0
    var scrollY: Int = 0

    /** How big the content is, measured by the subclass after it laid itself out. */
    abstract val contentWidth: Int
    abstract val contentHeight: Int

    private val barThickness = 3
    private var draggingVertical = false
    private var draggingHorizontal = false

    /** The edges of what is on screen, in content coordinates. */
    val viewLeft: Int get() = scrollX
    val viewTop: Int get() = scrollY
    val viewRight: Int get() = scrollX + width
    val viewBottom: Int get() = scrollY + height

    private val maxScrollX: Int get() = (contentWidth - width).coerceAtLeast(0)
    private val maxScrollY: Int get() = (contentHeight - height).coerceAtLeast(0)

    /** Content size from its far edge: padding is added only once the edge is already off screen. */
    fun extentFor(edge: Int, visible: Int): Int = if (edge > visible) edge + sidePadding else edge

    fun clampScroll() {
        scrollX = scrollX.coerceIn(0, maxScrollX)
        scrollY = scrollY.coerceIn(0, maxScrollY)
    }

    /** A gap of [units] on a screen of the reference height, scaled to this screen's height. */
    fun scaled(units: Int): Int =
        (units * height / Common.UI.LAYOUT_REFERENCE_HEIGHT.toFloat()).roundToInt().coerceAtLeast(1)

    /** Side space that grows with the screen, so wide windows do not press content to the edge. */
    val sidePadding: Int
        get() = (width * Common.UI.SCREEN_SIDE_PAD_FRACTION).toInt().coerceAtLeast(Common.UI.SCREEN_SIDE_PAD_MIN)

    /** Width for each of [count] columns sharing the screen, never below [minWidth] nor above the cap. */
    fun columnWidth(count: Int, spacing: Int, minWidth: Int): Int {
        if (count <= 0) return 0
        val usable = width - sidePadding * 2 - (count - 1) * spacing
        return (usable / count).coerceAtMost(Common.UI.COLUMN_MAX_WIDTH).coerceAtLeast(minWidth)
    }

    /** Where a row of columns of [totalWidth] starts: centred when it fits, else at the side padding. */
    fun columnsStartX(totalWidth: Int): Int =
        ((width - totalWidth) / 2).coerceAtLeast(sidePadding)

    /** Everything that scrolls, in content coordinates. */
    abstract fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)

    /** Whatever stays put over the content, such as tooltips, in screen coordinates. */
    open fun extractFixed(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        clampScroll()

        graphics.pose().pushMatrix()
        graphics.pose().translate(-scrollX.toFloat(), -scrollY.toFloat())
        extractContent(graphics, mouseX + scrollX, mouseY + scrollY, delta)
        graphics.pose().popMatrix()

        drawScrollBars(graphics)
        extractFixed(graphics, mouseX, mouseY, delta)
    }

    private fun drawScrollBars(graphics: GuiGraphicsExtractor) {
        if (maxScrollY > 0) {
            val barX = width - barThickness
            val barHeight = (height * height / contentHeight).coerceAtLeast(6)
            val barY = (height - barHeight) * scrollY / maxScrollY

            graphics.fill(barX, 0, width, height, 0x40000000)
            graphics.fill(barX, barY, width, barY + barHeight, Common.UI.TEXT_COLOR)
        }
        if (maxScrollX > 0) {
            val barY = height - barThickness
            val barWidth = (width * width / contentWidth).coerceAtLeast(6)
            val barX = (width - barWidth) * scrollX / maxScrollX

            graphics.fill(0, barY, width, height, 0x40000000)
            graphics.fill(barX, barY, barX + barWidth, height, Common.UI.TEXT_COLOR)
        }
    }

    private fun shifted(event: MouseButtonEvent): MouseButtonEvent =
        MouseButtonEvent(event.x + scrollX, event.y + scrollY, MouseButtonInfo(event.button(), event.modifiers()))

    open fun contentMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = false
    open fun contentMouseReleased(event: MouseButtonEvent): Boolean = false
    open fun contentMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean = false
    open fun contentMouseMoved(mouseX: Double, mouseY: Double) {}
    open fun contentMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean = false

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == 0) {
            draggingVertical = maxScrollY > 0 && event.x >= width - barThickness
            draggingHorizontal = !draggingVertical && maxScrollX > 0 && event.y >= height - barThickness
            if (draggingVertical || draggingHorizontal) return true
        }
        return contentMouseClicked(shifted(event), doubled)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (draggingVertical || draggingHorizontal) {
            draggingVertical = false
            draggingHorizontal = false
            return true
        }
        return contentMouseReleased(shifted(event))
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (draggingVertical) {
            scrollY = ((event.y / height) * contentHeight - height / 2).toInt()
            clampScroll()
            return true
        }
        if (draggingHorizontal) {
            scrollX = ((event.x / width) * contentWidth - width / 2).toInt()
            clampScroll()
            return true
        }
        return contentMouseDragged(shifted(event), dragX, dragY)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        contentMouseMoved(mouseX + scrollX, mouseY + scrollY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (contentMouseScrolled(mouseX + this.scrollX, mouseY + this.scrollY, scrollX, scrollY)) return true

        val shift = InputConstants.isKeyDown(Minecraft.getInstance().window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(Minecraft.getInstance().window, GLFW.GLFW_KEY_RIGHT_SHIFT)
        val sideways = if (shift) scrollY else scrollX

        if (sideways != 0.0) {
            this.scrollX -= (sideways * Common.UI.SCROLL_STEP).toInt()
        } else {
            this.scrollY -= (scrollY * Common.UI.SCROLL_STEP).toInt()
        }
        clampScroll()
        return true
    }

    companion object {
        /** The screen being shown, when it is one of these. */
        fun current(): ScrollableScreen? = McCompat.currentScreen() as? ScrollableScreen
    }
}
