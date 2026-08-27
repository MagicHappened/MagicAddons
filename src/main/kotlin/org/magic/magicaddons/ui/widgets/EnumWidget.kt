package org.magic.magicaddons.ui.widgets

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
    val font = Minecraft.getInstance().font
    var overlayOpen = false

    override var focusedState: Boolean = false

    private fun valueChanged(newValue: T) {
        currentValue = newValue
        overlay.valueWidgets.clear()
        overlayOpen = false
        valueChanged?.invoke(newValue)
    }


    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        graphics.fill(x, y, x + width, y + height, Common.UI.BACKGROUND_COLOR)
        graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

        graphics.text(
            font,
            Component.literal(currentValue.toString()),
            x + Common.UI.TEXT_X_PAD,
            y + (height - font.lineHeight) / 2,
            Common.UI.TEXT_COLOR,
            false
        )

        graphics.text(
            font,
            Component.literal("↓"),
            x + width - font.width("↓") - 4,
            y + (height - font.lineHeight) / 2,
            Common.UI.TEXT_COLOR,
            false
        )

    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            if (mouseButtonEvent.button() == 0) {
                if (!overlayOpen){
                    overlay.valueWidgets.clear()
                    values.forEach { value ->
                        if (value == currentValue) return@forEach
                        val widget = ClickableRowWidget(
                            value
                        )
                        overlay.valueWidgets.add(widget)
                    }
                    overlay.layoutOverlay()
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

        val overlayRowHeight: Int
            get() = (this@EnumWidget.height * 0.8f).toInt()

        val valueWidgets: MutableList<ClickableRowWidget<T>> = mutableListOf()

        override val overlayX: Int
            get() = this@EnumWidget.x
        override val overlayY: Int
            get() = this@EnumWidget.y + this@EnumWidget.height
        override val overlayWidth: Int
            get() = this@EnumWidget.width
        override val overlayHeight: Int
            get() = overlayRowHeight * valueWidgets.size




        fun layoutOverlay() {
            var currentY = overlayY
            if (includeSearch) {
                TODO("Not yet implemented")
            }

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
            valueWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
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
