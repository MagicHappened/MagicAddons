package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.screens.Screen
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.CropCollector
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawPanel

/**
 * The collector's checklist, docked right so the garden stays visible behind it. Opened with G
 * while a run is live, closed with G or escape.
 */
/** Where the checklist was scrolled to, kept outside the screen so reopening lands back there. */
private var scroll: Int = 0

class CollectScreen : Screen(Component.literal("Crop Collection")) {

    private companion object {
        const val ROW_HEIGHT: Int = 13
        const val CHECKBOX: Int = 9
        const val PAD: Int = 4
        const val EDGE_GAP: Int = 6

        /** The two verdict buttons stand a little taller than a list row. */
        const val BUTTON_HEIGHT: Int = ROW_HEIGHT + 4

        const val MAX_PANEL_WIDTH: Int = 260
    }

    private val checkbox = CheckboxWidget()

    private var panelX: Int = 0
    private var panelY: Int = 0
    private var panelWidth: Int = 0
    private var panelHeight: Int = 0
    private var listTop: Int = 0
    private var visibleRows: Int = 0

    /** The two verdict buttons, laid out as pseudo-rows under the list. */
    private var finishY: Int = 0
    private var quitY: Int = 0

    private fun layout(rows: List<CropCollector.Row>) {
        val widest = rows.maxOfOrNull { font.width(it.label) } ?: 0

        panelWidth = (CHECKBOX + PAD * 3 + widest).coerceIn(120, MAX_PANEL_WIDTH)
        panelX = width - panelWidth - EDGE_GAP

        val buttonSpace = BUTTON_HEIGHT * 2 + Common.UI.SPACING + PAD * 2
        val headerSpace = ROW_HEIGHT + PAD

        visibleRows = ((height - EDGE_GAP * 2 - buttonSpace - headerSpace - PAD * 2) / ROW_HEIGHT)
            .coerceAtMost(rows.size)

        panelHeight = headerSpace + visibleRows * ROW_HEIGHT + buttonSpace + PAD * 2
        panelY = (height - panelHeight) / 2
        listTop = panelY + PAD + headerSpace

        finishY = listTop + visibleRows * ROW_HEIGHT + PAD
        quitY = finishY + BUTTON_HEIGHT + Common.UI.SPACING
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val rows = CropCollector.rows()
        layout(rows)

        scroll = scroll.coerceIn(0, (rows.size - visibleRows).coerceAtLeast(0))

        graphics.drawPanel(panelX, panelY, panelX + panelWidth, panelY + panelHeight)

        graphics.text(
            font,
            Component.literal("Collect — G closes"),
            panelX + PAD,
            panelY + PAD,
            Common.UI.ACCENT_COLOR,
            false
        )

        rows.drop(scroll).take(visibleRows).forEachIndexed { i, row ->
            val rowY = listTop + i * ROW_HEIGHT
            val textX: Int

            // a row that can be ticked lights up under the mouse, like any other row on the kit
            if (row.collectable && mouseX in panelX until panelX + panelWidth && mouseY in rowY until rowY + ROW_HEIGHT) {
                graphics.fill(panelX + Common.UI.BORDER_SIZE, rowY, panelX + panelWidth - Common.UI.BORDER_SIZE, rowY + ROW_HEIGHT, Common.UI.HOVER_WASH)
            }

            if (row.collectable) {
                checkbox.x = panelX + PAD
                checkbox.y = rowY + (ROW_HEIGHT - CHECKBOX) / 2
                checkbox.size = CHECKBOX
                checkbox.checked = row.confirmed
                checkbox.render(graphics)
                textX = panelX + PAD + CHECKBOX + PAD
            } else {
                // nothing to tick: a plant with no definition is reported, never collected
                textX = panelX + PAD + CHECKBOX + PAD
            }

            graphics.text(
                font,
                Component.literal(row.label),
                textX,
                rowY + (ROW_HEIGHT - font.lineHeight) / 2,
                row.color,
                false
            )
        }

        if (rows.size > visibleRows) {
            graphics.text(
                font,
                Component.literal("… ${scroll + visibleRows}/${rows.size}"),
                panelX + PAD,
                finishY - ROW_HEIGHT - PAD,
                Common.UI.TEXT_DIM_COLOR,
                false
            )
        }

        button(graphics, finishY, "Write the file", Common.UI.SUCCESS_COLOR, mouseX, mouseY)
        button(graphics, quitY, "Dismiss without writing", Common.UI.DANGER_COLOR, mouseX, mouseY)
    }

    private fun button(graphics: GuiGraphicsExtractor, y: Int, label: String, color: Int, mouseX: Int, mouseY: Int) {
        val left = panelX + PAD
        val right = panelX + panelWidth - PAD
        val hovered = mouseX in left until right && mouseY in y until y + BUTTON_HEIGHT

        graphics.drawButtonPanel(left, y, right, y + BUTTON_HEIGHT, hovered)
        graphics.text(
            font,
            Component.literal(label),
            left + (right - left - font.width(label)) / 2,
            y + (BUTTON_HEIGHT - font.lineHeight) / 2,
            color,
            false
        )
    }

    /** The whole row is the target: at this size the checkbox alone would be a test of aim. */
    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val x = mouseButtonEvent.x.toInt()
        val y = mouseButtonEvent.y.toInt()

        if (x !in panelX until panelX + panelWidth) {
            return super.mouseClicked(mouseButtonEvent, doubled)
        }

        if (y in finishY until finishY + BUTTON_HEIGHT) {
            CropCollector.finish()
            onClose()
            return true
        }

        if (y in quitY until quitY + BUTTON_HEIGHT) {
            CropCollector.quit()
            onClose()
            return true
        }

        if (y in listTop until listTop + visibleRows * ROW_HEIGHT) {
            val row = CropCollector.rows().getOrNull(scroll + (y - listTop) / ROW_HEIGHT)

            if (row != null && row.collectable) {
                CropCollector.toggle(row.id, announce = false)
            }
            return true
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        scroll -= scrollY.toInt()
        return true
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        // the key that opened it closes it, so reviewing is one hand on one key
        if (keyEvent.key() == GLFW.GLFW_KEY_G) {
            onClose()
            return true
        }
        return super.keyPressed(keyEvent)
    }

    /** No blur, no dim, no panorama: the garden behind the list is what the list is about. */
    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) = Unit

    override fun isPauseScreen(): Boolean = false
}
