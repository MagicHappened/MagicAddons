package org.magic.magicaddons.util.compat

import net.minecraft.ChatFormatting
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
//? if >=26.2 {
/*import net.minecraft.network.chat.TextColor
*///?}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object McCompat {

    fun currentScreen(): Screen? {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen()
        *///?} else {
        return Minecraft.getInstance().screen
        //?}
    }

    fun camera(): Camera {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gameRenderer.mainCamera()
        *///?} else {
        return Minecraft.getInstance().gameRenderer.mainCamera
        //?}
    }

    fun chat(): ChatComponent {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.chat
        *///?} else {
        return Minecraft.getInstance().gui.chat
        //?}
    }

    fun setScreen(screen: Screen?) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.setScreen(screen)
        *///?} else {
        Minecraft.getInstance().setScreen(screen)
        //?}
    }

    fun extractDeferredSubtitles(minecraft: Minecraft) {
        //? if >=26.2 {
        /*minecraft.gui.hud.extractDeferredSubtitles()
        *///?} else {
        minecraft.gui.extractDeferredSubtitles()
        //?}
    }

    fun showTitle(title: Component, fadeIn: Int, stay: Int, fadeOut: Int) {
        //? if >=26.2 {
        /*val hud = Minecraft.getInstance().gui.hud
        hud.setTimes(fadeIn, stay, fadeOut)
        hud.setTitle(title)
        *///?} else {
        val gui = Minecraft.getInstance().gui
        gui.setTimes(fadeIn, stay, fadeOut)
        gui.setTitle(title)
        //?}
    }

    fun hudHidden(): Boolean {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.isHidden
        *///?} else {
        return Minecraft.getInstance().options.hideGui
        //?}
    }

    fun chatColor(formatting: ChatFormatting): Int {
        //? if >=26.2 {
        /*return TextColor.fromLegacyFormat(formatting)?.value ?: 0xFFFFFF
        *///?} else {
        return formatting.color ?: 0xFFFFFF
        //?}
    }

    fun greenStainedGlass(): Block {
        //? if >=26.2 {
        /*return Blocks.STAINED_GLASS.green()
        *///?} else {
        return Blocks.GREEN_STAINED_GLASS
        //?}
    }
}
