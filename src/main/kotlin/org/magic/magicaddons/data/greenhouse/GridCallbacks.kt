package org.magic.magicaddons.data.greenhouse

import net.minecraft.core.BlockPos

data class DyingPlant(
    val plant: String,
    val greenhouse: String,
    val plotId: String
)

interface GridCallbacks {

    fun placedDefinitionAt(soil: BlockPos): CropDefinition?

    /** whether to ignore water retain */
    fun assumeFlatWater(): Boolean = false

    /** Whether the game is hanging water bars over the plants right now, in place of their own bars. */
    fun waterBarsExpected(): Boolean = false

    fun forgetPlayerPlacementAt(soil: BlockPos)

    /** after receiving confirmation for a placed crop */
    fun placementConfirmed(def: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean

    /** marks the plant instance as player placed. */
    fun markAsPlacedPlant(instance: GreenhouseElementInstance)

    /** Marks [instance] as a mutation that spawned where nothing stood at the last look. */
    fun claimSpawnedMutation(instance: GreenhouseElementInstance, layout: GreenhouseLayout)

    /** A plant predicted dead was found standing. */
    fun warnSurvivor(plant: DyingPlant)

    /** Used until a feature installs itself: nothing was placed, nothing is claimed, nothing is said. */
    object None : GridCallbacks {
        override fun placedDefinitionAt(soil: BlockPos): CropDefinition? = null
        override fun forgetPlayerPlacementAt(soil: BlockPos) = Unit
        override fun placementConfirmed(def: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean = false
        override fun markAsPlacedPlant(instance: GreenhouseElementInstance) = Unit
        override fun claimSpawnedMutation(instance: GreenhouseElementInstance, layout: GreenhouseLayout) = Unit
        override fun warnSurvivor(plant: DyingPlant) = Unit
    }
}
