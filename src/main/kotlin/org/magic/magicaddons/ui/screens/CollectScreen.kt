package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.CropCollector
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawPanel

// collector screen, currently only for collecting crops that dont have data, eventually, will not have a use.
class CollectScreen : MagicAddonsScreen(Component.literal("Crop Collection"), "the collector screen") {

    private companion object {
        const val ROW_HEIGHT: Int = 13
        const val CHECKBOX_SIZE: Int = 9
        const val PADDING: Int = 4
        const val SCREEN_EDGE_GAP: Int = 6

        const val BUTTON_HEIGHT: Int = ROW_HEIGHT + 4

        const val MIN_PANEL_WIDTH: Int = 120
        const val MAX_PANEL_WIDTH: Int = 260

        var savedScroll: Int = 0
    }

    private val rowCheckbox = CheckboxWidget()

    private var panelX: Int = 0
    private var panelY: Int = 0
    private var panelWidth: Int = 0
    private var panelHeight: Int = 0
    private var listTop: Int = 0
    private var visibleRows: Int = 0
    private var isListOverflowing: Boolean = false

    private var writeButtonY: Int = 0
    private var dismissButtonY: Int = 0

    private fun layoutPanel(rows: List<CropCollector.ChecklistRow>) {
        val widestLabelWidth = rows.maxOfOrNull { font.width(it.label) } ?: 0

        panelWidth = (CHECKBOX_SIZE + PADDING * 3 + widestLabelWidth).coerceIn(MIN_PANEL_WIDTH, MAX_PANEL_WIDTH)
        panelX = width - panelWidth - SCREEN_EDGE_GAP

        val buttonSpace = BUTTON_HEIGHT * 2 + Common.UI.SPACING + PADDING * 2
        val headerSpace = ROW_HEIGHT + PADDING

        val rowsThatFit = (height - SCREEN_EDGE_GAP * 2 - buttonSpace - headerSpace - PADDING * 2) / ROW_HEIGHT
        isListOverflowing = rows.size > rowsThatFit
        val listRows = rows.size.coerceAtMost(rowsThatFit).coerceAtLeast(0)
        visibleRows = if (isListOverflowing) (listRows - 1).coerceAtLeast(0) else listRows

        panelHeight = headerSpace + listRows * ROW_HEIGHT + buttonSpace + PADDING * 2
        panelY = (height - panelHeight) / 2
        listTop = panelY + PADDING + headerSpace

        writeButtonY = listTop + listRows * ROW_HEIGHT + PADDING
        dismissButtonY = writeButtonY + BUTTON_HEIGHT + Common.UI.SPACING
    }

    private fun maxScroll(rowCount: Int): Int = (rowCount - visibleRows).coerceAtLeast(0)

    private fun stepScroll(steps: Int) {
        savedScroll = (savedScroll + steps).coerceIn(0, maxScroll(CropCollector.rows().size))
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val rows = CropCollector.rows()
        layoutPanel(rows)

        savedScroll = savedScroll.coerceIn(0, maxScroll(rows.size))

        graphics.drawPanel(panelX, panelY, panelX + panelWidth, panelY + panelHeight)

        graphics.text(
            font,
            Component.literal("Collect — G closes"),
            panelX + PADDING,
            panelY + PADDING,
            Common.UI.ACCENT_COLOR,
            false
        )

        val textX = panelX + PADDING + CHECKBOX_SIZE + PADDING

        rows.drop(savedScroll).take(visibleRows).forEachIndexed { rowIndex, row ->
            val rowY = listTop + rowIndex * ROW_HEIGHT

            if (row.isCollectable && mouseX in panelX until panelX + panelWidth && mouseY in rowY until rowY + ROW_HEIGHT) {
                graphics.fill(panelX + Common.UI.BORDER_SIZE, rowY, panelX + panelWidth - Common.UI.BORDER_SIZE, rowY + ROW_HEIGHT, Common.UI.HOVER_WASH)
            }

            if (row.isCollectable) {
                rowCheckbox.x = panelX + PADDING
                rowCheckbox.y = rowY + (ROW_HEIGHT - CHECKBOX_SIZE) / 2
                rowCheckbox.size = CHECKBOX_SIZE
                rowCheckbox.isChecked = row.confirmed
                rowCheckbox.render(graphics)
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

        if (isListOverflowing) {
            graphics.text(
                font,
                Component.literal("… ${savedScroll + visibleRows}/${rows.size}"),
                panelX + PADDING,
                listTop + visibleRows * ROW_HEIGHT + (ROW_HEIGHT - font.lineHeight) / 2,
                Common.UI.TEXT_DIM_COLOR,
                false
            )
        }

        drawActionButton(graphics, writeButtonY, "Write the file", Common.UI.SUCCESS_COLOR, mouseX, mouseY)
        drawActionButton(graphics, dismissButtonY, "Dismiss without writing", Common.UI.DANGER_COLOR, mouseX, mouseY)
    }

    private fun drawActionButton(graphics: GuiGraphicsExtractor, y: Int, label: String, color: Int, mouseX: Int, mouseY: Int) {
        val left = panelX + PADDING
        val right = panelX + panelWidth - PADDING
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

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()

        if (x !in panelX until panelX + panelWidth) {
            return super.onMouseClicked(event, doubled)
        }

        if (y in writeButtonY until writeButtonY + BUTTON_HEIGHT) {
            CropCollector.finish()
            onClose()
            return true
        }

        if (y in dismissButtonY until dismissButtonY + BUTTON_HEIGHT) {
            CropCollector.quit()
            onClose()
            return true
        }

        if (y in listTop until listTop + visibleRows * ROW_HEIGHT) {
            val row = CropCollector.rows().getOrNull(savedScroll + (y - listTop) / ROW_HEIGHT)

            if (row != null && row.isCollectable) {
                CropCollector.toggleEntry(row.id)
            }
            return true
        }

        return super.onMouseClicked(event, doubled)
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        stepScroll(-scrollY.toInt())
        return true
    }

    override fun onKeyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_G) {
            onClose()
            return true
        }
        return super.onKeyPressed(event)
    }

    override fun onExtractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    override fun isPauseScreen(): Boolean = false
}
