package org.magic.magicaddons.util.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Everything the game renamed between 26.1.2 and 26.2, behind one door, so callers ask one question
 * that means the same on both. The version directives live here rather than in the features.
 */
object McCompat {

    /** The screen the player is looking at, or null while they are looking at the world. */
    fun currentScreen(): Screen? {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen()
        *///?} else {
        return Minecraft.getInstance().screen
        //?}
    }

    /** Puts a screen up, or takes whatever is up down when it is null. */
    fun setScreen(screen: Screen?) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.setScreen(screen)
        *///?} else {
        Minecraft.getInstance().setScreen(screen)
        //?}
    }

    /** The subtitle pass a screen has to run itself when it draws its own background. */
    fun extractDeferredSubtitles(minecraft: Minecraft) {
        //? if >=26.2 {
        /*minecraft.gui.hud.extractDeferredSubtitles()
        *///?} else {
        minecraft.gui.extractDeferredSubtitles()
        //?}
    }

    /** Whether the player has hidden the hud, which anything drawing over it should respect. */
    fun hudHidden(): Boolean {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.isHidden
        *///?} else {
        return Minecraft.getInstance().options.hideGui
        //?}
    }
}
