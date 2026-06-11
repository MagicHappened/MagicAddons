package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance

object LayoutRenderState {
    const val RED_TINT: Int = 0x70FFA6A6
    const val NO_TINT: Int = 0x70FFFFFF

    val slotRenders = mutableListOf<SlotRenderGroup>()
    val cropRenders = mutableListOf<CropRenderGroup>()

    data class SlotRenderGroup(
        val blockPos: BlockPos,
        val blockState: BlockState,
        val tint: Int = NO_TINT
    )

    data class CropRenderGroup(
        val instance: GreenhouseElementInstance,
        val basePos: BlockPos,
        val blockMap: Map<BlockPos, BlockState>,
        val stands: List<ArmorStand>,
        var tint: Int = NO_TINT
    )
}