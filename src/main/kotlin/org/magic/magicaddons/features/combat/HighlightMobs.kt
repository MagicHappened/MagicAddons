package org.magic.magicaddons.features.combat

import net.minecraft.client.render.DrawStyle
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.world.debug.gizmo.GizmoDrawing
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.WorldEntities


object HighlightMobs : Feature() {
    init {
        EventBus.register(this)
    }

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        if (!enabled) return


        WorldEntities.entityList.forEach { info ->
            val player = info.entity
            if (player !is PlayerEntity) return@forEach

            val tags = info.armorStandTags ?: emptyList()

            val shouldHighlight = tags.any { it == "Littlefoot" }

            if (shouldHighlight) {
                val minX = player.x - 0.5
                val minY = player.y
                val minZ = player.z - 0.5
                val maxX = player.x + 0.5
                val maxY = player.y + 2.0
                val maxZ = player.z + 0.5

                val box = Box(minX, minY, minZ, maxX, maxY, maxZ)

                GizmoDrawing.box(
                    box,
                    DrawStyle.stroked(0xFFFF0000.toInt(), 2f) // ARGB red
                ).ignoreOcclusion()
            }


        }
    }


    override val id: String = "HighlightMobs"
    override val displayName: String = "Mob Highlight"
    override val  tooltipMessage: String = "Highlights specific mobs of your choosing"
    override val category: String = "Combat"

}