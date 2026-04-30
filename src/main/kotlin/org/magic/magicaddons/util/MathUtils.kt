package org.magic.magicaddons.util

import net.minecraft.world.phys.Vec3

object MathUtils {
    fun rotateOffset(offset: Vec3, rotation: Int): Vec3 {
        return when (rotation % 4) {
            0 -> offset // north
            1 -> Vec3(-offset.z, offset.y, offset.x)   // west
            2 -> Vec3(-offset.x, offset.y, -offset.z)  // south
            3 -> Vec3(offset.z, offset.y, -offset.x)   // east
            else -> offset
        }
    }

}