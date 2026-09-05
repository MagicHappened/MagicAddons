package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.ScrollView
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.config.BooleanSettingWidget
import org.magic.magicaddons.ui.widgets.config.SettingWidget
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.VersionChecker
import org.magic.magicaddons.util.compat.McCompat

/**
 * The config, filling the window: a header with the search, the categories down the left, and the
 * picked category's features as blocks in a scrolling view on the right.
 */
class ConfigScreen(title: Component, val parent: Screen?) : Screen(title), OverlayContext, ScrollView {

    /** Open lists and histories, drawn over the blocks and offered every input first. */
    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    private val categories = FeatureManager.categories()
    private var selected: FeatureManager.Category = categories.first()

    /** One root widget per feature, kept across category switches so what was unfolded stays so. */
    private val blocks = mutableMapOf<Feature, SettingWidget<Boolean>>()

    private val search = TextField(0, SEARCH_HEIGHT, Component.literal("Search…")).also {
        it.setMaxLength(64)
        it.setResponder { rebuildHits() }
    }

    /** A setting the search found: where it is, and the names down to it. */
    private class SearchHit(val category: FeatureManager.Category, val feature: Feature, val path: List<SettingNode<*>>) {
        val label: String = path.joinToString(" › ") { it.displayName }
    }

    private var hits: List<SearchHit> = emptyList()
    private var dropdownOpen = false
    private var dropdownOpenedAt = 0L
    private var dropdownScroll = 0

    private var scroll = 0
    private var contentHeight = 0
    private var draggingBar = false

    /** A widget to scroll into view on the next layout, set by the search or the edit command. */
    private var pendingReveal: SettingWidget<*>? = null

    private var loaded = false

    // the panels, in screen coordinates, settled by layoutPanels
    private var headerTop = 0
    private var headerBottom = 0
    private var panelsTop = 0
    private var panelsBottom = 0
    private var sideLeft = 0
    private var sideRight = 0
    private var mainLeft = 0
    private var mainRight = 0

    /** The part of the main panel blocks are seen through. */
    private val clipLeft: Int get() = mainLeft + Common.UI.BORDER_SIZE
    private val clipRight: Int get() = mainRight - Common.UI.BORDER_SIZE
    private val clipTop: Int get() = panelsTop + Common.UI.BORDER_SIZE
    private val clipBottom: Int get() = panelsBottom - Common.UI.BORDER_SIZE

    /** Where the blocks lie, in content coordinates: screen coordinates at a scroll of zero. */
    private val contentLeft: Int get() = clipLeft + MAIN_PAD
    private val contentRight: Int get() = clipRight - MAIN_PAD - Common.UI.SCROLLBAR_WIDTH - 2
    private val contentTop: Int get() = clipTop + MAIN_PAD

    private val viewHeight: Int get() = clipBottom - clipTop
    private val maxScroll: Int get() = (contentHeight - viewHeight).coerceAtLeast(0)

    override val viewLeft: Int get() = clipLeft
    override val viewRight: Int get() = clipRight
    override val viewTop: Int get() = clipTop + scroll
    override val viewBottom: Int get() = clipBottom + scroll

    private val closeSize = 16
    private val closeLeft: Int get() = width - MARGIN - HEADER_PAD - closeSize
    private val closeTop: Int get() = headerTop + (HEADER_HEIGHT - closeSize) / 2

    /** Each category's row in the side panel, with the thick divider before the developer ones. */
    private class CategoryRow(val category: FeatureManager.Category, val top: Int, val dividerAbove: Boolean)

    private var categoryRows: List<CategoryRow> = emptyList()

    override fun init() {
        super.init()
        if (!loaded) {
            MagicAddonsConfigJsonHandler.load()
            loaded = true
        }
        VersionChecker.check()
        closeOverlays()
        closeDropdown()
        layoutPanels()
    }

