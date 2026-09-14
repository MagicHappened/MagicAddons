package org.magic.magicaddons.events.interact

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** A right click with [item], the stack in the hand used at the moment of the click. */
class UseEvent(val player: Player, val item: ItemStack)
