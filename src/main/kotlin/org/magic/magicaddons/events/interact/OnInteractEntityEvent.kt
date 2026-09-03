package org.magic.magicaddons.events.interact

import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.magic.magicaddons.events.Cancellable

/** The player right clicked an entity. */
class OnInteractEntityEvent @JvmOverloads constructor(
    val player: Player,
    val target: Entity,
    val hand: InteractionHand,
    override var canceled: Boolean = false
) : Cancellable
