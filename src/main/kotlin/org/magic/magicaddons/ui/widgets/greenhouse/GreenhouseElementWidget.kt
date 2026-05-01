package org.magic.magicaddons.ui.widgets.greenhouse

import com.mojang.authlib.minecraft.client.MinecraftClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem
import tech.thatgravyboat.skyblockapi.utils.extentions.getItemModel
import kotlin.math.min

class GreenhouseElementWidget(val definition: CropDefinition) : Renderable, GuiEventListener {
    var widgetX: Int = 0
    var widgetY: Int = 0
    var padding: Int = 0
    var width = 50
    var height = 50


    var renderedStack: ItemStack = ItemStack.EMPTY

    fun init(){
        Minecraft.getInstance().itemModelResolver
    }


    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        guiGraphics.renderFakeItem(
            renderedStack,
            widgetX + padding,
            widgetY + padding,
            width,
            height,
        )
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}