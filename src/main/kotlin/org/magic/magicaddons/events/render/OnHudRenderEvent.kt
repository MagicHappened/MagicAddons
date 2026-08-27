package org.magic.magicaddons.events.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Fired while the in game hud collects what it wants to draw, after vanilla added its own elements. */
class OnHudRenderEvent(
    val graphics: GuiGraphicsExtractor,
    val deltaTracker: DeltaTracker
)
