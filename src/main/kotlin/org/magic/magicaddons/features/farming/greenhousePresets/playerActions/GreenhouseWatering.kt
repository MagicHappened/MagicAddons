package org.magic.magicaddons.features.farming.greenhousePresets.playerActions

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.time.Duration
import java.time.Instant
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData

object GreenhouseWatering {

    private val wateringCanIds: Set<String> = setOf(
        "HYDRO_CAN_1000",
        "HYDRO_CAN_TURBO_2000",
        "HYDRO_CAN_ULTRA_3000",
        "AQUAMASTER_X",
        "AQUAMASTER_HYDROMAX"
    )

    /** How long after a watering the bars are worth looking for before they take themselves away. */
    private val WATERING_WINDOW: Duration = Duration.ofSeconds(10)

    /** When the stands spawned by the last watering stop being expected. */
    private var wateringUntil: Instant? = null

    private fun isWateringCan(id: SkyBlockId): Boolean =
        id.id.substringAfter("item:").uppercase() in wateringCanIds

    fun wateringWindowOpen(): Boolean = wateringUntil?.isAfter(Instant.now()) == true

    /** Opens the window if the held item is a watering can, and says whether it did. */
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
            return
        }

        if (!GreenhouseData.inOwnGarden()) return
        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.isScannedThisVisit()) return

        val buildableArea = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        level.getEntitiesOfClass(ArmorStand::class.java, buildableArea).forEach { stand ->
            val barPercent = stand.customName?.let { waterBarPercent(it) } ?: return@forEach
            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val plant = grid.elementCoveringSlot(slot)?.plant ?: return@forEach

            if (!plant.cropDef.needsWater || plant.isPlacedMutation) return@forEach

            plant.waterLevel = barPercent.toDouble()
            // a bar is only good to a notch, so the level it gives is exact only when it reads full
            plant.waterExact = barPercent >= WaterModel.FULL_LEVEL
            plant.waterBestCase = null
            plant.waterPredictedInDebt = false
        }
    }

    /**
     * A water bar as a level between -100 and 100: blue notches are water held, red notches debt.
     * Any other colour is somebody else's bar, refused.
     */
    private fun waterBarPercent(name: Component): Int? {
        val counted = CropStandReader.barNotches(name) ?: return null
        if (counted.otherColoured > 0) return null

        // a bar cannot show both at once, and a negative level is the one worth reporting
        if (counted.debt > 0) return -(counted.debt * 100 / counted.total)

        return counted.filled * 100 / counted.total
    }
}
