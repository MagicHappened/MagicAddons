package org.magic.magicaddons.events.interact

import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.ItemStack

class OnAnyPlayerSwingEvent(
    val player: AbstractClientPlayerEntity,
    val mainHandStack: ItemStack ,
    val self: Boolean
)