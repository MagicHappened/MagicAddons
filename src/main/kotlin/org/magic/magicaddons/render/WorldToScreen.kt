package org.magic.magicaddons.render

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where a point in the world lands on the hud. A point off the screen, or behind the camera, comes
 * back pinned to the edge it lies past, so a marker can still point at it.
 */
object WorldToScreen {

    class ScreenPoint(val x: Float, val y: Float, val onScreen: Boolean)

    /** How far inside the screen an edge marker sits, so it is not drawn half off. */
    private const val EDGE_INSET: Float = 14f

    fun of(point: Vec3): ScreenPoint {
        val minecraft = Minecraft.getInstance()
        val camera = McCompat.camera()
        val window = minecraft.window

        val projected = minecraft.gameRenderer.projectPointToScreen(point)
        val behind = lookVector(camera).dot(point.subtract(camera.position())) <= 0

        // a point behind the camera projects mirrored through the middle, so both axes are turned
        // back around before the direction is read off them
        val ndcX = if (behind) -projected.x else projected.x
        val ndcY = if (behind) -projected.y else projected.y

        val width = window.guiScaledWidth.toFloat()
        val height = window.guiScaledHeight.toFloat()

        val onScreen = !behind && abs(ndcX) <= 1.0 && abs(ndcY) <= 1.0
        if (onScreen) {
            return ScreenPoint(
                ((ndcX + 1) / 2 * width).toFloat(),
                ((1 - ndcY) / 2 * height).toFloat(),
                true
            )
        }

        return atEdge(ndcX.toFloat(), ndcY.toFloat(), width, height)
    }

    /**
     * The point on the screen's border in the direction of a target that is not on it. The direction
     * is scaled until it meets the first border it would cross.
     */
    private fun atEdge(ndcX: Float, ndcY: Float, width: Float, height: Float): ScreenPoint {
        val halfWidth = width / 2 - EDGE_INSET
        val halfHeight = height / 2 - EDGE_INSET

        // in screen terms rather than clip terms, so y grows downwards like every other hud position
        val dirX = ndcX * halfWidth
        val dirY = -ndcY * halfHeight

        val longest = maxOf(abs(dirX) / halfWidth, abs(dirY) / halfHeight)
        if (longest <= 0f) return ScreenPoint(width / 2, EDGE_INSET, false)

        return ScreenPoint(width / 2 + dirX / longest, height / 2 + dirY / longest, false)
    }

    /** The way the camera faces, from its own angles rather than its rotation. */
    private fun lookVector(camera: Camera): Vec3 {
        val pitch = Math.toRadians(camera.xRot().toDouble())
        val yaw = Math.toRadians(camera.yRot().toDouble())
        val cosPitch = cos(pitch)

        return Vec3(-sin(yaw) * cosPitch, -sin(pitch), cos(yaw) * cosPitch)
    }
}
