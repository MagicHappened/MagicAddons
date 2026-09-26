package org.magic.magicaddons.features.farming.greenhousePresets.lookups

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhouseWatering
import org.magic.magicaddons.util.getBuildableArea

/** Reads other information about plants that isn't directly tied to stage. */
object PlantBars {

    private const val READ_PLANT_BARS_DELAY: Int = 20

    private var ticksUntilNextRead: Int = 0

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        if (--ticksUntilNextRead > 0) return
        ticksUntilNextRead = READ_PLANT_BARS_DELAY

        readBarsInPlot()
    }

    private fun readBarsInPlot() {
        // avoid reading plant bars when the watering window is open
        if (GreenhouseWatering.wateringWindowOpen()) return
        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val buildableArea = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        level.getEntitiesOfClass(ArmorStand::class.java, buildableArea).forEach { stand ->
            val barName = stand.customName ?: return@forEach

            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val plant = grid.elementCoveringSlot(slot)?.plant ?: return@forEach

            val chargeRule = plant.cropDef.chargeRule
            if (chargeRule != null) {
                val chargePercent = StandReader.chargeBarPercent(barName) ?: return@forEach

                plant.readings[StandReader.CHARGE] = chargePercent
                plant.charge = chargeRule.clampToNearest2k(chargePercent)
                plant.chargeKnown = true
                return@forEach
            }

            if (plant.cropDef.hasHungerBar) {
                plant.readings[StandReader.HUNGER] = StandReader.nonWaterBarPercent(barName) ?: return@forEach
            }
        }
    }
}
