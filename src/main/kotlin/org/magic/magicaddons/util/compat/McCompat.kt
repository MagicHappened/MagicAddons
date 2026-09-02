package org.magic.magicaddons.util.compat

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/**
 * Everything the game renamed between 26.1.2 and 26.2, behind one door.
 *
 * The two versions differ in a handful of places that have nothing to do with each other: where the
 * current screen lives, where the subtitle extraction hangs, whether a colour is asked of
 * [ChatFormatting] or of a text colour constant, and whether the stained glass blocks are separate
 * fields or one coloured collection. None of that is interesting to the code that calls it, so it
 * is answered once here and the callers ask a question that means the same thing on both.
 *
 * This is also the seam the version directives will live on once both versions build from one tree:
 * keeping them in a file nothing else reads means the features themselves never carry a comment
 * about which Minecraft they are being compiled for.
 */
object McCompat {

    /** The screen the player is looking at, or null while they are looking at the world. */
    fun currentScreen(): Screen? = Minecraft.getInstance().screen

    /** Puts [screen] up, or takes whatever is up down when it is null. */
    fun setScreen(screen: Screen?) {
        Minecraft.getInstance().setScreen(screen)
    }

    /** The subtitle pass a screen has to run itself when it draws its own background. */
    fun extractDeferredSubtitles(minecraft: Minecraft) {
        minecraft.gui.extractDeferredSubtitles()
    }

    /** Whether the player has hidden the hud, which anything drawing over it should respect. */
    fun hudHidden(): Boolean = Minecraft.getInstance().options.hideGui

    /** [formatting]'s colour as a packed rgb int, for comparing against what a component wears. */
    fun chatColor(formatting: ChatFormatting): Int = formatting.color ?: 0xFFFFFF

    /** The green stained glass block, which the chloronite wears when it is finished. */
    fun greenStainedGlass(): Block = Blocks.GREEN_STAINED_GLASS
}
