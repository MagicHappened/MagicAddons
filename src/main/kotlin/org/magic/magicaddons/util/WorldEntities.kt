package org.magic.magicaddons.util

import net.minecraft.client.MinecraftClient
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import kotlin.math.sqrt

object WorldEntities {
    init {
        EventBus.register(this)
    }

    // The single list we maintain
    var entityList: List<EntityInfo> = emptyList()
        private set

    var tickCounter = 0

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent){
        tickCounter++
        if (tickCounter == 20){
            tickCounter = 0
            update()
        }
    }

    fun update() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val world = client.world ?: return

        val range = 30.0
        val box = player.boundingBox.expand(range)

        val updatedList = mutableListOf<EntityInfo>()

        // Iterate all entities in range (excluding armor stands)
        world.getOtherEntities(player, box)
            .filter { it !is ArmorStandEntity }
            .forEach { entity ->

                val armorStandTags: List<String>? = if (entity is PlayerEntity) {
                    // Nearby armor stands for players
                    world.getOtherEntities(null, entity.boundingBox.expand(0.5, 2.0, 0.5))
                        .filterIsInstance<ArmorStandEntity>()
                        .mapNotNull { it.customName?.string }
                } else null

                val distance = sqrt(
                    (player.x - entity.x).let { it * it } +
                            (player.y - entity.y).let { it * it } +
                            (player.z - entity.z).let { it * it }
                )

                updatedList += EntityInfo(
                    entity = entity,
                    armorStandTags = armorStandTags,
                    distance = distance
                )
            }

        entityList = updatedList
    }
}