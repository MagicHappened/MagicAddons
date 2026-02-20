package org.magic.magicaddons.features.api

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.screen.slot.Slot

interface SlotRenderable {
    fun onSlotRender(context: DrawContext, slot: Slot, screen: HandledScreen<*>)
}