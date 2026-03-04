package org.magic.magicaddons.features.mining

import org.magic.magicaddons.events.BusSubscriber
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.AddParticleEvent

object HidePowderCoatingParticles : BusSubscriber() {

    @EventHandler
    fun onAddParticle(event: AddParticleEvent){

    }

}