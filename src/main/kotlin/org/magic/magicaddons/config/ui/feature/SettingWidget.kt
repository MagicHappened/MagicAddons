package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.config.data.SettingNode

abstract class SettingWidget<T>(
    protected val node: SettingNode<T>
) : Drawable, Element {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 300
    var height: Int = 50


    fun setPosition(newX: Int, newY: Int) {
        x = newX
        y = newY
    }

    abstract override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        return click.x.toInt() in x..x+width &&
                click.y.toInt() in y..y+height
    }


}