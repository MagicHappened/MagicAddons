package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.ToggleRowWidget
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.stepScroll

class ToggleListSettingWidget(
    private val setting: ToggleListSetting,
    overlays: OverlayContext
) : SettingWidget<MutableList<ListEntry>>(setting, overlays) {

    private val searchBox = TextField(0, ROW_HEIGHT, Component.literal(setting.searchLabel)).also {
        it.setMaxLength(64)
        it.isFramed = true
        it.setResponder {
            rowScroll = 0
            rebuildRows()
        }
    }

    private val rows = mutableListOf<ToggleRowWidget<String>>()

    private var matchingChoices: List<String> = emptyList()
    private var rowScroll: Int = 0

    private var rowsBuiltForWidth: Int = -1

    override val bottomPadding: Int = 0

    private fun rowsTop(): Int =
        if (setting.searchable) belowTextTop() + ROW_HEIGHT + Common.UI.SPACING_SMALL else belowTextTop()
    private fun listHeight(): Int = VISIBLE_ROWS * ROW_HEIGHT

    private fun isChoiceEnabled(name: String): Boolean = setting.value.any { it.value == name }

    private fun setChoiceEnabled(name: String, isEnabled: Boolean) {
        if (isEnabled) {
            if (!isChoiceEnabled(name)) setting.value.add(ListEntry(name, name, true))
        } else {
            setting.value.removeAll { it.value == name }
        }
    }

    private fun matchingChoicesInOrder(): List<String> {
        val search = searchBox.value.trim()
        return setting.choices()
            .filter { search.isEmpty() || it.contains(search, ignoreCase = true) }
            .sortedWith(compareByDescending<String> { isChoiceEnabled(it) }.thenBy { it.lowercase() })
    }

    override fun belowTextHeight(): Int =
        if (setting.searchable) ROW_HEIGHT + Common.UI.SPACING_SMALL + listHeight() else listHeight()

    override fun layoutControl() {
        searchBox.x = belowTextLeft()
        searchBox.y = belowTextTop()
        searchBox.width = belowTextWidth()

        if (rowsBuiltForWidth != width) {
            rowsBuiltForWidth = width
            rebuildRows()
        } else {
            placeRows()
        }
    }

    override fun onGroupOpened() {
        rebuildRows()
    }

    private fun placeRows() {
        var currentY = rowsTop()
        rows.forEach { row ->
            row.x = belowTextLeft()
            row.y = currentY
            row.width = belowTextWidth()
            currentY += ROW_HEIGHT
        }
    }

    private fun rebuildRows() {
        matchingChoices = matchingChoicesInOrder()
        rowScroll = rowScroll.coerceIn(0, (matchingChoices.size - VISIBLE_ROWS).coerceAtLeast(0))

        rows.clear()
        var currentY = rowsTop()
        matchingChoices.drop(rowScroll).take(VISIBLE_ROWS).forEach { name ->
            val row = ToggleRowWidget(value = name, isEnabled = { isChoiceEnabled(name) }, onToggle = { setChoiceEnabled(name, it) })
            row.x = belowTextLeft()
            row.y = currentY
            row.width = belowTextWidth()
            row.height = ROW_HEIGHT
            rows.add(row)
            currentY += ROW_HEIGHT
        }
        rows.lastOrNull()?.hasDividerBelow = false
    }

    override fun renderBelowText(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (setting.searchable) searchBox.render(graphics)

        val top = rowsTop()
        graphics.fill(belowTextLeft(), top, belowTextLeft() + belowTextWidth(), top + listHeight(), Common.UI.FIELD_COLOR)
        rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }

        if (rows.isEmpty()) {
            graphics.modText(font, Component.literal("Nothing matches"), belowTextLeft() + Common.UI.TEXT_X_PAD, top + (ROW_HEIGHT - font.lineHeight) / 2, Common.UI.DISABLED_TEXT_COLOR)
        }

        graphics.drawScrollBar(belowTextLeft() + belowTextWidth() - Common.UI.SCROLLBAR_WIDTH - 1, top, listHeight(), matchingChoices.size, VISIBLE_ROWS, rowScroll)

        graphics.fill(belowTextLeft(), top, belowTextLeft() + belowTextWidth(), top + 1, Common.UI.BORDER_COLOR)
        graphics.fill(belowTextLeft(), top, belowTextLeft() + 1, top + listHeight(), Common.UI.BORDER_COLOR)
        graphics.fill(belowTextLeft() + belowTextWidth() - 1, top, belowTextLeft() + belowTextWidth(), top + listHeight(), Common.UI.BORDER_COLOR)
    }

    private fun isOverRows(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in belowTextLeft() until belowTextLeft() + belowTextWidth() && mouseY.toInt() in rowsTop() until rowsTop() + listHeight()

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        if (!isOverRows(mouseX, mouseY) || matchingChoices.size <= VISIBLE_ROWS) return false

        rowScroll = stepScroll(rowScroll, scrollY, matchingChoices.size, VISIBLE_ROWS)
        rebuildRows()
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        isHovered = isHovered && mouseY.toInt() < belowTextTop()
        rows.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (setting.searchable && searchBox.mouseClicked(event, doubled)) return true
        return rows.any { it.mouseClicked(event, doubled) }
    }

    override fun dropFocus() {
        searchBox.isFocused = false
        super.dropFocus()
    }

    override fun charTyped(event: CharacterEvent): Boolean = searchBox.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean = searchBox.keyPressed(event) || super.keyPressed(event)

    private companion object {
        const val ROW_HEIGHT: Int = 16
        const val VISIBLE_ROWS: Int = 5
    }
}
