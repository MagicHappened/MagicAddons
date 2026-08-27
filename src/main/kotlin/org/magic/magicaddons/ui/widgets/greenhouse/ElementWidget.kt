package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import org.magic.magicaddons.Common
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem

class ElementWidget(val instance: GreenhouseElementInstance) : Renderable, Focusable {
    var widgetX: Int = 0
    var widgetY: Int = 0
    var padding: Int = 0
    var width = 50
    var height = 50
    var sprite: TextureAtlasSprite? = ScreenUtil.getSpriteForState(Blocks.FIRE.defaultBlockState(),Direction.NORTH)
    var renderedStack: ItemStack = ItemStack.EMPTY
    var markingColor: Int? = null
    override var focusedState: Boolean = false

    /**
     * A single fact about a plant, small enough to write over the plant itself. [color] is how the
     * hover controls stand for this fact, the swatches carry no text of their own.
     */
    enum class HoverInfo(val color: Int) {
        GrowthStage(0xFF3FBF3F.toInt()),
        WaterLevel(0xFF3F7FDF.toInt()),
        DecayTime(0xFFCC3333.toInt());

        /** This fact about [instance], or null while the game has not told us the value yet. */
        fun valueFor(instance: GreenhouseElementInstance): String? = when (this) {
            GrowthStage -> when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> "${stage.stage}/${instance.cropDef.maxStage}"
                // a guessed stage is worth showing, as long as it does not look measured
                is GrowthStageInfo.Estimated -> "~${stage.range.first}-${stage.range.last}"
                null -> null
            }
            WaterLevel -> instance.waterLevel?.let { "$it" }
            DecayTime -> decayRemainingMs(instance)?.let { readableDuration(it) }
        }
    }

    fun init(){
        markingColor = instance.slot.slotMark?.color
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        markingColor?.let {
            graphics.drawBorder(
                widgetX + 1,
                widgetY + 1,
                widgetX + width - 1,
                widgetY + height - 1,
                1,
                it
            )
        }
        if (instance.elementId == "Fire") {
            renderFire(graphics, mouseX, mouseY, deltaTick)
            return
        }
        graphics.renderFakeItem(
            renderedStack,
            widgetX + padding,
            widgetY + padding,
            width - padding * 2,
            height - padding * 2
        )
    }

    fun renderFire(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float){
        val sprite = sprite
        sprite ?: return
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            widgetX,
            widgetY,
            width,
            height
        )
    }

    /**
     * Writes [info] over the plant itself, for the one fact the player pinned to always be visible.
     * Nothing is drawn while that fact is unknown, an empty backdrop says less than the plant does.
     */
    fun renderHoverButtonInfo(graphics: GuiGraphicsExtractor, info: HoverInfo) {
        val text = info.valueFor(instance) ?: return
        val font = Minecraft.getInstance().font

        // a hundred slots share the grid, full size text would not fit inside one of them
        val textWidth = font.width(text) * INFO_TEXT_SCALE
        val textHeight = font.lineHeight * INFO_TEXT_SCALE
        val textX = widgetX + (width - textWidth) / 2f
        val textY = widgetY + height - textHeight - 1f

        // the plant behind it is busy, the text needs its own ground to stay readable
        graphics.fill(
            (textX - 1f).toInt(),
            (textY - 1f).toInt(),
            (textX + textWidth + 1f).toInt(),
            (textY + textHeight).toInt(),
            Common.UI.OVERLAY_BACKGROUND_COLOR
        )

        val pose = graphics.pose()
        pose.pushMatrix()

        try {
            pose.translate(textX, textY)
            pose.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE)
            graphics.text(font, text, 0, 0, Common.UI.OVERLAY_TEXT_COLOR, false)
        } finally {
            pose.popMatrix()
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            ChatUtils.sendWithPrefix("Clicked on ${instance.elementId} ")
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in widgetX..widgetX+width
                && mouseY.toInt() in widgetY..widgetY + height)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
    }
    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font
        val cropDefinition = instance.cropDef

        val lines = buildList {
            add(Component.literal(cropDefinition.name).withStyle(ChatFormatting.GREEN))

            instance.slot.slotMark?.let { marking ->
                add(labelled("Role", marking.name))
            }

            val growthText = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known ->
                    "${stage.stage}/${cropDefinition.maxStage}"

                is GrowthStageInfo.Estimated ->
                    "${stage.range.first}-${stage.range.last}/${cropDefinition.maxStage} (estimated)"

                null -> null
            }

            growthText?.let { add(labelled("Growth", it)) }

            // a plant that never drinks has no water level worth a line of its own
            if (cropDefinition.needsWater) {
                add(labelled("Water", instance.waterLevel?.toString() ?: "Unknown"))
            }

            decayRemainingMs(instance)?.let { add(labelled("Decays in", readableDuration(it))) }

            val footprint = cropDefinition.footprint
            if (footprint.width > 1 || footprint.height > 1) {
                add(labelled("Size", "${footprint.width}x${footprint.height}"))
            }
        }

        val components = lines.map { ClientTooltipComponent.create(it.visualOrderText) }

        graphics.tooltip(
            font,
            components,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        )
    }




    companion object {
        /** A slot is small, but the numbers still have to be legible from across the grid. */
        private const val INFO_TEXT_SCALE: Float = 0.75f

        private fun labelled(label: String, value: String): Component =
            Component.literal("$label: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE))

        /**
         * How long this plant has before it decays, or null when either the plant never decays or
         * its age was never read off a plant diagnostic.
         */
        private fun decayRemainingMs(instance: GreenhouseElementInstance): Long? {
            val decayTime = instance.cropDef.decayTimeMs ?: return null
            val age = instance.age ?: return null

            return (decayTime - age).coerceAtLeast(0L)
        }

        /** [ms] as the two largest units that fit, the way the game words its own timers. */
        private fun readableDuration(ms: Long): String {
            val seconds = ms / 1000

            val parts = listOf(
                "d" to seconds / 86400,
                "h" to seconds % 86400 / 3600,
                "m" to seconds % 3600 / 60,
                "s" to seconds % 60
            ).filter { it.second > 0L }

            if (parts.isEmpty()) return "0s"

            return parts.take(2).joinToString(" ") { "${it.second}${it.first}" }
        }
    }
}