    private fun layoutPanels() {
        headerTop = MARGIN
        headerBottom = headerTop + HEADER_HEIGHT
        panelsTop = headerBottom + PANEL_GAP
        panelsBottom = height - MARGIN
        sideLeft = MARGIN
        sideRight = sideLeft + (width / 5).coerceIn(SIDE_MIN_WIDTH, SIDE_MAX_WIDTH)
        mainLeft = sideRight + PANEL_GAP
        mainRight = width - MARGIN

        search.width = (width / 3).coerceIn(SEARCH_MIN_WIDTH, SEARCH_MAX_WIDTH)
        search.x = (width - search.width) / 2
        search.y = headerTop + (HEADER_HEIGHT - SEARCH_HEIGHT) / 2

        var rowTop = panelsTop + Common.UI.BORDER_SIZE + Common.UI.SPACING
        var dividerPlaced = false
        categoryRows = categories.map { category ->
            val divider = category.dev && !dividerPlaced
            if (divider) {
                dividerPlaced = true
                rowTop += Common.UI.SPACING * 2 + THICK_DIVIDER
            }
            CategoryRow(category, rowTop, divider).also { rowTop += CATEGORY_ROW_HEIGHT }
        }
    }

    private fun blockFor(feature: Feature): SettingWidget<Boolean> =
        blocks.getOrPut(feature) { BooleanSettingWidget(feature.baseSetting, this) }

    private fun shownBlocks(): List<SettingWidget<Boolean>> = selected.features.map { blockFor(it) }

    /** Lays the picked category's blocks down the main view; done before every frame, it is cheap. */
    private fun layoutBlocks() {
        val blockWidth = contentRight - contentLeft
        var currentY = contentTop
        shownBlocks().forEach { block ->
            val inner = block.layoutTree(contentLeft + Common.UI.BORDER_SIZE, currentY + Common.UI.BORDER_SIZE, blockWidth - Common.UI.BORDER_SIZE * 2)
            currentY += inner + Common.UI.BORDER_SIZE * 2 + BLOCK_GAP
        }
        contentHeight = currentY - BLOCK_GAP + MAIN_PAD - contentTop

        pendingReveal?.let { widget ->
            scroll = widget.y - contentTop - Common.UI.SPACING
            pendingReveal = null
        }
        scroll = scroll.coerceIn(0, maxScroll)
    }

    /** Opens the screen on [feature]'s category with its settings unfolded, for the edit command. */
    fun showFeature(feature: Feature) {
        selected = categories.firstOrNull { feature in it.features } ?: return
        val block = blockFor(feature)
        block.unfold(true)
        block.flashUntil = System.currentTimeMillis() + FLASH_MS
        pendingReveal = block
    }

    private fun select(category: FeatureManager.Category) {
        if (category == selected) return
        shownBlocks().forEach { it.dropFocus() }
        closeOverlays()
        selected = category
        scroll = 0
    }

    // ------------------------------------------------------------------ search

    private fun rebuildHits() {
        val query = search.value.trim()
        if (query.isEmpty()) {
            hits = emptyList()
            closeDropdown()
            return
        }

        val found = mutableListOf<SearchHit>()
        fun walk(category: FeatureManager.Category, feature: Feature, node: SettingNode<*>, above: List<SettingNode<*>>) {
            val path = above + node
            if (node.displayName.contains(query, ignoreCase = true)) found.add(SearchHit(category, feature, path))
            val under = node.children.orEmpty() + ((node as? EnumSetting<*>)?.providedChildren ?: emptyList())
            under.forEach { walk(category, feature, it, path) }
        }
        categories.forEach { category ->
            category.features.forEach { feature -> walk(category, feature, feature.baseSetting, emptyList()) }
        }

        hits = found.sortedBy { it.path.size }
        dropdownScroll = 0
        openDropdown()
    }

    private fun openDropdown() {
        if (dropdownOpen) return
        dropdownOpen = true
        dropdownOpenedAt = System.currentTimeMillis()
    }

    private fun closeDropdown() {
        dropdownOpen = false
    }

