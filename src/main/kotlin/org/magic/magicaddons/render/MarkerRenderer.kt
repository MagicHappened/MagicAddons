package org.magic.magicaddons.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf
import org.joml.Vector3f
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.render.HudRenderEvent
import org.magic.magicaddons.features.misc.HighlightMarkers
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * What a highlight far enough away to be only a dot is drawn as: the mob itself over it, and an
 * arrow on the edge of the screen for one that is behind the player. What is drawn, and for which
 * features, is [HighlightMarkers].
 */
object MarkerRenderer {

    init {
        EventBus.register(this)
    }

    /** Small enough to sit inside the ring drawn around it, and even so it centres on a whole pixel. */
    private const val ICON_SIZE: Int = 14

    /** How far the name sits under an icon, clear of the ring around it. */
    private const val NAME_GAP: Int = 6

    /** One highlight worth drawing this frame, as everything the drawing needs. */
    private class Marker(
        val entity: Entity,
        val mark: EntityUtils.HighlightMark,
        val color: Int,
        val at: WorldToScreen.ScreenPoint,
        val distance: Double,
        val named: Boolean
    ) {
        /** Two markers sharing this look alike, and are the case a name has to tell apart. */
        val look: String = mark.icon?.item?.toString() ?: entity.type.toString()
    }

    @EventHandler
    fun onHudRender(event: HudRenderEvent) {
        if (McCompat.hudHidden() || McCompat.currentScreen() != null) return

        val markers = collect().sortedByDescending { it.distance }

        drawTracer(event.graphics)

        // the furthest first, so the nearest marker ends up on top of the pile
        markers.forEach { draw(event.graphics, it) }
    }

    /**
     * A line from the crosshair to the nearest highlighted mob, drawn under the markers. It leads to
     * the nearest mob rather than the nearest marker: one close enough to be worth walking to is
     * usually too close to have been marked at all.
     */
    private fun drawTracer(graphics: GuiGraphicsExtractor) {
        if (!HighlightMarkers.tracerSetting.value || !HighlightMarkers.baseSetting.value) return

        val player = Minecraft.getInstance().player ?: return
        val window = Minecraft.getInstance().window

        val nearest = EntityUtils.resolvedMap
            .filter { (entity, source) ->
                entity.isAlive && source.throughWalls && HighlightMarkers.marks(source)
            }
            .minByOrNull { (entity, _) -> entity.distanceToSqr(player) } ?: return

        val at = WorldToScreen.of(nearest.key.position().add(0.0, nearest.key.bbHeight / 2.0, 0.0))

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

        if (!HighlightMarkers.marking()) return emptyList()

        EntityUtils.resolvedMap.forEach { (entity, source) ->
            if (!source.throughWalls || !entity.isAlive || !HighlightMarkers.marks(source)) return@forEach

            val distance = sqrt(entity.distanceToSqr(player))
            if (distance < HighlightMarkers.distanceSetting.value) return@forEach

            val mark = source.highlightMark(entity) ?: return@forEach
            val at = WorldToScreen.of(entity.position().add(0.0, entity.bbHeight / 2.0, 0.0))

            if (at.onScreen && !HighlightMarkers.iconSetting.value) return@forEach
            if (!at.onScreen && !HighlightMarkers.arrowsSetting.value) return@forEach

            found.add(
                Marker(entity, mark, source.highlightColor(entity), at, distance, HighlightMarkers.alwaysNameSetting.value)
            )
        }

        return named(found)
    }

    /** Names the markers the player could not otherwise tell apart, and any the settings always name. */
    private fun named(markers: List<Marker>): List<Marker> {
        val alike = markers.groupBy { it.look }.filterValues { it.size > 1 }.keys

        return markers.map { marker ->
            if (marker.named || marker.look in alike) marker.withName() else marker
        }
    }

    private fun Marker.withName(): Marker = Marker(entity, mark, color, at, distance, named = true)

    private fun draw(graphics: GuiGraphicsExtractor, marker: Marker) {
        // the icon is laid out from the same whole pixel the plate is drawn around, since a box half
        // a pixel out lands the mob off the middle of its own marker
        val centerX = marker.at.x.roundToInt()
        val centerY = marker.at.y.roundToInt()
        val left = centerX - ICON_SIZE / 2
        val top = centerY - ICON_SIZE / 2

        if (marker.at.onScreen) {
            MarkerBadge.drawRing(graphics, centerX.toFloat(), centerY.toFloat())
        } else {
            MarkerBadge.drawArrow(graphics, marker.at)
        }

        val icon = marker.mark.icon
        if (icon != null) {
            graphics.renderFakeItem(icon, left, top, ICON_SIZE, ICON_SIZE)
        } else {
            drawEntityIcon(graphics, marker.entity, left, top, ICON_SIZE)
        }

        // a name on the edge of the screen would be written off it, so an arrow carries none
        if (!marker.named || !marker.at.onScreen) return

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

    /**
     * The mob itself drawn into the marker, which is the only picture that exists for a mob with no
     * item to stand for it, and the only one that tells two shulkers of different colours apart.
     */
    private fun drawEntityIcon(graphics: GuiGraphicsExtractor, entity: Entity, left: Int, top: Int, size: Int) {
        @Suppress("UNCHECKED_CAST")
        val typed = Minecraft.getInstance().entityRenderDispatcher.getRenderer(entity)
                as EntityRenderer<Entity, EntityRenderState>
        val state = typed.createRenderState(entity, 1f)
        typed.extractRenderState(entity, state, 1f)

        // faced at the player rather than wherever the mob happens to be looking
        (state as? LivingEntityRenderState)?.let { living ->
            living.bodyRot = 180f
            living.yRot = 180f
            living.xRot = 0f
        }

        val tallest = maxOf(entity.bbWidth, entity.bbHeight).coerceAtLeast(MIN_MODEL_SIZE)
        val bounds = ScreenRectangle(left, top, size, size)

        graphics.guiRenderState.addPicturesInPictureState(
            GuiEntityRenderState(
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
        )
    }

    /** A model this small or smaller is scaled as if it were this big, so nothing divides by nothing. */
    private const val MIN_MODEL_SIZE: Float = 0.1f

    /** How much of the marker the model fills, leaving a little room around it. */
    private const val MODEL_MARGIN: Float = 0.9f
}
