package org.magic.magicaddons.events.interact

import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import org.magic.magicaddons.events.Cancellable

class OnAttackEntityEvent @JvmOverloads constructor(val player: PlayerEntity,
                                                    val target: Entity,
                                                    override var canceled: Boolean = false) : Cancellable