package org.magic.magicaddons.events.interact

import net.minecraft.core.BlockPos
import org.magic.magicaddons.events.Cancellable

class BlockBreakEvent(
    val pos: BlockPos,
    override var canceled: Boolean = false
) : Cancellable
