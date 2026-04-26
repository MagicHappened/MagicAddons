package org.magic.magicaddons.events.interact

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class OnBlockPlacedEvent(pos: BlockPos, player: LocalPlayer, blockState: BlockState?)