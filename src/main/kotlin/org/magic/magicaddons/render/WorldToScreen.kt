package org.magic.magicaddons.render

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object WorldToScreen {

    class ScreenPoint(val x: Float, val y: Float, val isOnScreen: Boolean)

    private const val EDGE_INSET: Float = 14f

    fun screenPointOf(worldPoint: Vec3): ScreenPoint {
        val minecraft = Minecraft.getInstance()
        val camera = McCompat.camera()
        val window = minecraft.window

        val projected = minecraft.gameRenderer.projectPointToScreen(worldPoint)
        val isBehindCamera = cameraLookVector(camera).dot(worldPoint.subtract(camera.position())) <= 0

        val ndcX = if (isBehindCamera) -projected.x else projected.x
        val ndcY = if (isBehindCamera) -projected.y else projected.y

        val screenWidth = window.guiScaledWidth.toFloat()
        val screenHeight = window.guiScaledHeight.toFloat()

        val isOnScreen = !isBehindCamera && abs(ndcX) <= 1.0 && abs(ndcY) <= 1.0
        if (isOnScreen) {
            return ScreenPoint(
                ((ndcX + 1) / 2 * screenWidth).toFloat(),
                ((1 - ndcY) / 2 * screenHeight).toFloat(),
                true
            )
        }

        return pointOnScreenEdge(ndcX.toFloat(), ndcY.toFloat(), screenWidth, screenHeight)
    }

    private fun pointOnScreenEdge(ndcX: Float, ndcY: Float, screenWidth: Float, screenHeight: Float): ScreenPoint {
        val halfWidth = screenWidth / 2 - EDGE_INSET
        val halfHeight = screenHeight / 2 - EDGE_INSET

        val directionX = ndcX * halfWidth
        val directionY = -ndcY * halfHeight

        val edgeScale = maxOf(abs(directionX) / halfWidth, abs(directionY) / halfHeight)
        if (edgeScale <= 0f) return ScreenPoint(screenWidth / 2, EDGE_INSET, false)

        return ScreenPoint(screenWidth / 2 + directionX / edgeScale, screenHeight / 2 + directionY / edgeScale, false)
    }

    private fun cameraLookVector(camera: Camera): Vec3 {
        val pitch = Math.toRadians(camera.xRot().toDouble())
        val yaw = Math.toRadians(camera.yRot().toDouble())
        val cosPitch = cos(pitch)

        return Vec3(-sin(yaw) * cosPitch, -sin(pitch), cos(yaw) * cosPitch)
    }
}
