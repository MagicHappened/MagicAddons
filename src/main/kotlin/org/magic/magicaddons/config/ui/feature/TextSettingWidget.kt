package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.util.ScreenUtil

class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {

    override var childrenExpanded: Boolean = false
    override var hovered: Boolean = false
    override val childrenWidgets: List<SettingWidget<*>>? = null
    val textFieldPadding: Int = 1
    val textPadding: Int = 2

    val textWidget: TextFieldWidget by lazy {
        TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            width - (borderSize + textFieldPadding) * 2,
            20, // placeholder, changed in init
            Text.literal("")
        )
    }


    override fun init() {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        textWidget.x = x + borderSize + textFieldPadding
        textWidget.y = y + borderSize + textFieldPadding + textRenderer.fontHeight + textPadding * 2
        textWidget.height = height - (borderSize + textFieldPadding + textPadding) * 2 - textRenderer.fontHeight
        textWidget.setMaxLength(256)
        textWidget.text = setting.value
        textWidget.setChangedListener {
            setting.value = it
        }

        super.init() // kinda redundant
    }

    override fun getActualHeight(): Int = height

    override fun charTyped(input: CharInput): Boolean {
        if (super.charTyped(input))
            return true
        if (textWidget.isFocused){
            if (textWidget.charTyped(input))
                return true
        }
        return false
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (super.keyPressed(input))
            return true
        if (textWidget.isFocused){
            if (textWidget.keyPressed(input))
                return true
        }
        return false
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ctx.fill(x, y, x + width, y + height, backgroundColor)
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)



        textWidget.render(ctx, mouseX, mouseY, delta)
        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("${setting.displayName}: "),
            x + 10,
            y + textPadding + borderSize,
            0xFFCCCCCC.toInt(),
            false
        )
        if (hovered){
            renderHovered(ctx, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val clickedTextWidget = textWidget.mouseClicked(click, doubled)
        textWidget.isFocused = clickedTextWidget

        return clickedTextWidget
    }

}