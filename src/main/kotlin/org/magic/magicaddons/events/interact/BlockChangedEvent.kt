package org.magic.magicaddons.events.interact

import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket

class BlockChangedEvent(val packet: ClientboundBlockUpdatePacket)