package org.magic.magicaddons.events.interact

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.phys.BlockHitResult

class BlockUseEvent(val player: LocalPlayer,val hit: BlockHitResult)