    private val dropdownWidth: Int get() = (search.width + DROPDOWN_EXTRA).coerceAtMost(width - MARGIN * 2)
    private val dropdownLeft: Int get() = (search.x + search.width / 2 - dropdownWidth / 2).coerceIn(MARGIN, width - MARGIN - dropdownWidth)
    private val dropdownTop: Int get() = search.y + search.height + Common.UI.SPACING_SMALL
    private val dropdownRows: Int get() = hits.size.coerceIn(1, DROPDOWN_MAX_ROWS)
    private val dropdownHeight: Int get() = dropdownRows * DROPDOWN_ROW_HEIGHT + Common.UI.BORDER_SIZE * 2

    private fun overDropdown(mouseX: Double, mouseY: Double): Boolean =
        dropdownOpen && mouseX.toInt() in dropdownLeft until dropdownLeft + dropdownWidth &&
                mouseY.toInt() in dropdownTop until dropdownTop + dropdownHeight

    private fun hitAt(mouseX: Double, mouseY: Double): SearchHit? {
        if (!overDropdown(mouseX, mouseY)) return null
        val row = (mouseY.toInt() - dropdownTop - Common.UI.BORDER_SIZE) / DROPDOWN_ROW_HEIGHT
        return hits.getOrNull(dropdownScroll + row)
    }

    /** Goes to the setting: its category shown, the rows above it unfolded, and it scrolled to and flashed. */
    private fun navigate(hit: SearchHit) {
        select(hit.category)
        val widget = blockFor(hit.feature).reveal(hit.path) ?: return
        widget.flashUntil = System.currentTimeMillis() + FLASH_MS
        pendingReveal = widget
        closeDropdown()
        search.focused = false
    }

    private fun renderDropdown(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!dropdownOpen) return
        val left = dropdownLeft
        val top = dropdownTop

        // clipped to how far it has opened, so it slides out under the field
        val shown = kotlin.math.round(dropdownHeight * eased(dropdownOpenedAt, DROPDOWN_MS)).toInt()
        graphics.enableScissor(left, top, left + dropdownWidth, top + shown)
        graphics.drawPanel(left, top, left + dropdownWidth, top + dropdownHeight)

        val hovered = hitAt(mouseX.toDouble(), mouseY.toDouble())
        val textRoom = dropdownWidth - Common.UI.BORDER_SIZE * 2 - Common.UI.TEXT_X_PAD * 2 - Common.UI.SCROLLBAR_WIDTH
        var rowTop = top + Common.UI.BORDER_SIZE

        if (hits.isEmpty()) {
            graphics.text(font, Component.literal("Nothing matches"), left + Common.UI.BORDER_SIZE + Common.UI.TEXT_X_PAD, rowTop + (DROPDOWN_ROW_HEIGHT - font.lineHeight) / 2, Common.UI.DISABLED_TEXT_COLOR, false)
            graphics.disableScissor()
            return
        }

        hits.drop(dropdownScroll).take(DROPDOWN_MAX_ROWS).forEach { hit ->
            if (hit === hovered) graphics.fill(left + Common.UI.BORDER_SIZE, rowTop, left + dropdownWidth - Common.UI.BORDER_SIZE, rowTop + DROPDOWN_ROW_HEIGHT, Common.UI.HOVER_WASH)

            // the category in the quiet colour, then the names down to the setting
            val textY = rowTop + (DROPDOWN_ROW_HEIGHT - font.lineHeight) / 2
            var textX = left + Common.UI.BORDER_SIZE + Common.UI.TEXT_X_PAD
            val prefix = "${hit.category.name} › "
            graphics.text(font, Component.literal(prefix), textX, textY, Common.UI.TEXT_DIM_COLOR, false)
            textX += font.width(prefix)

            val room = textRoom - font.width(prefix)
            val label = if (font.width(hit.label) <= room) hit.label else font.plainSubstrByWidth(hit.label, room - font.width(ELLIPSIS)) + ELLIPSIS
            graphics.text(font, Component.literal(label), textX, textY, Common.UI.TEXT_COLOR, false)
            rowTop += DROPDOWN_ROW_HEIGHT
        }

