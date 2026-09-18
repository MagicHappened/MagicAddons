package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.ScannedPlant
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.AttackEntityEvent
import org.magic.magicaddons.events.interact.BlockBreakEvent
import org.magic.magicaddons.util.ChatUtils

/** Refuses a swing that would take out a plant the settings say to keep, and says so once. */
object BreakProtection {

    /** holding the button fires the break every tick, so a plant is named once a second at most */
    private const val MESSAGE_COOLDOWN_MS: Long = 1_000

    private val lastMessageAt = mutableMapOf<Triple<String, Int, Int>, Long>()

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val plant = GreenhouseData.scannedPlantAtBlock(event.pos) ?: return
        if (protects(plant)) event.canceled = true
    }

    @EventHandler
    fun onAttackEntity(event: AttackEntityEvent) {
        val stand = event.target as? ArmorStand ?: return
        val plant = GreenhouseData.scannedPlantAtStand(stand) ?: return
        if (protects(plant)) event.canceled = true
    }

    private fun protects(scanned: ScannedPlant): Boolean {
        val plant = scanned.plant
        val keptAsIngredient = GreenhousePresets.preventBreakingIngredients() && GreenhouseData.isPlannedIngredient(scanned)
        val keptWhileGrowing = GreenhousePresets.preventBreakingGrowingMutations() &&
                plant.cropDef.isMutation && !plant.isFullyGrown
        if (!keptAsIngredient && !keptWhileGrowing) return false

        tell(scanned)
        return true
    }

    private fun tell(scanned: ScannedPlant) {
        val plant = scanned.plant
        val key = Triple(GreenhouseData.getCurrentGrid()?.layout?.id.orEmpty(), plant.slot.x, plant.slot.y)
        val now = System.currentTimeMillis()
        if (now - (lastMessageAt[key] ?: 0L) < MESSAGE_COOLDOWN_MS) return

        lastMessageAt[key] = now
        ChatUtils.sendWithPrefix("Prevented breaking ${plant.cropDef.name}")
    }
}
