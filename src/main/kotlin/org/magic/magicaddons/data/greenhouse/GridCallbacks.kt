package org.magic.magicaddons.data.greenhouse

import net.minecraft.core.BlockPos

/** One plant in trouble: what it is, which greenhouse holds it, and that greenhouse's layout id. */
data class DyingPlant(
    val plant: String,
    val greenhouse: String,
    val plotId: String
)

/** What a grid scan asks of the feature running it: the crops the player put down, and a plant found alive past its predicted death. */
interface GridCallbacks {

    /** The crop the player put down on the soil at [soil] this session, if any. */
    fun placedHereAt(soil: BlockPos): CropDefinition?

    /** The soil at [soil] no longer holds what was put down there. */
    fun forgetPlacementAt(soil: BlockPos)

    /** Whether a plant of [def] was just put down on [slot], taking the placement off the list. */
    fun takePlacement(def: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean

    /** Marks [instance] as a plant the player put down. */
    fun claimPlacedPlant(instance: GreenhouseElementInstance)

    /** Marks [instance] as a mutation that spawned where nothing stood at the last look. */
    fun claimSpawnedMutation(instance: GreenhouseElementInstance, layout: GreenhouseLayout)

    /** A plant predicted dead was found standing. */
    fun warnSurvivor(plant: DyingPlant)

    /** Used until a feature installs itself: nothing was placed, nothing is claimed, nothing is said. */
    object None : GridCallbacks {
        override fun placedHereAt(soil: BlockPos): CropDefinition? = null
        override fun forgetPlacementAt(soil: BlockPos) = Unit
        override fun takePlacement(def: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean = false
        override fun claimPlacedPlant(instance: GreenhouseElementInstance) = Unit
        override fun claimSpawnedMutation(instance: GreenhouseElementInstance, layout: GreenhouseLayout) = Unit
        override fun warnSurvivor(plant: DyingPlant) = Unit
    }
}
