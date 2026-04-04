package org.magic.magicaddons.features.mining

import net.minecraft.client.MinecraftClient
import net.minecraft.particle.DustParticleEffect
import net.minecraft.util.math.Vec3d
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

        val dust = event.parameters as? DustParticleEffect ?: return
        val dustPos = Vec3d(event.x, event.y, event.z)
        val distance: Double = dustPos.distanceTo(MinecraftClient.getInstance().player?.entityPos ?: return)

        event.canceled = distance <= 4.0
        // todo add logic checking if divan armor is equipped (use a cache, dont search armor here)
    }

    override val id: String = "HidePowderCoatingParticles"
    override val displayName: String = "Powder Coating Hider"
    override val tooltipMessage: String = "Hides powder coating particles when divan armor is equipped"
    override val category: String = "mining"

    override val baseSetting: BooleanSetting by lazy {
        BooleanSetting(
            key = "enabled",
            displayName = displayName,
            tooltip = tooltipMessage,
            value = false
        )
    }

}