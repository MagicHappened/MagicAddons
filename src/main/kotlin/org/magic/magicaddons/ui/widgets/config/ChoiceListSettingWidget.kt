package org.magic.magicaddons.ui.widgets.config

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
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar

/**
 * Every name in the catalogue as a row with a checkbox, under a search box narrowing them, in a
 * window of a few rows the wheel moves through. A name is stored only while it is switched on.
 */
class ChoiceListSettingWidget(
    private val listSetting: ToggleListSetting,
    overlays: OverlayContext
) : SettingWidget<MutableList<ListEntry>>(listSetting, overlays) {

    override val controlWidth: Int = 0
    override val controlHeight: Int = 0

    private val searchBox = TextField(0, ROW_HEIGHT, Component.literal(listSetting.searchLabel)).also {
        it.setMaxLength(64)
        it.setResponder {
            scroll = 0
            rebuildRows()
        }
    }

    private val rows = mutableListOf<ToggleRowWidget<String>>()

    /** What the search lets through, of which a scrolled window is on screen. */
    private var matching: List<String> = emptyList()
    private var scroll: Int = 0

    private fun rowsTop(): Int = extraTop() + ROW_HEIGHT + Common.UI.SPACING_SMALL
    private fun listHeight(): Int = VISIBLE_ROWS * ROW_HEIGHT

    private fun isOn(name: String): Boolean = listSetting.value.any { it.value == name }

    /**
     * Flips a name without moving its row: a row that jumped away the moment it was ticked would
     * vanish from under the mouse. The order catches up on the next scroll, search or reopen.
     */
    private fun setOn(name: String, on: Boolean) {
        if (on) {
            if (!isOn(name)) listSetting.value.add(ListEntry(name, name, true))
        } else {
            listSetting.value.removeAll { it.value == name }
        }
    }

    /** Switched-on names first, each group alphabetical, narrowed to what the search contains. */
    private fun ordered(): List<String> {
        val search = searchBox.value.trim()
        return listSetting.choices()
            .filter { search.isEmpty() || it.contains(search, ignoreCase = true) }
            .sortedWith(compareByDescending<String> { isOn(it) }.thenBy { it.lowercase() })
    }

    override fun extraHeight(): Int = ROW_HEIGHT + Common.UI.SPACING_SMALL + listHeight()

    override fun layoutControl() {
        searchBox.x = extraLeft()
        searchBox.y = extraTop()
        searchBox.width = extraWidth()
        rebuildRows()
    }

    /** The window of rows the scroll is looking at, laid out under the search box. */
    private fun rebuildRows() {
        matching = ordered()
        scroll = scroll.coerceIn(0, (matching.size - VISIBLE_ROWS).coerceAtLeast(0))

        rows.clear()
        var currentY = rowsTop()
        matching.drop(scroll).take(VISIBLE_ROWS).forEach { name ->
            val row = ToggleRowWidget(value = name, isEnabled = { isOn(name) }, onToggle = { setOn(name, it) })
            row.x = extraLeft()
            row.y = currentY
            row.width = extraWidth()
            row.height = ROW_HEIGHT
            rows.add(row)
            currentY += ROW_HEIGHT
        }
        rows.lastOrNull()?.dividerBelow = false
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun renderExtra(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        searchBox.render(graphics)

        val top = rowsTop()
        graphics.fill(extraLeft(), top, extraLeft() + extraWidth(), top + listHeight(), Common.UI.FIELD_COLOR)
        rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }

        if (rows.isEmpty()) {
            graphics.text(font, Component.literal("Nothing matches"), extraLeft() + Common.UI.TEXT_X_PAD, top + (ROW_HEIGHT - font.lineHeight) / 2, Common.UI.DISABLED_TEXT_COLOR, false)
        }

        graphics.drawScrollBar(extraLeft() + extraWidth() - Common.UI.SCROLLBAR_WIDTH - 1, top, listHeight(), matching.size, VISIBLE_ROWS, scroll)
        graphics.drawBorder(extraLeft(), top, extraLeft() + extraWidth(), top + listHeight(), 1, Common.UI.THIN_DIVIDER_COLOR)
    }

    private fun overRows(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in extraLeft()..(extraLeft() + extraWidth()) && mouseY.toInt() in rowsTop()..(rowsTop() + listHeight())

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!overRows(mouseX, mouseY) || matching.size <= VISIBLE_ROWS) return false

        scroll = (scroll - scrollY.toInt().coerceIn(-1, 1)).coerceIn(0, matching.size - VISIBLE_ROWS)
        rebuildRows()
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        // only the text counts as hovering this setting, a wash over the rows would cover the marks
        hovered = isMouseOver(mouseX, mouseY) && mouseY.toInt() < extraTop()
        rows.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (searchBox.mouseClicked(event, doubled)) return true
        return rows.any { it.mouseClicked(event, doubled) }
    }

    override fun dropFocus() {
        searchBox.focused = false
    }

    override fun charTyped(event: CharacterEvent): Boolean = searchBox.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean = searchBox.keyPressed(event)

    private companion object {
        const val ROW_HEIGHT: Int = 16

        /** How many rows show at once, however long the catalogue gets. */
        const val VISIBLE_ROWS: Int = 5
    }
}
