package org.magic.magicaddons.events.world

import net.minecraft.particle.ParticleEffect
import org.magic.magicaddons.events.Cancellable

class AddParticleEvent(
    val parameters: ParticleEffect,
    val x: Double,
    val y: Double,
    val z: Double,
    val velocityX: Double,
    val velocityY: Double,
    val velocityZ: Double,
    override var canceled: Boolean
) : Cancellable