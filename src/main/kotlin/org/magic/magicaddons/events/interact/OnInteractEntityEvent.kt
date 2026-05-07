package org.magic.magicaddons.events.interact

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

class OnInteractEntityEvent(val player: Player, val target: Entity)