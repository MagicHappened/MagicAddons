package org.magic.magicaddons.events.interact

import net.minecraft.core.BlockPos
import org.magic.magicaddons.events.Cancellable

class OnStartDestroyBlockEvent @JvmOverloads constructor(val blockPos: BlockPos, override var canceled: Boolean = false) : Cancellable{
}