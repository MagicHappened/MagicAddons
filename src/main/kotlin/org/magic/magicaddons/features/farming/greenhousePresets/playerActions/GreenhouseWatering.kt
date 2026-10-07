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
            return
        }

        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val buildableArea = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        level.getEntitiesOfClass(ArmorStand::class.java, buildableArea).forEach { stand ->
            val barPercent = stand.customName?.let { waterBarPercent(it) } ?: return@forEach
            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val plant = grid.elementCoveringSlot(slot)?.plant ?: return@forEach

            if (!plant.cropDef.needsWater || plant.isPlacedMutation) return@forEach

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
    }

    private fun waterBarPercent(name: Component): Int? {
        val counted = StandReader.barNotches(name) ?: return null
        if (counted.otherColoured > 0) return null

        if (counted.negative > 0) return -(counted.negative * 100 / counted.total)

        return counted.filled * 100 / counted.total
    }
}
