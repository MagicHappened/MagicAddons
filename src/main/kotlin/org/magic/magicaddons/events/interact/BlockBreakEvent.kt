package org.magic.magicaddons.events.interact

import net.minecraft.core.BlockPos
import org.magic.magicaddons.events.Cancellable

/** A block the player is about to break. Cancelled, no packet is sent and the block stays. */
class BlockBreakEvent(
    val pos: BlockPos,
    override var canceled: Boolean = false
) : Cancellable
