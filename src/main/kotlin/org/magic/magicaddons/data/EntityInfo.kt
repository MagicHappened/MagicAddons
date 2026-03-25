package org.magic.magicaddons.data

import net.minecraft.entity.Entity

data class EntityInfo(
    val entity: Entity,
    val armorStandTags: List<String>?,
    val distance: Double
)