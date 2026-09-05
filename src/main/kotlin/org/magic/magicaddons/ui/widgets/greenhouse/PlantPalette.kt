package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.ui.OverlayContext
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Every plant as an icon on a shelf, searched from the top, dragged from here onto a preset's grid.
 * Also holds the Clear all button and the Delete switch, which the owning screen acts on.
 */
class PlantPalette(
    overlayContext: OverlayContext,
    private val onClearAll: (MouseButtonEvent) -> Unit,
    private val onUndo: () -> Unit,
    private val onRedo: () -> Unit
) {

    /** What a click on a plant does while the selector is on something other than Off. */
    enum class MarkChoice(private val label: String, val marking: LayoutSlot.Marking?, val applies: Boolean) {
        Off("Mark off", null, false),
        Target("Target", LayoutSlot.Marking.Target, true),
        Ingredient("Ingredient", LayoutSlot.Marking.Ingredient, true),
        Unique("Unique crop", LayoutSlot.Marking.UniqueCrop, true),
        Clear("Clear mark", null, true);

        override fun toString(): String = label
    }

    private val markSelector = EnumWidget(
        values = MarkChoice.entries,
        currentValue = MarkChoice.Off,
        overlayContext = overlayContext,
        searchable = false
    )

    val markChoice: MarkChoice get() = markSelector.currentValue ?: MarkChoice.Off

    private val undoButton = ClickableButtonWidget(ARROW_WIDTH, ROW, Component.literal("←"))
    private val redoButton = ClickableButtonWidget(ARROW_WIDTH, ROW, Component.literal("→"))
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    private val font = Minecraft.getInstance().font

    private val search = TextField(0, ROW, Component.literal(SEARCH_HINT)).apply {
        setResponder { scroll = 0 }
    }

    private val clearButton = ClickableButtonWidget(buttonWidth("Clear all"), ROW, Component.literal("Clear all"))
    private val deleteButton = ClickableButtonWidget(buttonWidth("Delete"), ROW, Component.literal("Delete"))

    /** A button as wide as its word and the usual padding, so a row of them wastes nothing. */
    private fun buttonWidth(label: String): Int = font.width(label) + (Common.UI.TEXT_X_PAD + Common.UI.BORDER_SIZE) * 2

    /** While on, clicking a plant on the grid takes it off the preset. */
    var deleteMode: Boolean = false
        private set

    var hovered: CropDefinition? = null
        private set

    /** Where the mouse last was, so a scroll can work out what is under it now. */
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    /** The plant being dragged, and where the mouse has it. */
    var dragging: CropDefinition? = null
        private set
    private var dragX = 0
    private var dragY = 0

    /** The plant picked with a plain click, placed by the next click on a slot. */
    var selected: CropDefinition? = null
        private set

    /** The plant under the mouse button since it went down, until it moves enough to be a drag. */
    private var pressed: CropDefinition? = null
    private var pressX = 0.0
    private var pressY = 0.0

    /** Whatever is on the way to the grid, dragged or picked. */
    val carried: CropDefinition? get() = dragging ?: selected

    private var scroll = 0
    private var columns = 1
    private var visibleRows = 1

    /** A cell's width and height: the width shares the shelf, the height shares the room under the buttons. */
    private var cellWidth = MIN_CELL
    private var cellHeight = MIN_CELL

    /** Plants a player can place, by rarity then name, the dead plant right after the base crops. */
    private val crops: List<CropDefinition> = CropRegistry.all
        .filter { it.skyblockId != null }
        .sortedWith(compareBy({ sortTier(it) }, { it.name.lowercase() }))

    private fun sortTier(def: CropDefinition): Double =
        if (def.name == DEAD_PLANT) 0.5 else (CropRegistry.tierOf[def] ?: 7).toDouble()

    /** Plain text finds names; an @ in front finds effects, a # in front finds the soil a crop grows on. */
    private fun shown(): List<CropDefinition> {
        val typed = search.value.trim()
        return when {
            typed.startsWith("@") -> {
                val term = typed.drop(1).trim()
                crops.filter { def -> def.effects.any { it.label.contains(term, ignoreCase = true) } }
            }
            typed.startsWith("#") -> {
                val term = typed.drop(1).trim()
                crops.filter { def -> def.requiredSoil.any { it.name.string.contains(term, ignoreCase = true) } }
            }
            else -> crops.filter { it.name.contains(typed, ignoreCase = true) }
        }
    }

    /** Whether the mouse is on the little i in the shelf's corner. */
    private var infoHovered = false

    private fun infoCentre(): Pair<Int, Int> = (x + width - EDGE_PAD - INFO_RADIUS - 1) to (y + titleHeight() / 2)

    /** A ring with an i in it, at the shelf's top right, so the two search prefixes can be found. */
    private fun renderInfo(graphics: GuiGraphicsExtractor) {
        val (cx, cy) = infoCentre()
        for (dy in -INFO_RADIUS..INFO_RADIUS) {
            val half = kotlin.math.sqrt((INFO_RADIUS * INFO_RADIUS - dy * dy).toDouble()).toInt()
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, if (infoHovered) Common.UI.ACCENT_COLOR else Common.UI.BORDER_COLOR)
        }
        val inner = INFO_RADIUS - 1
        for (dy in -inner..inner) {
            val half = kotlin.math.sqrt((inner * inner - dy * dy).toDouble()).toInt()
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, Common.UI.BACKGROUND_COLOR)
        }
        graphics.text(font, Component.literal("i"), cx - font.width("i") / 2 + 1, cy - font.lineHeight / 2 + 1, Common.UI.TEXT_COLOR, false)
    }

    private fun isOverInfo(mouseX: Double, mouseY: Double): Boolean {
        val (cx, cy) = infoCentre()
        return mouseX.toInt() in cx - INFO_RADIUS..cx + INFO_RADIUS && mouseY.toInt() in cy - INFO_RADIUS..cy + INFO_RADIUS
    }

    private fun titleHeight(): Int = font.lineHeight + Common.UI.SPACING * 2
    /** How many rows the buttons take under the search: one, or two when the shelf is too narrow. */
    private var buttonRows: Int = 1

    /** Under the title come the search and the button rows, each a row and a gap. */
    private fun gridTop(): Int = y + titleHeight() + (ROW + Common.UI.SPACING) * (1 + buttonRows)
    /** The cells start where the search and the buttons start. */
    private fun gridLeft(): Int = x + EDGE_PAD

    /** The icon inside a cell: a whole multiple of sixteen, so its pixels land square. */
    private fun iconSize(): Int = ((minOf(cellWidth, cellHeight) - ICON_PAD * 2) / 16 * 16).coerceAtLeast(16)

    /** The ground under an icon, in the colour of the plant's rarity, or its own for base and rare crops. */
    private fun rarityColour(def: CropDefinition): Int = when (CropRegistry.tierOf[def] ?: 7) {
        1 -> RARITY_COMMON
        2 -> RARITY_UNCOMMON
        3 -> RARITY_RARE
        4 -> RARITY_EPIC
        5 -> RARITY_LEGENDARY
        6 -> RARE_CROP
        // the base crops, and the dead plant that players know from among them
        else -> BASE_CROP
    }

    fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        search.x = x + EDGE_PAD
        search.y = y + titleHeight()
        search.width = width - EDGE_PAD * 2

        clearButton.x = search.x
        clearButton.y = search.y + ROW + Common.UI.SPACING
        deleteButton.x = clearButton.x + clearButton.width + Common.UI.SPACING
        deleteButton.y = clearButton.y

        // the mark selector, as wide as its longest word, and the arrows follow Delete on the same
        // row when they fit, else take the next row
        val afterDelete = deleteButton.x + deleteButton.width + Common.UI.SPACING
        val arrows = ARROW_WIDTH * 2 + Common.UI.SPACING * 2
        val right = x + width - EDGE_PAD
        markSelector.fitToValues(right - search.x - arrows)
        val fitsBeside = afterDelete + markSelector.width + arrows <= right
        buttonRows = if (fitsBeside) 1 else 2

        markSelector.x = if (fitsBeside) afterDelete else search.x
        markSelector.y = if (fitsBeside) clearButton.y else clearButton.y + ROW + Common.UI.SPACING
        markSelector.height = ROW
        undoButton.x = markSelector.x + markSelector.width + Common.UI.SPACING
        undoButton.y = markSelector.y
        redoButton.x = undoButton.x + ARROW_WIDTH + Common.UI.SPACING
        redoButton.y = markSelector.y

        // the cells fill the room under the buttons exactly: as many rows of about the usual size
        // as fit, each row then stretched to use the whole height, within sane bounds
        val inner = width - EDGE_PAD * 2
        val room = (y + height - EDGE_PAD - gridTop()).coerceAtLeast(MIN_CELL)
        visibleRows = (room.toFloat() / TARGET_CELL).roundToInt().coerceAtLeast(1)
        cellHeight = (room / visibleRows).coerceIn(MIN_CELL, MAX_CELL)
        columns = (inner / cellHeight).coerceAtLeast(1)
        cellWidth = inner / columns
    }

    private fun totalRows(): Int = (shown().size + columns - 1) / columns

    /** The plant whose cell is under the mouse, when the mouse is on the grid. */
    private fun cropAt(mouseX: Double, mouseY: Double): CropDefinition? {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        if (mx < gridLeft() || my < gridTop() || my >= gridTop() + visibleRows * cellHeight) return null

        val column = (mx - gridLeft()) / cellWidth
        val row = (my - gridTop()) / cellHeight + scroll
        if (column >= columns) return null

        return shown().getOrNull(row * columns + column)
    }

    fun stackFor(def: CropDefinition): ItemStack =
        def.displayItem?.let { ItemStack(it) }
            ?: def.skyblockId?.toItem()?.takeUnless { it.isEmpty }
            ?: ItemStack(Items.BARRIER)

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawShelf(x, y, x + width, y + height, TITLE)
        renderInfo(graphics)

        search.render(graphics)
        deleteButton.pressed = deleteMode
        clearButton.extractRenderState(graphics, mouseX, mouseY, delta)
        deleteButton.extractRenderState(graphics, mouseX, mouseY, delta)

        // the box wears the colour of the mark it would give, and red while set to clear marks
        markSelector.frameColor = when (markChoice) {
            MarkChoice.Clear -> Common.UI.DANGER_COLOR
            else -> markChoice.marking?.color
        }
        markSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        undoButton.extractRenderState(graphics, mouseX, mouseY, delta)
        redoButton.extractRenderState(graphics, mouseX, mouseY, delta)

        val list = shown()
        val rows = totalRows()
        scroll = scroll.coerceIn(0, (rows - visibleRows).coerceAtLeast(0))

        list.drop(scroll * columns).take(visibleRows * columns).forEachIndexed { index, def ->
            val cellX = gridLeft() + index % columns * cellWidth
            val cellY = gridTop() + index / columns * cellHeight
            val right = cellX + cellWidth - 1
            val bottom = cellY + cellHeight - 1

            graphics.fill(cellX + 1, cellY + 1, right, bottom, rarityColour(def))
            if (def == hovered || def == selected) graphics.fill(cellX + 1, cellY + 1, right, bottom, Common.UI.HOVER_WASH)
            // the picked one is lit and framed twice as thick in the bright colour until it is put down
            if (def == selected) {
                graphics.drawBorder(cellX + 1, cellY + 1, right, bottom, Common.UI.BORDER_SIZE, Common.UI.SELECTED_FRAME_COLOR)
            } else {
                graphics.drawBorder(cellX + 1, cellY + 1, right, bottom, 1, Common.UI.BORDER_COLOR)
            }

            val icon = iconSize()
            graphics.renderFakeItem(stackFor(def), cellX + (cellWidth - icon) / 2, cellY + (cellHeight - icon) / 2, icon, icon)
        }

        if (rows > visibleRows) {
            graphics.drawScrollBar(
                x + width - EDGE_PAD - Common.UI.SCROLLBAR_WIDTH,
                gridTop(),
                visibleRows * cellHeight,
                rows,
                visibleRows,
                scroll
            )
        }
    }

    /** The carried plant under the mouse, seen through, so the slot it is over stays visible. */
    fun renderDrag(graphics: GuiGraphicsExtractor) {
        val def = carried ?: return
        val icon = iconSize()
        val (atX, atY) = if (dragging != null) dragX to dragY else lastMouseX.toInt() to lastMouseY.toInt()
        val left = atX - icon / 2
        val top = atY - icon / 2

        graphics.renderFakeItem(stackFor(def), left, top, icon, icon)
        graphics.fill(left, top, left + icon, top + icon, DRAG_VEIL)
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (infoHovered) {
            graphics.drawSimpleTooltip(SEARCH_HELP, mouseX + 8, mouseY + 8)
            return
        }
        if (carried != null) return
        val def = hovered ?: return
        graphics.drawSimpleTooltip(def.name, mouseX + 7, mouseY + 12)
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        lastMouseX = mouseX
        lastMouseY = mouseY
        hovered = cropAt(mouseX, mouseY)
        clearButton.mouseMoved(mouseX, mouseY)
        deleteButton.mouseMoved(mouseX, mouseY)
        infoHovered = isOverInfo(mouseX, mouseY)
        markSelector.mouseMoved(mouseX, mouseY)
        undoButton.mouseMoved(mouseX, mouseY)
        redoButton.mouseMoved(mouseX, mouseY)
    }

    fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        // a right click puts a picked plant down wherever the mouse is, another cell included
        if (event.button() == 1 && selected != null) {
            selected = null
            return true
        }
        if (search.mouseClicked(event, doubled)) return true
        if (markSelector.mouseClicked(event, doubled)) return true
        if (!isMouseOver(event.x, event.y)) return false

        if (undoButton.mouseClicked(event, doubled)) {
            onUndo()
            return true
        }
        if (redoButton.mouseClicked(event, doubled)) {
            onRedo()
            return true
        }

        if (clearButton.mouseClicked(event, doubled)) {
            onClearAll(event)
            return true
        }
        if (deleteButton.mouseClicked(event, doubled)) {
            deleteMode = !deleteMode
            return true
        }

        // the button is down on a plant: a move makes it a drag, a release in place makes it a pick
        pressed = cropAt(event.x, event.y)
        pressX = event.x
        pressY = event.y
        return true
    }

    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        val held = pressed ?: dragging ?: return false

        if (dragging == null && (abs(mouseX - pressX) > DRAG_SLACK || abs(mouseY - pressY) > DRAG_SLACK)) {
            dragging = held
            selected = null
        }
        if (dragging == null) return true

        dragX = mouseX.toInt()
        dragY = mouseY.toInt()
        return true
    }

    /**
     * Lets go: a dragged plant is handed back for the owner to place, a plain click picks the plant
     * up (or puts a picked one down again) and hands back nothing.
     */
    fun mouseReleased(): CropDefinition? {
        val dragged = dragging
        val clicked = pressed
        dragging = null
        pressed = null

        if (dragged != null) return dragged
        if (clicked != null) selected = if (selected == clicked) null else clicked
        return null
    }

    /** Puts a picked plant down without placing it. */
    fun dropSelection() {
        selected = null
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, scrollY: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        scroll = (scroll - scrollY.toInt().coerceIn(-1, 1)).coerceIn(0, (totalRows() - visibleRows).coerceAtLeast(0))
        hovered = cropAt(lastMouseX, lastMouseY)
        return true
    }

    fun charTyped(event: CharacterEvent): Boolean = search.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = search.keyPressed(event)

    companion object {
        const val TITLE: String = "Plants"
        private const val SEARCH_HINT: String = "Search…"

        private const val ROW: Int = 20

        /** How far the search and the cells sit from the shelf's frame. */
        private const val EDGE_PAD: Int = 4

        /** How far the mouse may move with the button down before a click becomes a drag. */
        private const val DRAG_SLACK: Double = 1.0

        private const val DEAD_PLANT: String = "Dead Plant"

        /** About the cell at 1080p on gui scale 2, and how far from it the fit may stray. */
        private const val TARGET_CELL: Int = 40
        private const val MIN_CELL: Int = 36
        private const val MAX_CELL: Int = 56
        private const val ICON_PAD: Int = 2

        /** The game's rarity colours, toned down, on the ground under each icon. */
        private const val BASE_CROP: Int = 0xFF6B4A2B.toInt()
        private const val RARE_CROP: Int = 0xFF1F6F6B.toInt()
        private const val RARITY_COMMON: Int = 0xFF5C5C5C.toInt()
        private const val RARITY_UNCOMMON: Int = 0xFF2E7D32.toInt()
        private const val RARITY_RARE: Int = 0xFF2F4FA3.toInt()
        private const val RARITY_EPIC: Int = 0xFF7B1F8A.toInt()
        private const val RARITY_LEGENDARY: Int = 0xFFB07A14.toInt()
        private const val ARROW_WIDTH: Int = 16

        private const val INFO_RADIUS: Int = 5
        private const val SEARCH_HELP: String = "Search by name\n@ before a word searches effects, such as @harvest\n# before a word searches the soil a crop grows on, such as #sand"

        /** Laid over the carried plant so it reads as not yet placed. */
        private const val DRAG_VEIL: Int = 0x70101010
    }
}
