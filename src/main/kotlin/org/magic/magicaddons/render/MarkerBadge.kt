package org.magic.magicaddons.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.features.customization.Customization
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * The plate a marked mob sits on, and the point that grows out of it when the mob is off screen.
 *
 * Both are one white picture tinted to the palette's colours rather than a shape built out of
 * rectangles: the gui checks every element it is handed against all the others, so a plate made of
 * two dozen rectangles cost a third of the render thread with ten mobs on screen. The ring is the
 * disc in the border colour with the face drawn smaller over it, which is why one picture does for
 * every thickness, and a tint is why one does for every palette.
 */
object MarkerBadge {

    init {
        EventBus.register(this)
    }

    private val DISC: Identifier =
        Identifier.fromNamespaceAndPath("magicaddons", "textures/ui/marker_disc.png")

    private val POINT: Identifier =
        Identifier.fromNamespaceAndPath("magicaddons", "textures/ui/marker_point.png")

    private const val DISC_TEXTURE: Int = 64
    private const val POINT_TEXTURE: Int = 96

    /** How wide the plate is drawn. */
    private const val PLATE: Int = 22

    /** The point's picture, drawn larger than the plate since the tip reaches well past the rim. */
    private const val POINT_DRAWN: Int = 42

    /**
     * Half the border the player picked, and never thinner than a pixel: a ring as thick as a panel's
     * border swallows the mob inside it.
     */
    private val thickness: Float get() = (Customization.borderSize * 0.5f).coerceAtLeast(1f)

    /** Drawn at full strength: a marker the transparency sliders had faded would be worth nothing. */
    private const val OPAQUE: Int = 0xFF000000.toInt()

    // read once rather than per blit: a custom palette builds its colours out of seven typed hex
    // fields every time it is asked. Markers are never drawn while a screen is open, so a colour
    // picked in the config screen is always read again before it can be seen here
    private var face: Int = 0
    private var frame: Int = 0
    private var colorsKnown: Boolean = false

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        colorsKnown = false
    }

    private fun readColors() {
        if (colorsKnown) return

        face = Customization.palette.background or OPAQUE
        frame = Customization.palette.border or OPAQUE
        colorsKnown = true
    }

    /** The plate alone, for a mob the player is already looking at. */
    fun drawRing(graphics: GuiGraphicsExtractor, centerX: Float, centerY: Float) {
        readColors()

        disc(graphics, centerX, centerY, PLATE, frame)
        disc(graphics, centerX, centerY, PLATE - (thickness * 2).roundToInt(), face)
    }

    /** The plate and its point, aimed at a mob that is off the screen. */
    fun drawArrow(graphics: GuiGraphicsExtractor, at: WorldToScreen.ScreenPoint) {
        val window = Minecraft.getInstance().window
        val angle = atan2(
            at.y - window.guiScaledHeight / 2f,
            at.x - window.guiScaledWidth / 2f
        )

        readColors()

        val pose = graphics.pose()

        // turned about the middle of the plate, which is the middle of the point's own picture
        pose.pushMatrix()
        pose.translate(at.x, at.y)
        pose.rotate(angle)
        pose.translate(-POINT_DRAWN / 2f, -POINT_DRAWN / 2f)

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            POINT,
            0, 0,
            0f, 0f,
            POINT_DRAWN, POINT_DRAWN,
            POINT_TEXTURE, POINT_TEXTURE,
            POINT_TEXTURE, POINT_TEXTURE,
            frame
        )

        pose.popMatrix()

        drawRing(graphics, at.x, at.y)
    }

    private fun disc(graphics: GuiGraphicsExtractor, centerX: Float, centerY: Float, size: Int, color: Int) {
        if (size <= 0) return

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            DISC,
            (centerX - size / 2f).roundToInt(), (centerY - size / 2f).roundToInt(),
            0f, 0f,
            size, size,
            DISC_TEXTURE, DISC_TEXTURE,
            DISC_TEXTURE, DISC_TEXTURE,
            color
        )
    }
}
