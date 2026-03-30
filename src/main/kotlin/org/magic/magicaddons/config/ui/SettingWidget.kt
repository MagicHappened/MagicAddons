package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.config.data.SettingNode

abstract class SettingWidget<T>(
    protected val node: SettingNode
) : Drawable, Element {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 200
    var height: Int = 20


    fun setPosition(newX: Int, newY: Int) {
        x = newX
        y = newY
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderSelf(ctx, mouseX, mouseY, delta)

        var childY = y + height

        node.getChildren()?.forEach { childNode ->
            val widget = SettingWidgetFactory.create(childNode)

            widget.setPosition(x + 10, childY)
            widget.render(ctx, mouseX, mouseY, delta)

            childY += widget.height

        }
    }

    protected abstract fun renderSelf(
        ctx: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    )

    open fun getTotalHeight(): Int {
        var totalHeight = height

        return totalHeight
    }
    override fun mouseClicked(click: Click, doubled: Boolean): Boolean = false


}
