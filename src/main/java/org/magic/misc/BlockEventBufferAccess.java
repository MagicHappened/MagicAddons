package org.magic.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public interface BlockEventBufferAccess {
    Map<BlockPos, BlockState> magicaddons$getPendingPlaces();
    Map<BlockPos, BlockState> magicaddons$getPendingBreaks();
}
