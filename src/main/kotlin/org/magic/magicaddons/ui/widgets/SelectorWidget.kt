package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class SelectorWidget<T>(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var values: List<T>,
    var currentValue: T?,
    val includeSearch: Boolean = false,
    val onLeftClickValue: ((T) -> Unit)? = null,
    val onRightClickValue: ((T) -> Unit)? = null,
    val valueChanged: ((T) -> Unit)? = null,

) : Renderable, GuiEventListener {
    val overlay = EnumOverlay(0)
    val font = Minecraft.getInstance().font
    var renderOverlay = false

    private fun valueChanged(newValue: T) {
        currentValue = newValue
        overlay.valueWidgets.clear()
        valueChanged?.invoke(newValue)
        renderOverlay = false
    }


    override fun render(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        deltaTick: Float
    ) {
        graphics.fill(x, y, x + width, y + height, Common.UI.BACKGROUND_COLOR)
        graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

        graphics.drawString(
            font,
            Component.literal(currentValue.toString()),
            x + Common.UI.TEXT_X_PAD,
            y + (height - font.lineHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        graphics.drawString(
            font,
            Component.literal("↓"),
            x + width - font.width("↓") - 4,
            y + (height - font.lineHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            if (mouseButtonEvent.button() == 0) {
                if (!renderOverlay) {
                    values.forEach { value ->
                        if (value == currentValue) return@forEach
                        val widget = ClickableRowWidget(
                            value
                        )
                        overlay.valueWidgets.add(widget)
                    }
                    overlay.layoutOverlay()
                    renderOverlay = true
                } else {
                    overlay.valueWidgets.clear()
                    renderOverlay = false
                }
                onLeftClickValue?.invoke(currentValue!!)
                return true
            } else if (mouseButtonEvent.button() == 1) {
                onRightClickValue?.invoke(currentValue!!)
            }
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x..x + width &&
                mouseY.toInt() in y..y + height)
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

    inner class EnumOverlay(override val renderPriority: Int) : OverlayRenderable, GuiEventListener {
        val overlayX get() = this@SelectorWidget.x
        val overlayY get() = this@SelectorWidget.y + this@SelectorWidget.height
        val overlayWidth get() = this@SelectorWidget.width
        val overlayRowHeight get() = (this@SelectorWidget.height * 0.8f).toInt()


        val valueWidgets: MutableList<ClickableRowWidget<T>> = mutableListOf()

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
            guiGraphics: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ) {
            if (!renderOverlay) return
            valueWidgets.forEach { it.render(guiGraphics, mouseX, mouseY) }
        }

        override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
            valueWidgets.forEach {
                if (it.mouseClicked(mouseButtonEvent, doubled)) {
                    if (mouseButtonEvent.button() == 0) {
                        this@SelectorWidget.onLeftClickValue?.invoke(it.value)
                        this@SelectorWidget.valueChanged(it.value)
                        ChatUtils.sendWithPrefix("Left clicked on: ${it.value?.javaClass} or ${it.value}")
                        return true
                    } else if (mouseButtonEvent.button() == 1) {
                        this@SelectorWidget.onRightClickValue?.invoke(it.value)
                        ChatUtils.sendWithPrefix("Right clicked on: ${it.value?.javaClass} or ${it.value}")
                        //todo open context menu
                    }


                }
            }
            return false
        }

        override fun mouseMoved(d: Double, e: Double) {
            valueWidgets.forEach {
                it.mouseMoved(d, e)
            }
        }

        override fun setFocused(focused: Boolean) {
            isFocused = focused
        }

        override fun isFocused(): Boolean = isFocused


    }
}