        graphics.drawScrollBar(left + dropdownWidth - Common.UI.BORDER_SIZE - Common.UI.SCROLLBAR_WIDTH, top + Common.UI.BORDER_SIZE, dropdownRows * DROPDOWN_ROW_HEIGHT, hits.size, DROPDOWN_MAX_ROWS, dropdownScroll)
        graphics.disableScissor()
    }

    // ------------------------------------------------------------------ drawing

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, deltaTick)
        }
        graphics.fill(0, 0, width, height, Common.UI.SCREEN_DIM_COLOR)
        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        layoutBlocks()

        renderHeader(graphics, mouseX, mouseY)
        renderSidePanel(graphics, mouseX, mouseY)
        renderMain(graphics, mouseX, mouseY, delta)
        renderDropdown(graphics, mouseX, mouseY)
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.drawPanel(MARGIN, headerTop, width - MARGIN, headerBottom)

        // the mod's name a step larger than the text, "Config" beside it in the quiet colour
        val titleX = MARGIN + HEADER_PAD
        val titleY = headerTop + (HEADER_HEIGHT - font.lineHeight * TITLE_SCALE) / 2f
        graphics.pose().pushMatrix()
        graphics.pose().translate(titleX.toFloat(), titleY)
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE)
        graphics.text(font, Component.literal(Common.MOD_NAME), 0, 0, Common.UI.TEXT_COLOR, false)
        graphics.pose().popMatrix()

        val subtitleX = titleX + (font.width(Common.MOD_NAME) * TITLE_SCALE).toInt() + Common.UI.SPACING
        graphics.text(font, Component.literal("Config"), subtitleX, (titleY + (font.lineHeight * TITLE_SCALE - font.lineHeight)).toInt(), Common.UI.TEXT_DIM_COLOR, false)

        search.render(graphics)

        val overClose = mouseX in closeLeft until closeLeft + closeSize && mouseY in closeTop until closeTop + closeSize
        graphics.drawButtonPanel(closeLeft, closeTop, closeLeft + closeSize, closeTop + closeSize, overClose)
        val inset = 5
        graphics.drawLine(closeLeft + inset, closeTop + inset, closeLeft + closeSize - inset, closeTop + closeSize - inset, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(closeLeft + closeSize - inset, closeTop + inset, closeLeft + inset, closeTop + closeSize - inset, 1, Common.UI.TEXT_COLOR)
    }

    private fun renderSidePanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.drawPanel(sideLeft, panelsTop, sideRight, panelsBottom)

        val rowLeft = sideLeft + Common.UI.BORDER_SIZE
        val rowRight = sideRight - Common.UI.BORDER_SIZE

        categoryRows.forEachIndexed { index, row ->
            if (row.dividerAbove) {
                val lineTop = row.top - Common.UI.SPACING - THICK_DIVIDER
                graphics.fill(rowLeft + SIDE_PAD, lineTop, rowRight - SIDE_PAD, lineTop + THICK_DIVIDER, Common.UI.BORDER_COLOR)
            } else if (index > 0) {
                graphics.fill(rowLeft + SIDE_PAD, row.top, rowRight - SIDE_PAD, row.top + 1, Common.UI.THIN_DIVIDER_COLOR)
            }

            val picked = row.category == selected
            val over = mouseX in rowLeft until rowRight && mouseY in row.top until row.top + CATEGORY_ROW_HEIGHT
            if (picked) {
                graphics.fill(rowLeft, row.top, rowRight, row.top + CATEGORY_ROW_HEIGHT, Common.UI.PRESSED_SHADE)
                graphics.fill(rowLeft, row.top, rowLeft + PICK_STRIP, row.top + CATEGORY_ROW_HEIGHT, Common.UI.SELECTED_FRAME_COLOR)
            } else if (over) {
                graphics.fill(rowLeft, row.top, rowRight, row.top + CATEGORY_ROW_HEIGHT, Common.UI.HOVER_WASH)
            }

            graphics.text(
                font,
                Component.literal(row.category.name),
                rowLeft + SIDE_PAD + PICK_STRIP,
                row.top + (CATEGORY_ROW_HEIGHT - font.lineHeight) / 2,
                if (picked) Common.UI.TEXT_COLOR else Common.UI.TEXT_DIM_COLOR,
                false
            )
        }

        // the version at the bottom, and above it the update line when there is a newer one
        val textWidth = rowRight - rowLeft - SIDE_PAD * 2
        var lineY = panelsBottom - Common.UI.BORDER_SIZE - Common.UI.SPACING - font.lineHeight
        val version = font.split(Component.literal(VersionChecker.currentVersion()), textWidth)
        version.asReversed().forEach {
            graphics.text(font, it, rowLeft + SIDE_PAD, lineY, Common.UI.DISABLED_TEXT_COLOR, false)
            lineY -= font.lineHeight
        }
        VersionChecker.result?.takeIf { it.outdated }?.let { found ->
            lineY -= Common.UI.SPACING
            font.split(Component.literal(found.headline()), textWidth).asReversed().forEach {
                graphics.text(font, it, rowLeft + SIDE_PAD, lineY, Common.UI.SELECTED_FRAME_COLOR, false)
                lineY -= font.lineHeight
            }
        }
    }

    private fun renderMain(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(mainLeft, panelsTop, mainRight, panelsBottom)

        val contentMouseY = mouseY + scroll
        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom)
        graphics.pose().pushMatrix()
        graphics.pose().translate(0f, -scroll.toFloat())

        shownBlocks().forEach { block ->
            val left = block.x - Common.UI.BORDER_SIZE
            val top = block.y - Common.UI.BORDER_SIZE
            graphics.drawPanel(left, top, left + (contentRight - contentLeft), top + block.totalHeight() + Common.UI.BORDER_SIZE * 2)
            block.render(graphics, mouseX, contentMouseY, delta)
        }
        overlays.asReversed().forEach { it.renderOverlay(graphics, mouseX, contentMouseY, delta) }

        graphics.pose().popMatrix()
        graphics.disableScissor()

        graphics.drawScrollBar(clipRight - Common.UI.SCROLLBAR_WIDTH - 1, clipTop, viewHeight, contentHeight, viewHeight, scroll)
    }

    // ------------------------------------------------------------------ input

    private fun overMain(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in clipLeft until clipRight && mouseY.toInt() in clipTop until clipBottom

    private fun overBar(mouseX: Double, mouseY: Double): Boolean =
        maxScroll > 0 && mouseX.toInt() >= clipRight - Common.UI.SCROLLBAR_WIDTH - 3 && overMain(mouseX, mouseY)

    /** The event moved into content coordinates, which the blocks live in. */
    private fun shifted(event: MouseButtonEvent): MouseButtonEvent =
        MouseButtonEvent(event.x, event.y + scroll, MouseButtonInfo(event.button(), event.modifiers()))

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        // the second event of a double click is the same click again; acting on it would undo the first
        if (doubled) return true

        hitAt(event.x, event.y)?.let {
            navigate(it)
            return true
        }
        if (overDropdown(event.x, event.y)) return true

        if (search.mouseClicked(event, doubled)) {
            if (search.value.isNotBlank()) openDropdown()
            return true
        }
        closeDropdown()

        if (event.x.toInt() in closeLeft until closeLeft + closeSize && event.y.toInt() in closeTop until closeTop + closeSize) {
            onClose()
            return true
        }

        categoryRows.firstOrNull {
            event.x.toInt() in sideLeft until sideRight && event.y.toInt() in it.top until it.top + CATEGORY_ROW_HEIGHT
        }?.let {
            select(it.category)
            return true
        }

        if (event.button() == 0 && overBar(event.x, event.y)) {
            draggingBar = true
            return true
        }

        if (!overMain(event.x, event.y)) {
            // a click off the blocks still lets a focused field go
            shownBlocks().forEach { it.dropFocus() }
            return super.mouseClicked(event, doubled)
        }

        val content = shifted(event)
        // an open list takes the click if it lands inside it; anywhere else closes every list and
        // the click goes on to the settings underneath
        if (overlays.toList().any { it.mouseClicked(content, doubled) }) return true
        if (overlays.isNotEmpty()) closeOverlays()

        var handled = false
        shownBlocks().forEach { if (it.mouseClicked(content, doubled)) handled = true }
        return handled
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (draggingBar) {
            draggingBar = false
            return true
        }
        return shownBlocks().any { it.mouseReleased(shifted(event)) }
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (draggingBar) {
            scroll = (((event.y - clipTop) / viewHeight) * contentHeight - viewHeight / 2).toInt().coerceIn(0, maxScroll)
            return true
        }
        return shownBlocks().any { it.mouseDragged(shifted(event), dragX, dragY) }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val contentY = mouseY + scroll
        overlays.forEach { it.mouseMoved(mouseX, contentY) }
        shownBlocks().forEach { it.mouseMoved(mouseX, contentY) }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (overDropdown(mouseX, mouseY)) {
            dropdownScroll = (dropdownScroll - scrollY.toInt().coerceIn(-1, 1)).coerceIn(0, (hits.size - DROPDOWN_MAX_ROWS).coerceAtLeast(0))
            return true
        }
        if (!overMain(mouseX, mouseY)) return false

        val contentY = mouseY + scroll
        if (overlays.any { it.mouseScrolled(mouseX, contentY, scrollX, scrollY) }) return true
        if (shownBlocks().any { it.mouseScrolled(mouseX, contentY, scrollX, scrollY) }) return true

        scroll = (scroll - (scrollY * Common.UI.SCROLL_STEP).toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (search.charTyped(characterEvent)) return true
        if (overlays.any { it.charTyped(characterEvent) }) return true
        return shownBlocks().any { it.charTyped(characterEvent) }
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            // escape backs out one step: the search, then the open lists, then the screen
            if (search.focused) {
                search.focused = false
                closeDropdown()
                return true
            }
            if (overlays.isNotEmpty()) {
                closeOverlays()
                return true
            }
        }
        if (search.focused) {
            if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
                hits.firstOrNull()?.let { navigate(it) }
                return true
            }
            if (search.keyPressed(keyEvent)) return true
        }
        if (overlays.any { it.keyPressed(keyEvent) }) return true
        if (shownBlocks().any { it.keyPressed(keyEvent) }) return true
        return super.keyPressed(keyEvent)
    }

    override fun onClose() {
        McCompat.setScreen(parent)
    }

    override fun removed() {
        MagicAddonsConfigJsonHandler.save()
    }

    private companion object {
        const val MARGIN: Int = 6
        const val PANEL_GAP: Int = Common.UI.SPACING
        const val HEADER_HEIGHT: Int = 30
        const val HEADER_PAD: Int = 8
        const val TITLE_SCALE: Float = 1.3f
        const val SEARCH_HEIGHT: Int = 16
        const val SEARCH_MIN_WIDTH: Int = 100
        const val SEARCH_MAX_WIDTH: Int = 240

        const val SIDE_MIN_WIDTH: Int = 90
        const val SIDE_MAX_WIDTH: Int = 150
        const val SIDE_PAD: Int = 6
        const val CATEGORY_ROW_HEIGHT: Int = 18
        const val PICK_STRIP: Int = 3
        const val THICK_DIVIDER: Int = 2

        const val MAIN_PAD: Int = Common.UI.SPACING_LARGE
        const val BLOCK_GAP: Int = Common.UI.SPACING_LARGE

        const val DROPDOWN_ROW_HEIGHT: Int = 14
        const val DROPDOWN_MAX_ROWS: Int = 8
        const val DROPDOWN_EXTRA: Int = 120
        const val DROPDOWN_MS: Long = 150
        const val ELLIPSIS: String = "…"

        /** How long a row found by the search stays framed. */
        const val FLASH_MS: Long = 1500
    }
}
