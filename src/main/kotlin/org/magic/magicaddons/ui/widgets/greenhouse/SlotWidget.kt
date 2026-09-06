package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.util.ScreenUtil.drawCheckerboard
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.util.ScreenUtil

/** One slot of a grid: the soil asked for, or the checkerboard when air is asked for. */
class SlotWidget(
    val slot: LayoutSlot
) : Renderable {

    private var sprite: TextureAtlasSprite? = null

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    /** Whether the slot asks for air, drawn as the checkerboard; a slot with nothing set draws nothing. */
    private var air: Boolean = false

    fun init() {
        val block = slot.placedBlock
        air = block?.block == Blocks.AIR
        sprite = if (block == null || air) null else ScreenUtil.getSpriteForState(block, Direction.UP)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (air) {
            graphics.drawCheckerboard(x, y, x + width, y + height)
            return
        }
        val sprite = sprite ?: return
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
    }
}
