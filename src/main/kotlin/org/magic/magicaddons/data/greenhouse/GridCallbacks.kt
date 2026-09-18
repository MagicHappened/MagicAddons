package org.magic.magicaddons.data.greenhouse

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand

data class DyingPlant(
    val plant: String,
    val greenhouse: String,
    val plotId: String
)

interface GridCallbacks {

    fun placedCropAt(soilPos: BlockPos): CropDefinition?

    /** whether to ignore water retain */
    fun assumeFlatWater(): Boolean = false

    /** the game hangs its own water bars over the plants while cans are out */
    fun waterBarsExpected(): Boolean = false

    fun forgetPlayerPlacementAt(soilPos: BlockPos)

    /** after receiving confirmation for a placed crop */
    fun placementConfirmed(crop: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean

    fun markAsPlaced(plant: Plant)

    /** marks [plant] as a mutation that spawned where nothing stood at the last look */
    fun claimSpawnedMutation(plant: Plant, layout: GreenhouseLayout)

    /** a plant predicted dead was found standing */
    fun warnSurvivor(plant: DyingPlant)

    /** a scan no longer matches a plant the records had */
    fun plantLostInScan(previous: Plant, origin: BlockPos, remainingStands: List<ArmorStand>) = Unit

    /** used until a feature installs itself */
    object None : GridCallbacks {
        override fun placedCropAt(soilPos: BlockPos): CropDefinition? = null
        override fun forgetPlayerPlacementAt(soilPos: BlockPos) = Unit
        override fun placementConfirmed(crop: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean = false
        override fun markAsPlaced(plant: Plant) = Unit
        override fun claimSpawnedMutation(plant: Plant, layout: GreenhouseLayout) = Unit
        override fun warnSurvivor(plant: DyingPlant) = Unit
    }
}
