package org.magic.magicaddons.features.farming.greenhousePresets.playerActions

import java.time.Duration
import java.time.Instant
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.data.server.CoopSync
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.greenhouse.GreenhouseChangedEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId

object GreenhouseWatering {

    private val wateringCanIds: Set<String> = setOf(
        "HYDRO_CAN_1000",
        "HYDRO_CAN_TURBO_2000",
        "HYDRO_CAN_ULTRA_3000",
        "AQUAMASTER_X",
        "AQUAMASTER_HYDROMAX"
    )
    private val WATERING_WINDOW: Duration = Duration.ofSeconds(5)

    private var wateringUntil: Instant? = null

    private val wateredThisWindow: MutableSet<Plant> = mutableSetOf()

    private const val HUNGER_LABEL: String = "Hunger"
    private val HUNGER_BAR_SETTLE: Duration = Duration.ofMillis(1500)

    private class PendingWater(val percent: Int, val since: Instant)

    private val pendingHungryPlantWater: MutableMap<Plant, PendingWater> = mutableMapOf()

    private fun isWateringCan(id: SkyBlockId): Boolean =
        id.id.substringAfter("item:").uppercase() in wateringCanIds

    fun wateringWindowOpen(): Boolean = wateringUntil?.isAfter(Instant.now()) == true

    fun startWateringWindow(heldId: SkyBlockId): Boolean {
        if (!isWateringCan(heldId)) return false

        wateringUntil = Instant.now().plus(WATERING_WINDOW)
        return true
    }

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        readWaterStands()
    }

    private fun readWaterStands() {
        val until = wateringUntil ?: return

        if (Instant.now().isAfter(until)) {
            wateringUntil = null
            wateredThisWindow.clear()
            pendingHungryPlantWater.clear()
            return
        }

        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val buildableArea = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        val standNamesByPlant = level.getEntitiesOfClass(ArmorStand::class.java, buildableArea)
            .mapNotNull { stand ->
                val name = stand.customName ?: return@mapNotNull null
                val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@mapNotNull null
                val plant = grid.elementCoveringSlot(slot)?.plant ?: return@mapNotNull null
                plant to name
            }
            .groupBy({ it.first }, { it.second })

        val now = Instant.now()
        standNamesByPlant.forEach { (plant, names) ->
            if (!plant.cropDef.needsWater || plant.isPlacedMutation) return@forEach
            val barPercent = names.firstNotNullOfOrNull { waterBarPercent(it) } ?: return@forEach
            if (plant.cropDef.hasHungerBar && !hungryPlantWaterSettled(plant, names, barPercent, now)) return@forEach

            val before = plant.waterLevel
            plant.waterLevel = barPercent.toDouble()
            if (before == null || barPercent > before) {
                if (wateredThisWindow.add(plant)) CoopSync.noteWatered(grid)
                GreenhouseData.markContentChanged(grid)
            }

            plant.waterExact = barPercent >= PlotPrediction.WATER_FULL_LEVEL
            plant.waterBestCase = null
            plant.waterPredictedNegative = false
        }
        GreenhouseData.noteGridUpdated(grid, GreenhouseChangedEvent.Source.Watering)
    }

    private fun hungryPlantWaterSettled(plant: Plant, names: List<Component>, barPercent: Int, now: Instant): Boolean {
        if (names.any { it.string.contains(HUNGER_LABEL, ignoreCase = true) }) {
            pendingHungryPlantWater.remove(plant)
            return false
        }

        val pending = pendingHungryPlantWater[plant]
        if (pending == null || pending.percent != barPercent) {
            pendingHungryPlantWater[plant] = PendingWater(barPercent, now)
            return false
        }
        return Duration.between(pending.since, now) >= HUNGER_BAR_SETTLE
    }

    private fun waterBarPercent(name: Component): Int? {
        val counted = StandReader.barNotches(name) ?: return null
        if (counted.otherColoured > 0) return null

        if (counted.negative > 0) return -(counted.negative * 100 / counted.total)

        return counted.filled * 100 / counted.total
    }
}
