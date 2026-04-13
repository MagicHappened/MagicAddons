package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.apache.commons.io.filefilter.FalseFileFilter
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.config.ui.DropDownBoxWidget
import org.magic.magicaddons.util.ScreenUtil

class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {

    override var childrenExpanded: Boolean = false
    override var hovered: Boolean = false
    var shouldRenderHistory = false

    var lastFocusedValue: String = setting.value

    override val childrenWidgets: List<SettingWidget<*>>? = null
    val textFieldPadding: Int = 1
    val textPadding: Int = 2

    val historyBlacklist = mutableListOf(
        ""
    )


    val textWidget: TextFieldWidget by lazy {
        TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            width - (borderSize + textFieldPadding) * 2,
            20, // placeholder, changed in init
            Text.literal("")
        )
    }
    val historyWidgets: MutableList<DropDownBoxWidget<String>> = mutableListOf()


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

    fun initHistoryWidgets() {
        historyWidgets.clear()

        var currentY = textWidget.y + textWidget.height
        setting.history.forEach { historyValue ->
            val historyWidget = DropDownBoxWidget(
                value = historyValue,
                displayText = { historyValue },
                onClick = { historyTextWidgetClicked(it) },
                removable = true,
                onRemove = { onHistoryWidgetXClicked(it) }
            )

            historyWidget.x = textWidget.x
            historyWidget.y = currentY
            historyWidget.width = textWidget.width
            historyWidget.height = textWidget.height
            currentY += historyWidget.height

            historyWidgets.add(historyWidget)
        }
    }

    override fun getTotalHeight(): Int = height

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
        if (shouldRenderHistory){
            historyWidgets.forEach {
                it.render(ctx)
            }
        }
    }


    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {

        val clickedTextWidget = textWidget.mouseClicked(click, doubled)
        if (clickedTextWidget){
            textWidget.isFocused = true
            initHistoryWidgets()
            shouldRenderHistory = true
            return true
        }

        if (shouldRenderHistory){
            historyWidgets.forEach {
                if (it.mouseClicked(click, doubled)){
                    shouldRenderHistory = false
                    textWidget.isFocused = false
                    return true
                }
            }
        }

        if (textWidget.isFocused){
            if (lastFocusedValue !in historyBlacklist && lastFocusedValue != setting.value){
                setting.history.add(lastFocusedValue)
            }
        }
        lastFocusedValue = setting.value
        textWidget.isFocused = false
        shouldRenderHistory = false


        return false
    }

    fun onHistoryWidgetXClicked(widget: DropDownBoxWidget<String>) {
        historyWidgets.remove(widget)
        setting.history.remove(widget.value)
    }



    fun historyTextWidgetClicked(widget: DropDownBoxWidget<String>){
        if (setting.value !in historyBlacklist){
            setting.history.add(setting.value)
        }
        setting.value = widget.value
        setting.history.remove(widget.value)
        textWidget.text = widget.value
        shouldRenderHistory = false
    }
}