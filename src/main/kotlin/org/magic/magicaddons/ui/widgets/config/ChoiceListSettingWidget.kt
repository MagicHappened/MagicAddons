package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.ui.screens.FeatureEditScreen
import org.magic.magicaddons.util.compat.McCompat
import org.magic.magicaddons.ui.widgets.ToggleRowWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

/**
 * Every name in the catalogue as a row with a checkbox, a search box above narrowing them, and a
 * window of a few rows the wheel moves through. A name is stored only while it is switched on.
 */
class ChoiceListSettingWidget(
    private val listSetting: ToggleListSetting
) : SettingWidget<MutableList<ListEntry>>(listSetting) {

    override val hasChildren: Boolean = false
    override var childrenExpanded: Boolean = true
    override var hovered: Boolean = false

    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    /** The title strip is this tall for one line of text and grows when the name wraps. */
    private val titleMinHeight = 20
    private var titleHeight = titleMinHeight

    /** A row is at least this tall; a wrapped name makes it taller. */
    private val rowHeight = 20

    /** Room between the widget border and the search box or a row. */
    private val inset = 2

    /** Rows overlap by one pixel so their borders meet as a single line. */
    private val rowOverlap = 1

    /** How many rows show at once, however long the catalogue gets. */
    private val visibleRows = 5

    private val searchBox = EditBox(Minecraft.getInstance().font, 100, rowHeight, Component.literal(""))

    private val rows = mutableListOf<ToggleRowWidget<String>>()

    /** What the search lets through, of which a scrolled window is on screen. */
    private var matching: List<String> = emptyList()
    private var scroll: Int = 0

    private var rowY: Int = 0

    private val title: Component get() = Component.literal(listSetting.displayName)

    /** The rows on screen, top to bottom, overlaps counted. */
    private fun listHeight(): Int = rows.sumOf { it.height - rowOverlap } + rowOverlap


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

    override fun layout() {
        titleHeight = (wrappedHeight(font, title, width - textXPad * 2) + titleMinHeight - font.lineHeight)
            .coerceAtLeast(titleMinHeight)

        searchBox.x = x + inset
        searchBox.y = y + borderSize + titleHeight
        searchBox.width = width - inset * 2
        searchBox.height = rowHeight
        searchBox.setMaxLength(64)
        searchBox.setHint(Component.literal(listSetting.searchLabel))
        searchBox.setResponder {
            scroll = 0
            rebuildRows(relayout = true)
        }

        rowY = searchBox.y + rowHeight + inset

        rebuildRows(relayout = false)
    }

    /**
     * Rebuilds the window of rows and the widget's height to fit them. A search that changed the
     * count asks the screen to move whatever sits below, a layout pass is already doing that.
     */
    private fun rebuildRows(relayout: Boolean) {
        matching = ordered()
        scroll = scroll.coerceIn(0, (matching.size - visibleRows).coerceAtLeast(0))

        rows.clear()

        var currentY = rowY

        matching.drop(scroll).take(visibleRows).forEach { name ->
            val row = ToggleRowWidget(
                value = name,
                isEnabled = { isOn(name) },
                onToggle = { setOn(name, it) }
            )

            row.x = x + inset
            row.y = currentY
            row.width = width - inset * 2
            row.fitHeight(rowHeight)

            rows.add(row)
            currentY += row.height - rowOverlap
        }

        // an empty window still shows the "nothing matches" line, so it keeps one row of height
        val shown = if (rows.isEmpty()) rowHeight else listHeight()
        val newHeight = rowY + shown + inset + borderSize - y

        if (newHeight != height) {
            height = newHeight
            if (relayout) requestRelayout?.invoke()
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(x, y, x + width, y + height, backgroundColor)

        val titleTextHeight = wrappedHeight(font, title, width - textXPad * 2)

        graphics.drawWrappedText(
            font,
            title,
            x + textXPad,
            y + borderSize + (titleHeight - titleTextHeight) / 2,
            width - textXPad * 2,
            Common.UI.TEXT_COLOR
        )

        searchBox.extractRenderState(graphics, mouseX, mouseY, delta)

        rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }

        if (rows.isEmpty()) {
            graphics.text(
                font,
                Component.literal("Nothing matches"),
                x + textXPad,
                rowY + (rowHeight - font.lineHeight) / 2,
                Common.UI.DISABLED_TEXT_COLOR,
                false
            )
        }

        renderScrollBar(graphics)
        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)
    }

    /** A sliver on the right edge saying how deep the list goes and where the window sits in it. */
    private fun renderScrollBar(graphics: GuiGraphicsExtractor) {
        if (matching.size <= visibleRows) return

        val listHeight = listHeight()
        val barX = x + width - inset - 2
        val barHeight = (listHeight * visibleRows / matching.size).coerceAtLeast(6)
        val travel = listHeight - barHeight
        val barY = rowY + travel * scroll / (matching.size - visibleRows)

        graphics.fill(barX, rowY, barX + 2, rowY + listHeight, 0x40000000)
        graphics.fill(barX, barY, barX + 2, barY + barHeight, Common.UI.TEXT_COLOR)
    }

    private fun overRows(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x..(x + width) && mouseY.toInt() in rowY..(rowY + listHeight())

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!overRows(mouseX, mouseY) || matching.size <= visibleRows) return false

        scroll = (scroll - scrollY.toInt().coerceIn(-1, 1)).coerceIn(0, matching.size - visibleRows)
        rebuildRows(relayout = false)
        return true
    }

    /**
     * Only the title counts as hovering this setting: a tooltip over the rows would cover the very
     * checkmarks the mouse is there to press.
     */
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = mouseX.toInt() in x..(x + width) &&
                mouseY.toInt() in y..(y + borderSize + titleHeight)

        if (hovered) {
            (McCompat.currentScreen() as? FeatureEditScreen)?.hoveredWidget = this
        }

        rows.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (searchBox.mouseClicked(mouseButtonEvent, doubled)) {
            searchBox.isFocused = true
            return true
        }
        searchBox.isFocused = false

        rows.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }

        return false
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean =
        searchBox.isFocused && searchBox.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean =
        searchBox.isFocused && searchBox.keyPressed(keyEvent)

    override fun getTotalHeight(): Int = height
}
