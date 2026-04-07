package org.magic.magicaddons.extensions

import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.EntityEquipment
import org.magic.mixins.LivingEntityAccessor

val AbstractClientPlayerEntity.armorStacks: EntityEquipment
    get() = (this as LivingEntityAccessor).equipment