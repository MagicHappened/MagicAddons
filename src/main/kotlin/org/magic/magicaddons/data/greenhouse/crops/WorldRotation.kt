package org.magic.magicaddons.data.greenhouse.crops

import net.minecraft.world.phys.Vec3

// skyblock turns the plants by 90 degrees * ((z - x) mod 4)
object WorldRotation {

    fun quarterTurnsAt(x: Int, z: Int): Int = Math.floorMod(z - x, 4)

    fun turned(offset: Vec3, quarterTurns: Int): Vec3 = when (Math.floorMod(quarterTurns, 4)) {
        1 -> Vec3(-offset.z, offset.y, offset.x)
        2 -> Vec3(-offset.x, offset.y, -offset.z)
        3 -> Vec3(offset.z, offset.y, -offset.x)
        else -> offset
    }
}
