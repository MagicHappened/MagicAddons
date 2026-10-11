package org.magic.magicaddons.data.greenhouse.plot

import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.crops.*

interface GridCallbacks {

    fun placedCropAt(soilPos: BlockPos): CropDefinition?

    fun assumeFlatWater(): Boolean = false

    fun waterBarsExpected(): Boolean = false

    fun forgetPlayerPlacementAt(soilPos: BlockPos)

    fun placementConfirmed(plant: Plant, grid: GreenhouseGrid): Long?

    fun markAsPlaced(plant: Plant, placedAt: Long?)

    fun claimSpawnedMutation(plant: Plant, layout: PlotLayout)

    object None : GridCallbacks {
        override fun placedCropAt(soilPos: BlockPos): CropDefinition? = null
        override fun forgetPlayerPlacementAt(soilPos: BlockPos) = Unit
        override fun placementConfirmed(plant: Plant, grid: GreenhouseGrid): Long? = null
        override fun markAsPlaced(plant: Plant, placedAt: Long?) = Unit
        override fun claimSpawnedMutation(plant: Plant, layout: PlotLayout) = Unit
    }
}
