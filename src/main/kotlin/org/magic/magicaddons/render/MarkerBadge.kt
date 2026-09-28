package org.magic.magicaddons.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.magic.magicaddons.Common
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.features.customization.Customization
import kotlin.math.atan2
import kotlin.math.roundToInt

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

    private const val PLATE: Int = 22

    private const val POINT_DRAWN: Int = 42

    private val thickness: Float get() = (Customization.borderSize * 0.5f).coerceAtLeast(1f)

    private var face: Int = 0
    private var frame: Int = 0
    private var colorsKnown: Boolean = false

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        colorsKnown = false
    }

    private fun readColors() {
        if (colorsKnown) return

        face = Customization.palette.background or Common.UI.OPAQUE_ALPHA
        frame = Customization.palette.border or Common.UI.OPAQUE_ALPHA
        colorsKnown = true
    }

    fun drawRing(graphics: GuiGraphicsExtractor, centerX: Float, centerY: Float) {
        readColors()

        disc(graphics, centerX, centerY, PLATE, frame)
        disc(graphics, centerX, centerY, PLATE - (thickness * 2).roundToInt(), face)
    }

    fun drawArrow(graphics: GuiGraphicsExtractor, at: WorldToScreen.ScreenPoint) {
        val window = Minecraft.getInstance().window
        val angle = atan2(
            at.y - window.guiScaledHeight / 2f,
            at.x - window.guiScaledWidth / 2f
        )

        readColors()

        val pose = graphics.pose()

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
