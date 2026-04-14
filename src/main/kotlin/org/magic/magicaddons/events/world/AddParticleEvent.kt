package org.magic.magicaddons.events.world

import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import org.magic.magicaddons.events.Cancellable

class AddParticleEvent @JvmOverloads constructor(
    var packet: ParticleS2CPacket,
    override var canceled: Boolean = false
) : Cancellable

/*

        val blockStateParticleEffect = event.parameters as? BlockStateParticleEffect ?: return
        val block = blockStateParticleEffect.blockState.block

        if (Registries.BLOCK.getId(block).path != "redstone_block") {
            return
        }

 */