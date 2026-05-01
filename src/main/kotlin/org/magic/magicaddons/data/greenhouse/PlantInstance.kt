package org.magic.magicaddons.data.greenhouse

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand

data class PlantInstance(
    val element: CropDefinitionProvider,
    val origin: BlockPos,
    val stands: MutableList<ArmorStand> = mutableListOf(),
    val occupied: MutableSet<BlockPos> = mutableSetOf()
)