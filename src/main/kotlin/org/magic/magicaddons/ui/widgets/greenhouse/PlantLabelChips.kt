package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.modText

class PlantLabelChips(private val onTeleport: () -> Unit) {

    private val font = Minecraft.getInstance().font

    private val chips: Map<PlantLabel, ClickableButtonWidget> = PlantLabel.entries.associateWith { label ->
        ClickableButtonWidget(font.width(label.chipName) + DOT_SIZE + Common.UI.TEXT_X_PAD * 3, HEIGHT) { graphics ->
            val dotTop = y + (height - DOT_SIZE) / 2
            graphics.fill(x + Common.UI.TEXT_X_PAD, dotTop, x + Common.UI.TEXT_X_PAD + DOT_SIZE, dotTop + DOT_SIZE, label.color)
            graphics.modText(font, label.chipName, x + Common.UI.TEXT_X_PAD * 2 + DOT_SIZE, y + (height - font.lineHeight) / 2 + 1, Common.UI.TEXT_COLOR)
        }
    }

    private val teleportChip = ClickableButtonWidget(font.width(TELEPORT_LABEL) + Common.UI.TEXT_X_PAD * 2, HEIGHT, Component.literal(TELEPORT_LABEL))

    var isTeleportShown: Boolean = false

    private var labelX: Int = 0
    private var top: Int = 0

    fun layoutAlong(left: Int, top: Int) {
        this.top = top
        labelX = left
        var chipX = left + font.width(ROW_LABEL) + Common.UI.SPACING
        PlantLabel.entries.forEach { label ->
            val chip = chips.getValue(label)
            chip.x = chipX
            chip.y = top
            chipX += chip.width + Common.UI.SPACING
        }
        teleportChip.x = chipX + Common.UI.SPACING
        teleportChip.y = top
    }

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.modText(font, ROW_LABEL, labelX, top + (HEIGHT - font.lineHeight) / 2 + 1, Common.UI.TEXT_DIM_COLOR)
        chips.forEach { (label, chip) ->
            chip.isPressed = label == selectedLabel
            chip.extractRenderState(graphics, mouseX, mouseY, delta)
        }
        if (isTeleportShown) teleportChip.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        chips.values.forEach { it.mouseMoved(mouseX, mouseY) }
        teleportChip.mouseMoved(mouseX, mouseY)
    }

    fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        chips.forEach { (label, chip) ->
            if (chip.mouseClicked(event, doubled)) {
                selectedLabel = if (label == selectedLabel) null else label
                return true
            }
        }
        if (isTeleportShown && teleportChip.mouseClicked(event, doubled)) {
            onTeleport()
            return true
        }
        return false
    }

    fun cycleSelectedLabel(down: Boolean) {
        val labels = PlantLabel.entries
        val current = selectedLabel

        selectedLabel = when (current) {
            null -> if (down) labels.first() else labels.last()
            else -> labels[(current.ordinal + (if (down) 1 else -1) + labels.size) % labels.size]
        }
    }

    companion object {
        const val HEIGHT: Int = 20

        private const val DOT_SIZE: Int = 7

        private const val ROW_LABEL: String = "Show on plants:"
        private const val TELEPORT_LABEL: String = "Teleport to Plot"

        var selectedLabel: PlantLabel? = PlantLabel.GrowthStage
            private set

        fun showStageLabels() {
            selectedLabel = PlantLabel.GrowthStage
        }
    }
}
