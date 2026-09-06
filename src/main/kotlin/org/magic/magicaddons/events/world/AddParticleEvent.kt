package org.magic.magicaddons.events.world


import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import org.magic.magicaddons.events.Cancellable

class AddParticleEvent @JvmOverloads constructor(
    val packet: ClientboundLevelParticlesPacket,
    override var canceled: Boolean = false
) : Cancellable
