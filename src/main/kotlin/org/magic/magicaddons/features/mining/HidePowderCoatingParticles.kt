package org.magic.magicaddons.features.mining

import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.AddParticleEvent
import org.magic.magicaddons.features.Feature

object HidePowderCoatingParticles : Feature() {

    init {
        EventBus.register(this)
    }


    @EventHandler
    fun onAddParticle(event: AddParticleEvent){
        if (!baseSetting.value) return
        if (!event.packet.particle.type.equals(ParticleTypes.DUST)) return


        val dustPos = Vec3(event.packet.x, event.packet.y, event.packet.z)
        val distance: Double = dustPos.distanceTo(Minecraft.getInstance().player?.position() ?: return)

        event.canceled = distance <= 4.0
        // todo add logic checking if divan armor is equipped (use a cache, dont search armor here)
    }

    override val id: String = "HidePowderCoatingParticles"
    override val displayName: String = "Powder Coating Hider"
    override val tooltipMessage: String = "Hides powder coating particles when divan armor is equipped"
    override val category: String = "mining"

    override val baseSetting: BooleanSetting = BooleanSetting(
            key = "enabled",
            displayName = displayName,
            tooltip = tooltipMessage,
            value = false
        )


}