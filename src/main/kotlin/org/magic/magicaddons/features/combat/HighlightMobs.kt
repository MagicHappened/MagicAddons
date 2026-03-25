package org.magic.magicaddons.features.combat

import com.google.gson.JsonParser
import net.minecraft.client.render.DrawStyle
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.world.debug.gizmo.GizmoDrawing
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.WorldEntities
import java.util.Base64


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

            // matcho skin
            val shouldHighlight = PlayerUtils.getSkinHash(player).equals("ef2daabb78a1f7aa12d145d88c0ca46b9e856f5534e9286e555faf0c291f4fd5")

            if (shouldHighlight) {
                val minX = player.x - 0.2
                val minY = player.y
                val minZ = player.z - 0.2
                val maxX = player.x + 0.2
                val maxY = player.y + 2.0
                val maxZ = player.z + 0.2

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