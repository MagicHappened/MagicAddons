package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.util.ScreenUtil

abstract class SettingWidget<T>(
    protected val node: SettingNode<T>
) : Drawable, Element {

    abstract val childrenWidgets: List<SettingWidget<*>>?

    var x: Int = 0
    var y: Int = 0
    open var width: Int = 20
    open var height: Int = 40

    val childPadding: Int = 4
    var childrenExpanded = false

    val borderColor: Int = 0xFF000000.toInt()
    val borderSize: Int = 2
    val backgroundColor: Int = 0xFF555555.toInt()

    val textXPad: Int = 10
    val textYPad: Int = 10

    abstract fun init()

    abstract fun getActualHeight(): Int

    abstract override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

    fun renderChildrenIfExpanded(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float){
        if (childrenExpanded) {

            childrenWidgets?.forEach {
                it.x = x + 10
                it.y = y + height + childPadding
                it.width = width - 10
                it.height = height
                it.init()
                it.render(ctx, mouseX, mouseY, delta)
            }
        }
    }


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        return click.x.toInt() in x..x+width &&
                click.y.toInt() in y..y+height
    }

    override fun isFocused(): Boolean = isFocused

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

}