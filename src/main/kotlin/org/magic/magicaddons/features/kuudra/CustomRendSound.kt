package org.magic.magicaddons.features.kuudra

import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.registry.Registries
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.AddParticleEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils

object CustomRendSound : Feature() {

    override val id: String = "CustomRendSound"
    override val displayName: String = "Custom Rend Sound"
    override val tooltipMessage: String = "Plays a custom selected sound when a rend pull is detected"
    override val category: String = "kuudra"
    override val baseSetting: BooleanSetting = BooleanSetting(
        key = "enabled",
        displayName = displayName,
        tooltip = tooltipMessage,
        value = false,
        children = listOf(
            TextSetting(
                key = "RendPullSoundPath",
                displayName = "Sound Path",
                tooltip = "The sound path for the rend sound",
                value = "mob.goat.death.screamer"
            )
        )
    )

    @EventHandler
    fun onAddParticleEvent(event: AddParticleEvent) {

        if (!baseSetting.value) return

        val id = Registries.PARTICLE_TYPE.getId(event.parameters.type) // minecraft redstone block
        val blockStateParticleEffect = event.parameters as? BlockStateParticleEffect ?: return
        val blockState = blockStateParticleEffect.blockState
        if (blockState != null) {
            val blockId = Registries.BLOCK.getId(blockState.block)

            ChatUtils.sendWithPrefix(
                "Particle: $id | Block: $blockId | State: $blockState"
            )
        } else {
            ChatUtils.sendWithPrefix("Particle: $id")
        }

    }


}