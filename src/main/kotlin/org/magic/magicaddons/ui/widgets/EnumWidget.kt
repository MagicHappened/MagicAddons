package org.magic.magicaddons.ui.widgets

import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class EnumWidget<T>(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var values: List<T>,
    var currentValue: T?,
    val overlayContext: OverlayContext,
    val includeSearch: Boolean = false,
    val onLeftClickValue: ((T?, MouseButtonEvent) -> Unit)? = null,
    val onRightClickValue: ((T?, MouseButtonEvent) -> Unit)? = null,
    val valueChanged: ((T) -> Unit)? = null,
    ) : Renderable, Focusable {
    val overlay = EnumOverlay(1)

    /**
     * The gap between the border and what sits inside it, on both sides.
     *
     * Wider than the four the text used to get, which left the name all but touching the border
     * while the flip beside it breathed, and the two sat side by side looking like two different
     * widgets.
     */
    private val TEXT_PAD: Int = 6

    /** Narrow enough to still look like a selector when every value is a short word. */
    private val MIN_WIDTH: Int = 60

    private val ARROW: String = "↓"
    private val ELLIPSIS: String = "…"

    val font = Minecraft.getInstance().font
    var overlayOpen = false

    override var focusedState: Boolean = false

    private fun valueChanged(newValue: T) {
        currentValue = newValue
        overlay.valueWidgets.clear()
        overlayOpen = false
        valueChanged?.invoke(newValue)
    }


    /**
     * Sets the width from the longest value it might have to show, up to [maxWidth].
     *
     * The caller used to pick a number and the widget cut whatever did not fit, so a name was
     * shortened because of a guess made before anyone knew what the names were. It measures now,
     * and the ellipsis is kept for the one case that is really about room: a name too long for the
     * screen rather than too long for a number somebody typed.
     */
    fun fitToValues(maxWidth: Int) {
        val shown = values.map { it.toString() } + listOfNotNull(currentValue?.toString())
        val longest = shown.maxOfOrNull { font.width(it) } ?: 0

        width = (longest + TEXT_PAD * 2 + font.width(ARROW) + Common.UI.SPACING)
            .coerceIn(MIN_WIDTH, maxWidth.coerceAtLeast(MIN_WIDTH))
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        graphics.fill(x, y, x + width, y + height, Common.UI.BACKGROUND_COLOR)
        graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

        val textY = y + (height - font.lineHeight) / 2

        // the arrow keeps to the far side and the name is given what is left, so a long name runs
        // out of room before it runs into the arrow rather than under it
        val arrowWidth = font.width(ARROW)
        val room = width - TEXT_PAD * 2 - arrowWidth - Common.UI.SPACING

        val name = currentValue?.toString() ?: "Select…"
        val shown = if (font.width(name) <= room) {
            name
        } else {
            font.plainSubstrByWidth(name, room - font.width(ELLIPSIS)) + ELLIPSIS
        }

        graphics.text(
            font,
            Component.literal(shown),
            x + TEXT_PAD,
            textY,
            Common.UI.TEXT_COLOR,
            false
        )

        graphics.text(
            font,
            Component.literal(ARROW),
            x + width - arrowWidth - TEXT_PAD,
            textY,
            Common.UI.TEXT_COLOR,
            false
        )
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            if (mouseButtonEvent.button() == 0) {
                if (!overlayOpen){
                    overlay.searchText = ""
                    overlay.rebuildRows()
                    overlayOpen = true
                    overlayContext.addOverlay(overlay)
                }
                else {
                    overlayOpen = false
                    overlayContext.removeOverlay(overlay)
                }
                onLeftClickValue?.invoke(currentValue, mouseButtonEvent)
                return true
            } else if (mouseButtonEvent.button() == 1) {
                val handler = onRightClickValue ?: return false

                handler.invoke(currentValue, mouseButtonEvent)
            }
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x until x + width &&
                mouseY.toInt() in y until y + height
    }



    inner class EnumOverlay(override val renderPriority: Int) : OverlayRenderable, Focusable {

        // closing the overlay any other way, such as a click landing outside it, would otherwise
        // leave the widget believing it is still open and swallow the next click on it
        override fun onClosed() {
            overlayOpen = false
        }

        override var focusedState: Boolean = false

        override var hoveredElement: GuiEventListener? = null

        /**
         * A row is as tall as the closed selector, so the list reads as the same control opened
         * rather than a smaller one underneath it.
         */
        val overlayRowHeight: Int
            get() = this@EnumWidget.height

        val valueWidgets: MutableList<ClickableRowWidget<T>> = mutableListOf()

        /**
         * What has been typed so far, narrowing the list to values that start with it.
         *
         * Prefix rather than contains: the ask was typing M to see the crops starting with M,
         * and a contains-match surfaces every crop with an m somewhere in it instead.
         */
        var searchText: String = ""

        /** Builds the rows from whatever the search currently lets through. */
        fun rebuildRows() {
            valueWidgets.clear()

            values.forEach { value ->
                if (value == currentValue) return@forEach
                if (!value.toString().startsWith(searchText, ignoreCase = true)) return@forEach

                valueWidgets.add(ClickableRowWidget(value))
            }

            layoutOverlay()
        }

        override val overlayX: Int
            get() = this@EnumWidget.x
        /**
         * Under the selector, or above it when the screen runs out underneath. A list that falls
         * off the bottom is a list whose last values cannot be picked at all.
         */
        override val overlayY: Int
            get() {
                val below = this@EnumWidget.y + this@EnumWidget.height
                val room = Minecraft.getInstance().window.guiScaledHeight

                return if (below + overlayHeight <= room) {
                    below
                } else {
                    (this@EnumWidget.y - overlayHeight).coerceAtLeast(0)
                }
            }
        override val overlayWidth: Int
            get() = this@EnumWidget.width
        override val overlayHeight: Int
            get() = overlayRowHeight * (valueWidgets.size + if (includeSearch) 1 else 0)




        fun layoutOverlay() {
            var currentY = overlayY

            // the first row's height is the search box's; drawn in renderOverlay, it only needs
            // the rows below it to start one row further down
            if (includeSearch) currentY += overlayRowHeight

            valueWidgets.forEach {
                it.x = overlayX
                it.y = currentY
                it.width = overlayWidth
                it.height = overlayRowHeight
                currentY += overlayRowHeight
            }
        }

        override fun renderOverlay(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ) {
            if (includeSearch) {
                val font = Minecraft.getInstance().font
                val boxY = overlayY

                graphics.fill(overlayX, boxY, overlayX + overlayWidth, boxY + overlayRowHeight, Common.UI.BACKGROUND_COLOR)
                graphics.drawBorder(
                    overlayX, boxY,
                    overlayX + overlayWidth, boxY + overlayRowHeight,
                    1, Common.UI.BORDER_COLOR
                )
                graphics.text(
                    font,
                    Component.literal(searchText + "_"),
                    overlayX + TEXT_PAD,
                    boxY + (overlayRowHeight - font.lineHeight) / 2,
                    Common.UI.TEXT_COLOR,
                    false
                )
            }

            valueWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
        }

        override fun charTyped(characterEvent: CharacterEvent): Boolean {
            if (!includeSearch) return false

            searchText += characterEvent.codepointAsString()
            rebuildRows()
            return true
        }

        override fun keyPressed(keyEvent: KeyEvent): Boolean {
            if (!includeSearch) return false

            if (keyEvent.key == InputConstants.KEY_BACKSPACE && searchText.isNotEmpty()) {
                searchText = searchText.dropLast(1)
                rebuildRows()
                return true
            }

            return false
        }

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
            valueWidgets.forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) {
                    if (mouseButtonEvent.button() == 0) {
                        this@EnumWidget.onLeftClickValue?.invoke(it.value, mouseButtonEvent)
                        this@EnumWidget.valueChanged(it.value)
                        return true
                    } else if (mouseButtonEvent.button() == 1) {
                        this@EnumWidget.onRightClickValue?.invoke(it.value, mouseButtonEvent)
                        return true
                    }


                }
            }
            return false
        }

        override fun mouseMoved(mouseX: Double, mouseY: Double) {
            hoveredElement = null
            valueWidgets.forEach {
                it.mouseMoved(mouseX, mouseY)
                if (hoveredElement == null){
                    if (it.isMouseOverRow(mouseX, mouseY)) {
                        hoveredElement = it
                    }
                }

            }
        }



    }
}
