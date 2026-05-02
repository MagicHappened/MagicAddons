package org.magic.magicaddons.events.interact

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult

class OnBlockUseEvent(val player: LocalPlayer,val hit: BlockHitResult, val result: InteractionResult)