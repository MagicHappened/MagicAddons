package org.magic.magicaddons.events.interact

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class OnAnyPlayerSwingEvent(
    val player: Player,
    val mainHandStack: ItemStack,
    val self: Boolean
)