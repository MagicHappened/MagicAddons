package org.magic.magicaddons.ui.widgets.greenhouse

import kotlin.math.abs
import kotlin.math.roundToInt
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawCheckerboard
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.drawShelf
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLinesAtCursor
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.modText
import org.magic.magicaddons.util.ScreenUtil.drawItem
import org.magic.magicaddons.util.ScreenUtil.splitMod
import org.magic.magicaddons.util.ScreenUtil.stepScroll
import org.magic.magicaddons.util.compat.McCompat

class PlantPalette(
    overlayContext: OverlayContext,
    private val onClearAll: (MouseButtonEvent) -> Unit,
    private val onUndo: () -> Unit,
    private val onRedo: () -> Unit,
    private val uniqueMissing: (CropDefinition) -> Boolean
) {

    private val markSelector = DropdownWidget(
        values = MarkChoice.entries,
        currentValue = MarkChoice.Off,
        overlayContext = overlayContext,
        isSearchable = false,
        onValueChanged = { picked -> if (picked.applies) clearTools(keepMark = true) }
    )

    val markChoice: MarkChoice get() = markSelector.currentValue ?: MarkChoice.Off

    val isHoldingTool: Boolean get() = selectedItem != null || isDeleteMode || isUniquesMode || markChoice.applies

    fun pickUp(item: PaletteItem) {
        clearTools()
        selectedItem = item
    }

    fun clearTools(keepMark: Boolean = false) {
        selectedItem = null
        isDeleteMode = false
        isUniquesMode = false
        if (!keepMark) markSelector.currentValue = MarkChoice.Off
    }

    private val undoButton = ClickableButtonWidget(ARROW_WIDTH, ROW_HEIGHT, Component.literal("←"))
    private val redoButton = ClickableButtonWidget(ARROW_WIDTH, ROW_HEIGHT, Component.literal("→"))
    private var x: Int = 0
    private var y: Int = 0
    private var width: Int = 0
    private var height: Int = 0

    private val font = Minecraft.getInstance().font

    private val searchBox = TextField(0, ROW_HEIGHT, Component.literal(Common.UI.SEARCH_HINT)).apply {
        setResponder { scroll = 0 }
    }

    private val clearButton = ClickableButtonWidget(ClickableButtonWidget.widthFor("Clear all"), ROW_HEIGHT, Component.literal("Clear all"))
    private val deleteButton = ClickableButtonWidget(ClickableButtonWidget.widthFor("Delete"), ROW_HEIGHT, Component.literal("Delete"))
    private val uniquesButton = ClickableButtonWidget(ClickableButtonWidget.widthFor("Uniques"), ROW_HEIGHT, Component.literal("Uniques"))
    private val mergeButton = ClickableButtonWidget(ClickableButtonWidget.widthFor("Merge"), ROW_HEIGHT, Component.literal("Merge"))

    private val buttons = listOf(clearButton, deleteButton, uniquesButton, mergeButton, undoButton, redoButton)

    var isUniquesMode: Boolean = false

    var isMergeMode: Boolean = false
        private set

    var isDeleteMode: Boolean = false
        private set

    private var hoveredItem: PaletteItem? = null

    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    private var draggedItem: PaletteItem? = null
    private var dragX = 0
    private var dragY = 0

    var selectedItem: PaletteItem? = null
        private set

    private var pressedItem: PaletteItem? = null
    private var pressX = 0.0
    private var pressY = 0.0

    val carriedItem: PaletteItem? get() = draggedItem ?: selectedItem

    private var scroll = 0
    private var columns = 1
    private var visibleRows = 1

    private var cellWidth = MIN_CELL_SIZE
    private var cellHeight = MIN_CELL_SIZE

    private val crops: List<CropDefinition> = CropRegistry.allCrops
        .filter { it.skyblockId != null }
        .sortedWith(compareBy({ paletteSortOrder(it) }, { it.name.lowercase() }))

    private fun paletteSortOrder(def: CropDefinition): Double =
        if (def.name == DEAD_PLANT) 0.5 else def.tier.ordinal.toDouble()

    private val soils: List<Block> = CropRegistry.allCrops
        .flatMap { it.requiredSoil }
        .distinct()
        .sortedBy { it.name.string.lowercase() }

    private val items: List<PaletteItem> =
        crops.map { PaletteItem.Crop(it) } + soils.map { PaletteItem.Soil(it) } + PaletteItem.Soil(Blocks.AIR)

    private fun soilSearchWord(block: Block): String = block.name.string.replace(" ", "").lowercase()

    private fun matchingItems(): List<PaletteItem> {
        val searchText = searchBox.value.trim()
        return when {
            searchText.startsWith("@") -> {
                val searchTerm = searchText.drop(1).trim()
                items.filter { item -> item is PaletteItem.Crop && item.def.effects.any { it.label.contains(searchTerm, ignoreCase = true) } }
            }
            searchText.startsWith("#") -> {
                val searchTerm = searchText.drop(1).trim().replace(" ", "").lowercase()
                items.filter { item ->
                    when (item) {
                        is PaletteItem.Crop -> item.def.requiredSoil.any { soilSearchWord(it).startsWith(searchTerm) }
                        is PaletteItem.Soil -> soilSearchWord(item.block).startsWith(searchTerm)
                    }
                }
            }
            else -> items.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }

    private var isSearchHelpHovered = false

    private fun searchHelpIconCenter(): Pair<Int, Int> = (x + width - EDGE_PADDING - SEARCH_HELP_ICON_RADIUS - 1) to (y + titleHeight() / 2)

    private fun renderSearchHelpIcon(graphics: GuiGraphicsExtractor) {
        val (cx, cy) = searchHelpIconCenter()
        for (dy in -SEARCH_HELP_ICON_RADIUS..SEARCH_HELP_ICON_RADIUS) {
            val half = kotlin.math.sqrt((SEARCH_HELP_ICON_RADIUS * SEARCH_HELP_ICON_RADIUS - dy * dy).toDouble()).toInt()
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, if (isSearchHelpHovered) Common.UI.ACCENT_COLOR else Common.UI.BORDER_COLOR)
        }
        val inner = SEARCH_HELP_ICON_RADIUS - 1
        for (dy in -inner..inner) {
            val half = kotlin.math.sqrt((inner * inner - dy * dy).toDouble()).toInt()
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, Common.UI.BACKGROUND_COLOR)
        }
        graphics.modText(font, Component.literal("i"), cx - font.width("i") / 2 + 1, cy - font.lineHeight / 2 + 1, Common.UI.TEXT_COLOR)
    }

    private fun isOverSearchHelpIcon(mouseX: Double, mouseY: Double): Boolean {
        val (cx, cy) = searchHelpIconCenter()
        return mouseX.toInt() in cx - SEARCH_HELP_ICON_RADIUS..cx + SEARCH_HELP_ICON_RADIUS &&
                mouseY.toInt() in cy - SEARCH_HELP_ICON_RADIUS..cy + SEARCH_HELP_ICON_RADIUS
    }

    private fun titleHeight(): Int = font.lineHeight + Common.UI.SPACING * 2

    private var buttonRows: Int = 1

    private fun cellsTop(): Int = y + titleHeight() + (ROW_HEIGHT + Common.UI.SPACING) * (1 + buttonRows)
    private fun cellsLeft(): Int = x + EDGE_PADDING

    private fun iconSize(): Int = ((minOf(cellWidth, cellHeight) - ICON_PADDING * 2) / 16 * 16).coerceAtLeast(16)

    private fun cellColorOf(item: PaletteItem): Int = when (item) {
        is PaletteItem.Soil -> SOIL_CELL_COLOR
        is PaletteItem.Crop -> rarityColor(item.def)
    }

    private fun rarityColor(def: CropDefinition): Int = when (def.tier) {
        CropTier.Common -> RARITY_COMMON
        CropTier.Uncommon -> RARITY_UNCOMMON
        CropTier.Rare -> RARITY_RARE
        CropTier.Epic -> RARITY_EPIC
        CropTier.Legendary -> RARITY_LEGENDARY
        CropTier.RareCrop -> RARE_CROP
        else -> BASE_CROP
    }

    fun layoutIn(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        searchBox.x = x + EDGE_PADDING
        searchBox.y = y + titleHeight()
        searchBox.width = width - EDGE_PADDING * 2

        val right = x + width - EDGE_PADDING
        var rowX = searchBox.x
        var rowY = searchBox.y + ROW_HEIGHT + Common.UI.SPACING
        buttonRows = 1

        fun nextRow() {
            rowX = searchBox.x
            rowY += ROW_HEIGHT + Common.UI.SPACING
            buttonRows++
        }

        listOf(clearButton, deleteButton, uniquesButton, mergeButton).forEach { button ->
            if (rowX > searchBox.x && rowX + button.width > right) nextRow()

            button.x = rowX
            button.y = rowY
            rowX += button.width + Common.UI.SPACING
        }

        val arrowsWidth = ARROW_WIDTH * 2 + Common.UI.SPACING * 2
        markSelector.fitToValues(right - searchBox.x - arrowsWidth)
        if (rowX > searchBox.x && rowX + markSelector.width + arrowsWidth > right) nextRow()

        markSelector.x = rowX
        markSelector.y = rowY
        markSelector.height = ROW_HEIGHT
        undoButton.x = markSelector.x + markSelector.width + Common.UI.SPACING
        undoButton.y = rowY
        redoButton.x = undoButton.x + ARROW_WIDTH + Common.UI.SPACING
        redoButton.y = rowY

        val cellsWidth = width - EDGE_PADDING * 2
        val cellsHeight = (y + height - EDGE_PADDING - cellsTop()).coerceAtLeast(MIN_CELL_SIZE)
        visibleRows = (cellsHeight.toFloat() / TARGET_CELL_SIZE).roundToInt().coerceAtLeast(1)
        cellHeight = (cellsHeight / visibleRows).coerceIn(MIN_CELL_SIZE, MAX_CELL_SIZE)
        columns = (cellsWidth / cellHeight).coerceAtLeast(1)
        cellWidth = cellsWidth / columns
    }

    private fun totalRows(): Int = (matchingItems().size + columns - 1) / columns

    private fun itemAt(mouseX: Double, mouseY: Double): PaletteItem? {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        if (mx < cellsLeft() || my < cellsTop() || my >= cellsTop() + visibleRows * cellHeight) return null

        val column = (mx - cellsLeft()) / cellWidth
        val row = (my - cellsTop()) / cellHeight + scroll
        if (column >= columns) return null

        return matchingItems().getOrNull(row * columns + column)
    }

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawShelf(x, y, x + width, y + height, TITLE)
        renderSearchHelpIcon(graphics)

        searchBox.render(graphics)
        deleteButton.isPressed = isDeleteMode
        uniquesButton.isPressed = isUniquesMode
        mergeButton.isPressed = isMergeMode

        markSelector.frameColor = when (markChoice) {
            MarkChoice.Clear -> Common.UI.DANGER_COLOR
            else -> markChoice.marking?.color
        }
        markSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        buttons.forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }

        val matching = matchingItems()
        val rows = totalRows()
        scroll = scroll.coerceIn(0, (rows - visibleRows).coerceAtLeast(0))

        matching.drop(scroll * columns).take(visibleRows * columns).forEachIndexed { index, item ->
            val cellX = cellsLeft() + index % columns * cellWidth
            val cellY = cellsTop() + index / columns * cellHeight
            val right = cellX + cellWidth - 1
            val bottom = cellY + cellHeight - 1

            graphics.fill(cellX + 1, cellY + 1, right, bottom, cellColorOf(item))
            if (item == hoveredItem || item == selectedItem) graphics.fill(cellX + 1, cellY + 1, right, bottom, Common.UI.HOVER_WASH)
            if (item == selectedItem) {
                graphics.drawBorder(cellX + 1, cellY + 1, right, bottom, Common.UI.BORDER_SIZE, Common.UI.SELECTED_FRAME_COLOR)
            } else {
                graphics.drawBorder(cellX + 1, cellY + 1, right, bottom, 1, Common.UI.BORDER_COLOR)
            }

            val iconSize = iconSize()
            drawIcon(graphics, item, cellX + (cellWidth - iconSize) / 2, cellY + (cellHeight - iconSize) / 2, iconSize)

            if (isUniquesMode && item is PaletteItem.Crop && uniqueMissing(item.def)) {
                graphics.fill(cellX + 1, cellY + 1, right, bottom, MISSING_UNIQUE_WASH)
                graphics.drawBorder(cellX + 1, cellY + 1, right, bottom, Common.UI.BORDER_SIZE, Common.UI.DANGER_COLOR)
            }
        }

        if (rows > visibleRows) {
            graphics.drawScrollBar(
                x + width - EDGE_PADDING - Common.UI.SCROLLBAR_WIDTH,
                cellsTop(),
                visibleRows * cellHeight,
                rows,
                visibleRows,
                scroll
            )
        }
    }

    fun renderDrag(graphics: GuiGraphicsExtractor) {
        val item = carriedItem ?: return
        val iconSize = iconSize()
        val (atX, atY) = if (draggedItem != null) dragX to dragY else lastMouseX.toInt() to lastMouseY.toInt()
        val left = atX - iconSize / 2
        val top = atY - iconSize / 2

        drawIcon(graphics, item, left, top, iconSize)
        graphics.fill(left, top, left + iconSize, top + iconSize, DRAG_VEIL)
    }

    private fun drawIcon(graphics: GuiGraphicsExtractor, item: PaletteItem, left: Int, top: Int, iconSize: Int) {
        if (item is PaletteItem.Soil && item.block == Blocks.AIR) {
            graphics.drawCheckerboard(left, top, left + iconSize, top + iconSize)
            val label = Component.literal(item.name)
            graphics.text(font, label, left + (iconSize - font.width(label)) / 2, top + (iconSize - font.lineHeight) / 2, Common.UI.TEXT_COLOR, true)
            return
        }
        graphics.drawItem(item.stack, left, top, iconSize, iconSize)
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (isSearchHelpHovered) {
            val tooltipMaxWidth = (McCompat.currentScreen()?.width ?: Int.MAX_VALUE) - (mouseX + ScreenUtil.CURSOR_TOOLTIP_X) - Common.UI.TEXT_X_PAD * 2
            graphics.drawTooltipAtCursor(SEARCH_HELP, mouseX, mouseY, tooltipMaxWidth)
            return
        }
        if (carriedItem != null) return
        val item = hoveredItem ?: return

        val lines = buildList {
            add(Component.literal(item.name).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).visualOrderText)
            (item as? PaletteItem.Crop)?.def?.effects?.forEach { add(Component.literal(it.label).withStyle(ChatFormatting.GRAY).visualOrderText) }
            if (item is PaletteItem.Soil && item.block == Blocks.AIR) {
                add(Component.empty().visualOrderText)
                addAll(font.splitMod(Component.literal(AIR_NOTE).withStyle(ChatFormatting.GRAY), NOTE_WIDTH))
            }
        }
        graphics.drawTooltipLinesAtCursor(lines, mouseX, mouseY)
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, width, height)

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        lastMouseX = mouseX
        lastMouseY = mouseY
        hoveredItem = itemAt(mouseX, mouseY)
        isSearchHelpHovered = isOverSearchHelpIcon(mouseX, mouseY)
        markSelector.mouseMoved(mouseX, mouseY)
        buttons.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (searchBox.mouseClicked(event, doubled)) return true
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
            val isTurningOn = !isDeleteMode
            clearTools()
            isDeleteMode = isTurningOn
            return true
        }
        if (uniquesButton.mouseClicked(event, doubled)) {
            val isTurningOn = !isUniquesMode
            clearTools()
            isUniquesMode = isTurningOn
            return true
        }
        if (mergeButton.mouseClicked(event, doubled)) {
            isMergeMode = !isMergeMode
            return true
        }

        pressedItem = itemAt(event.x, event.y)
        pressX = event.x
        pressY = event.y
        return true
    }

    fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val heldItem = pressedItem ?: draggedItem ?: return false

        if (draggedItem == null && (abs(event.x - pressX) > DRAG_THRESHOLD || abs(event.y - pressY) > DRAG_THRESHOLD)) {
            draggedItem = heldItem
            selectedItem = null
        }
        if (draggedItem == null) return true

        this.dragX = event.x.toInt()
        this.dragY = event.y.toInt()
        return true
    }

    fun mouseReleased(): PaletteItem? {
        val dragged = draggedItem
        val clicked = pressedItem
        draggedItem = null
        pressedItem = null

        if (dragged != null) return dragged
        if (clicked != null) {
            val isPickingUp = selectedItem != clicked
            clearTools()
            if (isPickingUp) selectedItem = clicked
        }
        return null
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        scroll = stepScroll(scroll, scrollY, totalRows(), visibleRows)
        hoveredItem = itemAt(lastMouseX, lastMouseY)
        return true
    }

    fun charTyped(event: CharacterEvent): Boolean = searchBox.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = searchBox.keyPressed(event)

    companion object {
        const val TITLE: String = "Plants"

        private const val ROW_HEIGHT: Int = 20

        private const val EDGE_PADDING: Int = 4

        private const val DRAG_THRESHOLD: Double = 1.0

        private const val DEAD_PLANT: String = "Dead Plant"

        private const val TARGET_CELL_SIZE: Int = 40
        private const val MIN_CELL_SIZE: Int = 36
        private const val MAX_CELL_SIZE: Int = 56
        private const val ICON_PADDING: Int = 2

        private const val BASE_CROP: Int = 0xFF6B4A2B.toInt()
        private const val RARE_CROP: Int = 0xFF1F6F6B.toInt()
        private const val RARITY_COMMON: Int = 0xFF5C5C5C.toInt()
        private const val RARITY_UNCOMMON: Int = 0xFF2E7D32.toInt()
        private const val RARITY_RARE: Int = 0xFF2F4FA3.toInt()
        private const val RARITY_EPIC: Int = 0xFF7B1F8A.toInt()
        private const val RARITY_LEGENDARY: Int = 0xFFB07A14.toInt()

        private const val SOIL_CELL_COLOR: Int = 0xFF8A4A5E.toInt()

        private const val MISSING_UNIQUE_WASH: Int = 0x38FF0000

        private const val AIR_NOTE: String = "Useful for separating a Devourer from eating your other crops: " +
                "the hologram will ask for an air block here instead of allowing any block."
        private const val NOTE_WIDTH: Int = 170
        private const val ARROW_WIDTH: Int = 16

        private const val SEARCH_HELP_ICON_RADIUS: Int = 5
        private const val SEARCH_HELP: String = "Search by name\n" +
                "§b@§r before a word searches §beffects§r, such as §b@harvest§r\n" +
                "§6#§r before a word searches the §6soil§r a crop grows on, such as §6#sand§r"

        private const val DRAG_VEIL: Int = 0x70101010
    }
}
