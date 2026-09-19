package org.magic.magicaddons.features.farming.greenhousePresets.playerActions

import java.time.Duration
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.BlockHitResult
import java.time.Instant
import org.magic.magicaddons.data.greenhouse.Plant
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData

object GreenhousePlantDischarge {

    private var clickedPlant: Plant? = null
    private var clickedAt: Instant = Instant.EPOCH

    private val CLICK_TO_MESSAGE_WINDOW: Duration = Duration.ofSeconds(5)

    private val EMPTIED_REGEX: Regex = Regex("discharged all his energy", RegexOption.IGNORE_CASE)

    private val ABSORBED_REGEX: Regex = Regex("absorbed ([\\d,.]+)(k?) charge", RegexOption.IGNORE_CASE)

    fun setPlantClicked(plant: Plant?) {
        if (plant?.cropDef?.chargeRule == null) return

        clickedPlant = plant
        clickedAt = Instant.now()
    }

    @EventHandler
    fun onSystemChat(event: SystemChatEvent) {
        if (!EMPTIED_REGEX.containsMatchIn(event.text) && !ABSORBED_REGEX.containsMatchIn(event.text)) return

        val plant = clickedPlant?.takeIf { Duration.between(clickedAt, Instant.now()) <= CLICK_TO_MESSAGE_WINDOW }
            ?: plantUnderCrosshair()
            ?: return
        if (!event.text.contains(plant.cropDef.name, ignoreCase = true)) return

        if (EMPTIED_REGEX.containsMatchIn(event.text)) {
            plant.charge = 0
            plant.chargeKnown = true
            clickedPlant = null
            return
        }

        val absorbedCharge = ABSORBED_REGEX.find(event.text) ?: return
        plant.charge = (plant.charge - chargeSubtracted(absorbedCharge)).coerceAtLeast(0)
        plant.chargeKnown = true
        clickedPlant = null
    }

    private fun plantUnderCrosshair(): Plant? {
        val minecraft = Minecraft.getInstance()
        (minecraft.crosshairPickEntity as? ArmorStand)?.let { stand ->
            GreenhouseData.scannedPlantAtStand(stand)?.plant?.let { return it }
        }
        val blockHit = minecraft.hitResult as? BlockHitResult ?: return null
        return GreenhouseData.scannedPlantAtBlock(blockHit.blockPos)?.plant
    }

    private fun chargeSubtracted(match: MatchResult): Int {
        val writtenCharge = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return 0
        return (if (match.groupValues[2].isEmpty()) writtenCharge else writtenCharge * 1000).toInt()
    }
}
