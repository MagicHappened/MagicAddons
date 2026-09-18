package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.CROP_HEIGHT
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.Plant
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.EntityAddedEvent
import org.magic.magicaddons.events.world.EntityUpdatedEvent
import org.magic.magicaddons.util.getBuildableArea

/**
 * The charge bar of a plant only hangs in the world while the player stands near it, long after the
 * plot was read, so it is taken as the stands arrive and change rather than at a scan.
 */
object GreenhouseChargeBars {

    @EventHandler
    fun onEntityAdded(event: EntityAddedEvent) = readBarsNear(event.addedEntityList.map { it.entity })

    @EventHandler
    fun onEntityUpdated(event: EntityUpdatedEvent) = readBarsNear(event.updatedEntityList.map { it.entity })

    private fun readBarsNear(entities: List<net.minecraft.world.entity.Entity>) {
        val bars = entities.filterIsInstance<ArmorStand>().filter { stand ->
            stand.customName?.let { CropStandReader.nonWaterBarPercent(it) } != null
        }
        if (bars.isEmpty()) return

        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.isScannedThisVisit()) return
        val area = grid.plot?.getBuildableArea() ?: return
        if (bars.none { area.contains(it.position()) }) return

        grid.layout.plants.filter { it.cropDef.chargeRule != null }.forEach { readBarOf(grid, it) }
    }

    private fun readBarOf(grid: GreenhouseGrid, plant: Plant) {
        val rule = plant.cropDef.chargeRule ?: return
        val level = Minecraft.getInstance().level ?: return
        val origin = grid.getPosForSlot(plant.slot) ?: return

        val percent = level
            .getEntitiesOfClass(ArmorStand::class.java, plant.cropDef.footprint.spaceAbove(origin, CROP_HEIGHT))
            .mapNotNull { stand -> stand.customName?.let { CropStandReader.nonWaterBarPercent(it) } }
            .maxOrNull() ?: return

        plant.charge = rule.chargeShownBy(percent)
        plant.chargeKnown = true
    }
}
