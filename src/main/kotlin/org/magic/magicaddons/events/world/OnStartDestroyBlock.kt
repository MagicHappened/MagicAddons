package org.magic.magicaddons.events.world

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.magic.magicaddons.events.Cancellable

class OnStartDestroyBlock @JvmOverloads constructor(
    val pos: BlockPos,
    val direction: Direction,
    override var canceled: Boolean = false
) : Cancellable