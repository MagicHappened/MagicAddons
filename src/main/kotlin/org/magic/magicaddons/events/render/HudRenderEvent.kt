package org.magic.magicaddons.events.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

class HudRenderEvent(
    val graphics: GuiGraphicsExtractor,
    val deltaTracker: DeltaTracker
)
