package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawCheckerboard

class SlotWidget(
    val slot: LayoutSlot,
    isInPreset: Boolean
) : Renderable {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    private val isDrawnAsAir: Boolean = isInPreset && slot.soil == Blocks.AIR

    private val sprite: TextureAtlasSprite? = slot.soil
        ?.takeIf { it != Blocks.AIR }
        ?.let { ScreenUtil.spriteFor(it.defaultBlockState(), Direction.UP) }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (isDrawnAsAir) {
            graphics.drawCheckerboard(x, y, x + width, y + height)
            return
        }
        val sprite = sprite ?: return
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
    }
}
