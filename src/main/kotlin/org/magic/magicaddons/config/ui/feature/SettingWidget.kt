package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.util.ChatUtils
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
    abstract var childrenExpanded: Boolean

    val borderColor: Int = 0xFF000000.toInt()
    val borderSize: Int = 2
    val backgroundColor: Int = 0xFF555555.toInt()

    val textXPad: Int = 10
    val textYPad: Int = 10

    open fun init(){
        childrenWidgets?.forEach {
            it.x = x + 10
            it.y = y + height + childPadding
            it.width = width - 10
            it.height = height
            it.init()
        }
    }

    abstract fun getActualHeight(): Int

    abstract override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float)


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        return click.x.toInt() in x..x+width &&
                click.y.toInt() in y..y+height
    }

    override fun isFocused(): Boolean = isFocused

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!childrenExpanded) {
            return false
        }
        childrenWidgets?.forEach {
            if (it.charTyped(input)) return true
        }
        return false
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!childrenExpanded) {
            return false
        }
        childrenWidgets?.forEach {
            if (it.keyPressed(input)) return true
        }
        return super.keyPressed(input)
    }

    override fun toString(): String {
        return "Display Name: ${node.displayName} Value: ${node.value}"
    }
}