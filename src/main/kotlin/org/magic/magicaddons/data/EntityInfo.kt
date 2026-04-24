package org.magic.magicaddons.data

import net.minecraft.world.entity.Entity

data class EntityInfo(
    val entity: Entity,
    val informationEntities: List<Entity>?,
    val distance: Double
)