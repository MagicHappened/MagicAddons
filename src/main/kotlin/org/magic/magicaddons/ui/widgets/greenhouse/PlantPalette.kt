package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem
import org.magic.magicaddons.util.ScreenUtil.drawShelf
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip

/**
 * Every plant as an icon on a shelf, searched from the top, dragged from here onto a preset's grid.
 * Also holds the Clear all button and the Delete switch, which the owning screen acts on.
 */
class PlantPalette(
    private val onClearAll: (MouseButtonEvent) -> Unit
) {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    private val font = Minecraft.getInstance().font

    private val search = TextField(0, ROW, Component.literal(SEARCH_HINT)).apply {
        setResponder { scroll = 0 }
    }

    private val clearButton = ClickableButtonWidget(CLEAR_WIDTH, ROW, Component.literal("Clear all"))
    private val deleteButton = ClickableButtonWidget(DELETE_WIDTH, ROW, Component.literal("Delete"))

    /** While on, clicking a plant on the grid takes it off the preset. */
    var deleteMode: Boolean = false
        private set

    var hovered: CropDefinition? = null
        private set

    /** The plant being carried, and where the mouse has it. */
    var dragging: CropDefinition? = null
        private set
    private var dragX = 0
    private var dragY = 0

    private var scroll = 0
    private var columns = 1
    private var visibleRows = 1
    private var cell = MIN_CELL

    /** Plants a player can place, by rarity then name. Roots and fire have no seed to place. */
    private val crops: List<CropDefinition> = CropRegistry.all
        .filter { it.skyblockId != null }
        .sortedWith(compareBy({ CropRegistry.tierOf[it] ?: 7 }, { it.name.lowercase() }))

    private fun shown(): List<CropDefinition> {
        val typed = search.value.trim()
        return crops.filter { it.name.contains(typed, ignoreCase = true) }
    }

    private fun titleHeight(): Int = font.lineHeight + Common.UI.SPACING * 2
    private fun gridTop(): Int = y + titleHeight() + ROW + Common.UI.SPACING + ROW + Common.UI.SPACING
    private fun gridLeft(): Int = x + ActionPanel.PADDING

    /** The frame around an icon, in the colour of the plant's rarity. Base crops sit with the common ones. */
    private fun rarityColour(def: CropDefinition): Int = when (CropRegistry.tierOf[def] ?: 7) {
        0, 1 -> RARITY_COMMON
        2 -> RARITY_UNCOMMON
        3, 6 -> RARITY_RARE
        4 -> RARITY_EPIC
        5 -> RARITY_LEGENDARY
        else -> RARITY_COMMON
    }

    fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        search.x = x + ActionPanel.PADDING
        search.y = y + titleHeight()
        search.width = width - ActionPanel.PADDING * 2

        clearButton.x = search.x
        clearButton.y = search.y + ROW + Common.UI.SPACING
        deleteButton.x = clearButton.x + clearButton.width + Common.UI.SPACING
        deleteButton.y = clearButton.y

        // four to six across, each as big as that leaves; the rows are whatever fits under
        val inner = width - ActionPanel.PADDING * 2
        columns = (inner / MIN_CELL).coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        cell = (inner / columns).coerceAtLeast(MIN_CELL)
        visibleRows = ((y + height - ActionPanel.PADDING - gridTop()) / cell).coerceAtLeast(1)
    }

    private fun totalRows(): Int = (shown().size + columns - 1) / columns

    /** The plant whose cell is under the mouse, when the mouse is on the grid. */
    private fun cropAt(mouseX: Double, mouseY: Double): CropDefinition? {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        if (mx < gridLeft() || my < gridTop() || my >= gridTop() + visibleRows * cell) return null

        val column = (mx - gridLeft()) / cell
        val row = (my - gridTop()) / cell + scroll
        if (column >= columns) return null

        return shown().getOrNull(row * columns + column)
    }

    fun stackFor(def: CropDefinition): ItemStack =
        def.displayItem?.let { ItemStack(it) }
            ?: def.skyblockId?.toItem()?.takeUnless { it.isEmpty }
            ?: ItemStack(Items.BARRIER)

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawShelf(x, y, x + width, y + height, TITLE)

        search.render(graphics)
        deleteButton.pressed = deleteMode
        clearButton.extractRenderState(graphics, mouseX, mouseY, delta)
        deleteButton.extractRenderState(graphics, mouseX, mouseY, delta)

        val list = shown()
        val rows = totalRows()
        scroll = scroll.coerceIn(0, (rows - visibleRows).coerceAtLeast(0))

        list.drop(scroll * columns).take(visibleRows * columns).forEachIndexed { index, def ->
            val cellX = gridLeft() + index % columns * cell
            val cellY = gridTop() + index / columns * cell

            graphics.fill(cellX + 1, cellY + 1, cellX + cell - 1, cellY + cell - 1, Common.UI.FIELD_COLOR)
            if (def == hovered) graphics.fill(cellX + 1, cellY + 1, cellX + cell - 1, cellY + cell - 1, Common.UI.HOVER_WASH)
            graphics.drawBorder(cellX + 1, cellY + 1, cellX + cell - 1, cellY + cell - 1, 1, rarityColour(def))
            graphics.renderFakeItem(stackFor(def), cellX + ICON_PAD, cellY + ICON_PAD, cell - ICON_PAD * 2, cell - ICON_PAD * 2)
        }

        if (rows > visibleRows) {
            graphics.drawScrollBar(
                x + width - ActionPanel.PADDING - Common.UI.SCROLLBAR_WIDTH,
                gridTop(),
                visibleRows * cell,
                rows,
                visibleRows,
                scroll
            )
        }
    }

    /** The carried plant under the mouse, seen through, so the slot it is over stays visible. */
    fun renderDrag(graphics: GuiGraphicsExtractor) {
        val def = dragging ?: return
        val left = dragX - cell / 2
        val top = dragY - cell / 2

        graphics.renderFakeItem(stackFor(def), left, top, cell, cell)
        graphics.fill(left, top, left + cell, top + cell, DRAG_VEIL)
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (dragging != null) return
        val def = hovered ?: return
        graphics.drawSimpleTooltip(def.name, mouseX + 7, mouseY + 12)
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = cropAt(mouseX, mouseY)
        clearButton.mouseMoved(mouseX, mouseY)
        deleteButton.mouseMoved(mouseX, mouseY)
    }

    fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (search.mouseClicked(event, doubled)) return true
        if (!isMouseOver(event.x, event.y)) return false

        if (clearButton.mouseClicked(event, doubled)) {
            onClearAll(event)
            return true
        }
        if (deleteButton.mouseClicked(event, doubled)) {
            deleteMode = !deleteMode
            return true
        }

        cropAt(event.x, event.y)?.let {
            dragging = it
            dragX = event.x.toInt()
            dragY = event.y.toInt()
        }
        return true
    }

    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        if (dragging == null) return false
        dragX = mouseX.toInt()
        dragY = mouseY.toInt()
        return true
    }

    /** Lets go of the carried plant and says which it was, for the owner to place. */
    fun mouseReleased(): CropDefinition? {
        val def = dragging
        dragging = null
        return def
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, scrollY: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        scroll = (scroll - scrollY.toInt().coerceIn(-1, 1)).coerceIn(0, (totalRows() - visibleRows).coerceAtLeast(0))
        return true
    }

    fun charTyped(event: CharacterEvent): Boolean = search.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = search.keyPressed(event)

    companion object {
        const val TITLE: String = "Plants"
        private const val SEARCH_HINT: String = "Search…"

        private const val ROW: Int = 20
        private const val MIN_CELL: Int = 24
        private const val MIN_COLUMNS: Int = 4
        private const val MAX_COLUMNS: Int = 6
        private const val ICON_PAD: Int = 4

        /** The game's rarity colours, on the frame around each icon. */
        private const val RARITY_COMMON: Int = 0xFFAAAAAA.toInt()
        private const val RARITY_UNCOMMON: Int = 0xFF55FF55.toInt()
        private const val RARITY_RARE: Int = 0xFF5555FF.toInt()
        private const val RARITY_EPIC: Int = 0xFFAA00AA.toInt()
        private const val RARITY_LEGENDARY: Int = 0xFFFFAA00.toInt()
        private const val CLEAR_WIDTH: Int = 60
        private const val DELETE_WIDTH: Int = 50

        /** Laid over the carried plant so it reads as not yet placed. */
        private const val DRAG_VEIL: Int = 0x70101010
    }
}
