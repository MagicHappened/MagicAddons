package org.magic.magicaddons.util

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen

object ScreenUtil {


    private var newScreen: Screen? = null

    fun setScreen(screen: Screen) {
        newScreen = screen
    }

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            val target = newScreen ?: return@register

            if (MinecraftClient.getInstance().currentScreen !== target) {
                MinecraftClient.getInstance().setScreen(target)
            } else {
                newScreen = null
            }
        }
    }
    fun drawBorder(
        ctx: DrawContext,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        thickness: Int,
        color: Int
    ) {
        ctx.fill(x1, y1, x2, y1 + thickness, color)
        ctx.fill(x1, y2 - thickness, x2, y2, color)
        ctx.fill(x1, y1, x1 + thickness, y2, color)
        ctx.fill(x2 - thickness, y1, x2, y2, color)
    }

    fun drawSquareBorder(ctx: DrawContext, x: Int, y: Int, size: Int, thickness: Int, color: Int) {
        ctx.fill(x, y, x + size, y + thickness, color)
        ctx.fill(x, y + size - thickness, x + size, y + size, color)
        ctx.fill(x, y, x + thickness, y + size, color)
        ctx.fill(x + size - thickness, y, x + size, y + size, color)
    }

    fun drawLine(
        ctx: DrawContext,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        thickness: Int,
        color: Int
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val steps = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)).toInt()

        if (steps == 0) return

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = (x1 + dx * t).toInt()
            val y = (y1 + dy * t).toInt()

            // thickness (centered square)
            val half = thickness / 2
            ctx.fill(
                x - half,
                y - half,
                x + half + 1,
                y + half + 1,
                color
            )
        }
    }
}