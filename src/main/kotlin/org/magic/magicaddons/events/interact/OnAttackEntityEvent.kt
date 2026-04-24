package org.magic.magicaddons.events.interact

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.magic.magicaddons.events.Cancellable

class OnAttackEntityEvent @JvmOverloads constructor(val player: Player,
                                                    val target: Entity,
                                                    override var canceled: Boolean = false) : Cancellable