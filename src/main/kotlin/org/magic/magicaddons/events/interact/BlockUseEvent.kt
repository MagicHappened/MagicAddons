package org.magic.magicaddons.events.interact

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult

/** A right click on a block with [item], the stack in the hand used at the moment of the click. */
class BlockUseEvent(val player: LocalPlayer, val hit: BlockHitResult, val item: ItemStack)
