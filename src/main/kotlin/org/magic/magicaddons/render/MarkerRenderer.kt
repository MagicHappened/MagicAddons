package org.magic.magicaddons.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.render.HudRenderEvent
import org.magic.magicaddons.features.misc.HighlightMarkers
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.ScreenUtil.drawItem
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.roundToInt
import kotlin.math.sqrt

object MarkerRenderer {

    init {
        EventBus.register(this)
    }

    private const val ICON_SIZE: Int = 14

    private const val NAME_GAP: Int = 6

    private class Marker(
        val entity: Entity,
        val mark: EntityUtils.HighlightMark,
        val color: Int,
        val at: WorldToScreen.ScreenPoint,
        val distance: Double,
        val named: Boolean
    ) {
        val iconString: String = mark.icon?.item?.toString() ?: entity.type.toString()
    }

    @EventHandler
    fun onHudRender(event: HudRenderEvent) {
        if (McCompat.hudHidden() || McCompat.currentScreen() != null) return

        val markers = collect().sortedByDescending { it.distance }

        bakedAnIconThisFrame = false

        drawTracer(event.graphics)

        markers.forEach { draw(event.graphics, it) }
    }

    private fun drawTracer(graphics: GuiGraphicsExtractor) {
        if (!HighlightMarkers.tracerSetting.value || !HighlightMarkers.baseSetting.value) return

        val player = Minecraft.getInstance().player ?: return
        val window = Minecraft.getInstance().window

        val nearest = EntityUtils.resolvedMap
            .filter { (entity, source) ->
                entity.isAlive && source.throughWalls && HighlightMarkers.marks(source)
            }
            .minByOrNull { (entity, _) -> entity.distanceToSqr(player) } ?: return

        val at = WorldToScreen.screenPointOf(nearest.key.position().add(0.0, nearest.key.bbHeight / 2.0, 0.0))

        graphics.drawLine(
            window.guiScaledWidth / 2,
            window.guiScaledHeight / 2,
            at.x.roundToInt(),
            at.y.roundToInt(),
            TRACER_THICKNESS,
            nearest.value.highlightColor(nearest.key)
        )
    }

    private const val TRACER_THICKNESS: Int = 1

    private fun collect(): List<Marker> {
        val player = Minecraft.getInstance().player ?: return emptyList()
        val found = mutableListOf<Marker>()

        if (!HighlightMarkers.markingEnabled()) return emptyList()

        EntityUtils.resolvedMap.forEach { (entity, source) ->
            if (!source.throughWalls || !entity.isAlive || !HighlightMarkers.marks(source)) return@forEach

            val distance = sqrt(entity.distanceToSqr(player))
            if (distance < HighlightMarkers.distanceSetting.value) return@forEach

            val mark = source.highlightMark(entity) ?: return@forEach
            val at = WorldToScreen.screenPointOf(entity.position().add(0.0, entity.bbHeight / 2.0, 0.0))

            if (at.isOnScreen && !HighlightMarkers.iconSetting.value) return@forEach
            if (!at.isOnScreen && !HighlightMarkers.arrowsSetting.value) return@forEach

            found.add(
                Marker(entity, mark, source.highlightColor(entity), at, distance, HighlightMarkers.alwaysNameSetting.value)
            )
        }

        return named(found)
    }

    private fun named(markers: List<Marker>): List<Marker> {
        val alike = markers.groupBy { it.iconString }.filterValues { it.size > 1 }.keys

        return markers.map { marker ->
            if (marker.named || marker.iconString in alike) marker.withName() else marker
        }
    }

    private fun Marker.withName(): Marker = Marker(entity, mark, color, at, distance, named = true)

    private fun draw(graphics: GuiGraphicsExtractor, marker: Marker) {

        val centerX = marker.at.x.roundToInt()
        val centerY = marker.at.y.roundToInt()
        val left = centerX - ICON_SIZE / 2
        val top = centerY - ICON_SIZE / 2

        if (marker.at.isOnScreen) {
            MarkerBadge.drawRing(graphics, centerX.toFloat(), centerY.toFloat())
        } else {
            MarkerBadge.drawArrow(graphics, marker.at)
        }

        val icon = marker.mark.icon
        if (icon != null) {
            graphics.drawItem(icon, left, top, ICON_SIZE, ICON_SIZE)
        } else {
            drawEntityIcon(graphics, marker, left, top, ICON_SIZE)
        }

        if (!marker.named || !marker.at.isOnScreen) return

        val font = Minecraft.getInstance().font
        val nameWidth = font.width(marker.mark.name)
        graphics.text(
            font,
            marker.mark.name,
            centerX - nameWidth / 2,
            top + ICON_SIZE + NAME_GAP,
            marker.color,
            true
        )
    }

    private fun drawEntityIcon(graphics: GuiGraphicsExtractor, marker: Marker, left: Int, top: Int, size: Int) {
        val entity = marker.entity

        @Suppress("UNCHECKED_CAST")
        val typed = Minecraft.getInstance().entityRenderDispatcher.getRenderer(entity)
                as EntityRenderer<Entity, EntityRenderState>
        val state = typed.createRenderState(entity, 1f)
        typed.extractRenderState(entity, state, 1f)

        (state as? LivingEntityRenderState)?.let { living ->
            living.bodyRot = 180f
            living.yRot = 180f
            living.xRot = 0f
        }

        val tallest = maxOf(entity.bbWidth, entity.bbHeight).coerceAtLeast(MIN_MODEL_SIZE)
        val bounds = ScreenRectangle(left, top, size, size)

        val drawn = GuiEntityRenderState(
            state,
            Vector3f(0f, entity.bbHeight / 2f, 0f),
            Quaternionf().rotateZ(Math.PI.toFloat()),
            Quaternionf(),
            left,
            top,
            left + size,
            top + size,
            size / tallest * MODEL_MARGIN,
            bounds
        )

        val icon = bakedIcons.getOrPut(marker.iconKey) { BakedEntityIcon() }

        if (!icon.isBaked && bakedAnIconThisFrame) {
            graphics.guiRenderState.addPicturesInPictureState(drawn)
            return
        }

        bakedAnIconThisFrame = bakedAnIconThisFrame || !icon.isBaked
        icon.submit(drawn, graphics.guiRenderState)
    }

    private val Marker.iconKey: String
        get() = mark.name + if ((entity as? LivingEntity)?.isBaby == true) " baby" else ""

    private val bakedIcons: MutableMap<String, BakedEntityIcon> =
        object : LinkedHashMap<String, BakedEntityIcon>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BakedEntityIcon>): Boolean {
                if (size <= MAX_BAKED_ICONS) return false
                eldest.value.close()
                return true
            }
        }

    private const val MAX_BAKED_ICONS: Int = 64

    private var bakedAnIconThisFrame: Boolean = false

    private const val MIN_MODEL_SIZE: Float = 0.1f

    private const val MODEL_MARGIN: Float = 0.9f
}
