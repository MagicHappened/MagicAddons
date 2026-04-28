package org.magic.magicaddons.events.interact

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class OnBlockDestroyedEvent(val pos: BlockPos,val player: LocalPlayer,val blockState: BlockState?)