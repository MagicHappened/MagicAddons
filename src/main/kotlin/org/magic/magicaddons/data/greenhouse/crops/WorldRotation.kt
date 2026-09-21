package org.magic.magicaddons.data.greenhouse.crops

import kotlin.math.floor
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3

/** how skyblock turns its plants, a quarter turn per `(z - x) mod 4` of the base block */
object WorldRotation {

    fun quarterTurnsAt(x: Int, z: Int): Int = Math.floorMod(z - x, 4)

    fun turned(offset: Vec3, quarterTurns: Int): Vec3 = when (Math.floorMod(quarterTurns, 4)) {
        1 -> Vec3(-offset.z, offset.y, offset.x)
        2 -> Vec3(-offset.x, offset.y, -offset.z)
        3 -> Vec3(offset.z, offset.y, -offset.x)
        else -> offset
    }
}
