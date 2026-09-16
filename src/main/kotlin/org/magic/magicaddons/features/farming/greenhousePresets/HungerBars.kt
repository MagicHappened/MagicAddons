package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.util.getBuildableArea

/** Keeps the hunger of every plant that hangs a bar over itself current while the player stands in the greenhouse. */
object HungerBars {

    private const val LOOK_EVERY_TICKS: Int = 20

    private var ticksUntilNextLook: Int = 0

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        if (--ticksUntilNextLook > 0) return
        ticksUntilNextLook = LOOK_EVERY_TICKS

        readHungerBars()
    }

    private fun readHungerBars() {
        if (!GreenhouseData.inOwnGarden()) return
        // the game hangs water bars in place of the plants' own while a can is out, and a water bar
        // in debt is red the way a low hunger bar is
        if (GreenhouseWatering.wateringWindowOpen()) return
        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.isScannedThisVisit()) return

        val area = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        level.getEntitiesOfClass(ArmorStand::class.java, area).forEach { stand ->
            val percent = stand.customName?.let { CropStandReader.nonWaterBarPercent(it) } ?: return@forEach

            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val instance = grid.elementCoveringSlot(slot)?.plant ?: return@forEach
            if (!instance.cropDef.hasHungerBar) return@forEach

            instance.readings[CropStandReader.HUNGER] = percent
        }
    }
}
