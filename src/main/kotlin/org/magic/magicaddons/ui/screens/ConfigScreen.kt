package org.magic.magicaddons.ui.screens

import org.magic.magicaddons.util.ScreenUtil.splitMod
import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.ScrollView
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.config.BooleanSettingWidget
import org.magic.magicaddons.ui.widgets.config.SettingWidget
import org.magic.magicaddons.util.ScreenUtil.at
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.ui.background.ConfigBackground
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.stepScroll
import org.magic.magicaddons.util.VersionChecker
import org.magic.magicaddons.util.compat.McCompat

/**
 magic addons config screen
 */
class ConfigScreen(val parent: Screen?) : MagicScreen(Component.literal("Magic Addons Config"), "the config screen"), OverlayContext, ScrollView {

    /** currently opened overlays */
    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    private val categories = FeatureManager.categories()
    private var selected: FeatureManager.Category = categories.first()

    /** One root widget per feature, kept as a list so switching categories keep the unfolds  */
    private val blocks = mutableMapOf<Feature, SettingWidget<Boolean>>()

    private val search = TextField(0, SEARCH_HEIGHT, Component.literal(Common.UI.SEARCH_HINT)).also {
        it.setMaxLength(64)
        it.setResponder { rebuildHits() }
    }

    /** describes search results to display */
    private class SearchResult(val category: FeatureManager.Category, val feature: Feature, val path: List<SettingNode<*>>) {
        val label: String = path.joinToString(" › ") { it.displayName }
    }

    private var searchResults: List<SearchResult> = emptyList()
    private var dropdownOpen = false
    private var dropdownOpenedAt = 0L
    private var dropdownScroll = 0

    private var scroll = 0
    private var contentHeight = 0
    private var draggingBar = false

    /** a widget that is displayed by automatically navigating to it (eg edit command or search) */
    private var navigatedWidget: SettingWidget<*>? = null

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

    /** where the content is placed in relation to scroll of zero. */
    private val contentLeft: Int get() = clipLeft + MAIN_PAD
    private val contentRight: Int get() = clipRight - MAIN_PAD - Common.UI.SCROLLBAR_WIDTH - 2
    private val contentTop: Int get() = clipTop + MAIN_PAD

    private val viewHeight: Int get() = clipBottom - clipTop
    private val maxScroll: Int get() = (contentHeight - viewHeight).coerceAtLeast(0)

    override val viewLeft: Int get() = clipLeft
    override val viewRight: Int get() = clipRight
    override val viewTop: Int get() = clipTop + scroll
    override val viewBottom: Int get() = clipBottom + scroll

    private val closeLeft: Int get() = width - MARGIN - HEADER_PAD - CLOSE_SIZE
    private val closeTop: Int get() = headerTop + (HEADER_HEIGHT - CLOSE_SIZE) / 2

    private fun overClose(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, closeLeft, closeTop, CLOSE_SIZE, CLOSE_SIZE)

    /** category rows definitions, with a boolean for ones below the separator */
    private class CategoryRow(val category: FeatureManager.Category, val top: Int, val dividerAbove: Boolean)

    private var categoryRows: List<CategoryRow> = emptyList()

    /** How much smaller everything is drawn than it is laid out, so the player's scale is honoured. */
    private var drawScale: Float = 1f

    /** Whether a mouse button is down, which is what tells a slider being dragged from one let go of. */
    private var mouseHeld: Boolean = false

    override fun onInit() {
        super.onInit()
        if (!loaded) {
            MagicAddonsConfigJsonHandler.load()
            loaded = true
        }
        VersionChecker.check()

        // laid out in as many units as the scale asks for, then drawn at that scale to fit the window
        drawScale = Customization.uiScale
        width = (width / drawScale).toInt()
        height = (height / drawScale).toInt()

        closeOverlays()
        closeDropdown()
        layoutPanels()
    }

    /** Lays the screen out again at the scale just picked. */
    private fun rebuildAtNewScale() {
        val window = minecraft?.window ?: return

        resize(window.guiScaledWidth, window.guiScaledHeight)
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
            val divider = category.isUnrelatedToGame && !dividerPlaced
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

        navigatedWidget?.let { widget ->
            scroll = widget.y - contentTop - Common.UI.SPACING
            navigatedWidget = null
        }
        scroll = scroll.coerceIn(0, maxScroll)
    }

    /** Opens the screen on one setting of [feature], the rows above it unfolded and it scrolled to. */
    fun showSetting(feature: Feature, path: List<SettingNode<*>>) {
        selected = categories.firstOrNull { feature in it.features } ?: return
        val widget = blockFor(feature).reveal(path) ?: return
        widget.flashUntil = System.currentTimeMillis() + FLASH_MS
        navigatedWidget = widget
    }

    /** Opens the screen on [feature]'s category with its settings unfolded, for the edit command. */
    fun showFeature(feature: Feature) {
        selected = categories.firstOrNull { feature in it.features } ?: return
        val block = blockFor(feature)
        block.unfold(true)
        block.flashUntil = System.currentTimeMillis() + FLASH_MS
        navigatedWidget = block
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
            searchResults = emptyList()
            closeDropdown()
            return
        }

        val found = mutableListOf<SearchResult>()
        fun walk(category: FeatureManager.Category, feature: Feature, node: SettingNode<*>, above: List<SettingNode<*>>) {
            val path = above + node
            if (node.displayName.contains(query, ignoreCase = true)) found.add(SearchResult(category, feature, path))
            val under = node.children.orEmpty() + ((node as? EnumSetting<*>)?.providedChildren ?: emptyList())
            under.forEach { walk(category, feature, it, path) }
        }
        categories.forEach { category ->
            category.features.forEach { feature -> walk(category, feature, feature.baseSetting, emptyList()) }
        }

        searchResults = found.sortedBy { it.path.size }
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
    private val dropdownRows: Int get() = searchResults.size.coerceIn(1, DROPDOWN_MAX_ROWS)
    private val dropdownHeight: Int get() = dropdownRows * DROPDOWN_ROW_HEIGHT + Common.UI.BORDER_SIZE * 2

    private fun overDropdown(mouseX: Double, mouseY: Double): Boolean =
        dropdownOpen && inRect(mouseX, mouseY, dropdownLeft, dropdownTop, dropdownWidth, dropdownHeight)

    private fun hitAt(mouseX: Double, mouseY: Double): SearchResult? {
        if (!overDropdown(mouseX, mouseY)) return null
        val row = (mouseY.toInt() - dropdownTop - Common.UI.BORDER_SIZE) / DROPDOWN_ROW_HEIGHT
        return searchResults.getOrNull(dropdownScroll + row)
    }

    /** Goes to the setting: its category shown, the rows above it unfolded, and it scrolled to and flashed. */
    private fun navigate(hit: SearchResult) {
        select(hit.category)
        val widget = blockFor(hit.feature).reveal(hit.path) ?: return
        widget.flashUntil = System.currentTimeMillis() + FLASH_MS
        navigatedWidget = widget
        closeDropdown()
        search.focused = false
    }

    private fun renderDropdown(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!dropdownOpen) return
        val left = dropdownLeft
        val top = dropdownTop

        // clipped to how far it has opened, so it slides out under the field; not clipped once open,
        // since on a scaled screen the clip rounds down and shaves the borders off
        val opened = eased(dropdownOpenedAt, DROPDOWN_MS)
        val stillOpening = opened < 1f
        if (stillOpening) {
            val shown = kotlin.math.round(dropdownHeight * opened).toInt()
            graphics.enableScissor(left, top, left + dropdownWidth, top + shown)
        }
        graphics.drawPanel(left, top, left + dropdownWidth, top + dropdownHeight)

        val hovered = hitAt(mouseX.toDouble(), mouseY.toDouble())
        val textRoom = dropdownWidth - Common.UI.BORDER_SIZE * 2 - Common.UI.TEXT_X_PAD * 2 - Common.UI.SCROLLBAR_WIDTH
        var rowTop = top + Common.UI.BORDER_SIZE

        if (searchResults.isEmpty()) {
            graphics.modText(font, Component.literal("Nothing matches"), left + Common.UI.BORDER_SIZE + Common.UI.TEXT_X_PAD, rowTop + (DROPDOWN_ROW_HEIGHT - font.lineHeight) / 2, Common.UI.DISABLED_TEXT_COLOR)
            if (stillOpening) graphics.disableScissor()
            return
        }

        searchResults.drop(dropdownScroll).take(DROPDOWN_MAX_ROWS).forEach { hit ->
            if (hit === hovered) graphics.fill(left + Common.UI.BORDER_SIZE, rowTop, left + dropdownWidth - Common.UI.BORDER_SIZE, rowTop + DROPDOWN_ROW_HEIGHT, Common.UI.HOVER_WASH)

            // the category in the quiet colour, then the names down to the setting
            val textY = rowTop + (DROPDOWN_ROW_HEIGHT - font.lineHeight) / 2
            var textX = left + Common.UI.BORDER_SIZE + Common.UI.TEXT_X_PAD
            val prefix = "${hit.category.name} › "
            graphics.modText(font, Component.literal(prefix), textX, textY, Common.UI.TEXT_DIM_COLOR)
            textX += font.width(prefix)

            val label = ellipsised(font, hit.label, textRoom - font.width(prefix))
            graphics.modText(font, Component.literal(label), textX, textY, Common.UI.TEXT_COLOR)
            rowTop += DROPDOWN_ROW_HEIGHT
        }

        graphics.drawScrollBar(left + dropdownWidth - Common.UI.BORDER_SIZE - Common.UI.SCROLLBAR_WIDTH, top + Common.UI.BORDER_SIZE, dropdownRows * DROPDOWN_ROW_HEIGHT, searchResults.size, DROPDOWN_MAX_ROWS, dropdownScroll)
        if (stillOpening) graphics.disableScissor()
    }

    // ------------------------------------------------------------------ drawing

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // the slider that sets it lives on this screen, so a change is noticed here rather than sent.
        // Laying out again mid-drag would take the slider out from under the mouse, so it waits
        if (Customization.uiScale != drawScale && !mouseHeld) {
            rebuildAtNewScale()
            return
        }

        super.onRender(graphics, mouseX, mouseY, delta)

        graphics.pose().pushMatrix()
        graphics.pose().scale(drawScale, drawScale)

        val scaledMouseX = (mouseX / drawScale).toInt()
        val scaledMouseY = (mouseY / drawScale).toInt()

        layoutBlocks()

        renderHeader(graphics, scaledMouseX, scaledMouseY)
        renderSidePanel(graphics, scaledMouseX, scaledMouseY)
        renderMain(graphics, scaledMouseX, scaledMouseY, delta)
        renderDropdown(graphics, scaledMouseX, scaledMouseY)

        graphics.pose().popMatrix()
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.drawPanel(MARGIN, headerTop, width - MARGIN, headerBottom)

        // the mod's name a step larger than the text, "Config" beside it in the quiet colour
        val titleX = MARGIN + HEADER_PAD
        val titleY = headerTop + (HEADER_HEIGHT - font.lineHeight * TITLE_SCALE) / 2f
        graphics.pose().pushMatrix()
        graphics.pose().translate(titleX.toFloat(), titleY)
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE)
        graphics.modText(font, Component.literal(Common.MOD_NAME), 0, 0, Common.UI.TEXT_COLOR)
        graphics.pose().popMatrix()

        val subtitleX = titleX + (font.width(Common.MOD_NAME) * TITLE_SCALE).toInt() + Common.UI.SPACING
        graphics.modText(font, Component.literal("Config"), subtitleX, (titleY + (font.lineHeight * TITLE_SCALE - font.lineHeight)).toInt(), Common.UI.TEXT_DIM_COLOR)

        search.render(graphics)

        graphics.drawButtonPanel(closeLeft, closeTop, closeLeft + CLOSE_SIZE, closeTop + CLOSE_SIZE, overClose(mouseX.toDouble(), mouseY.toDouble()))
        graphics.drawLine(closeLeft + CLOSE_INSET, closeTop + CLOSE_INSET, closeLeft + CLOSE_SIZE - CLOSE_INSET, closeTop + CLOSE_SIZE - CLOSE_INSET, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(closeLeft + CLOSE_SIZE - CLOSE_INSET, closeTop + CLOSE_INSET, closeLeft + CLOSE_INSET, closeTop + CLOSE_SIZE - CLOSE_INSET, 1, Common.UI.TEXT_COLOR)
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
        val version = font.splitMod(Component.literal(VersionChecker.currentVersion()), textWidth)
        version.asReversed().forEach {
            graphics.modText(font, it, rowLeft + SIDE_PAD, lineY, Common.UI.DISABLED_TEXT_COLOR)
            lineY -= font.lineHeight
        }
        VersionChecker.result?.takeIf { it.outdated }?.let { found ->
            lineY -= Common.UI.SPACING
            font.splitMod(Component.literal(found.headline()), textWidth).asReversed().forEach {
                graphics.modText(font, it, rowLeft + SIDE_PAD, lineY, Common.UI.SELECTED_FRAME_COLOR)
                lineY -= font.lineHeight
            }
        }

        restoreBottom = lineY - Common.UI.SPACING
        renderRestoreButton(graphics, mouseX, mouseY)
    }

    private fun renderMain(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(mainLeft, panelsTop, mainRight, panelsBottom)
        // drawn inside the settings panel rather than over the whole screen, so the categories and
        // the header keep their own ground
        if (Customization.backgroundShowsOn(Customization.CONFIG_SCREEN)) {
            ConfigBackground.draw(graphics, clipLeft, clipTop, clipRight, clipBottom)
        }

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
        renderOverlays(graphics, mouseX, contentMouseY, delta)

        graphics.pose().popMatrix()
        graphics.disableScissor()

        graphics.drawScrollBar(clipRight - Common.UI.SCROLLBAR_WIDTH - 1, clipTop, viewHeight, contentHeight, viewHeight, scroll)
    }

    /** Whether the category on screen is the one whose settings decide how the mod looks. */
    private fun showsAppearance(): Boolean = selected.key == Customization.CATEGORY

    /** Where the button's underside sits, settled by the side panel once the version is placed. */
    private var restoreBottom: Int = 0

    private val restoreLeft: Int get() = sideLeft + Common.UI.BORDER_SIZE + SIDE_PAD
    private val restoreWidth: Int get() = sideRight - Common.UI.BORDER_SIZE - SIDE_PAD - restoreLeft
    private val restoreTop: Int get() = restoreBottom - RESTORE_HEIGHT

    private fun overRestore(mouseX: Double, mouseY: Double): Boolean =
        showsAppearance() && inRect(mouseX, mouseY, restoreLeft, restoreTop, restoreWidth, RESTORE_HEIGHT)

    /**
     * The button that puts the appearance settings back. It is drawn from the palette rather than
     * from Common.UI, so the transparency sliders cannot fade the one control that undoes them.
     */
    private fun renderRestoreButton(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!showsAppearance()) return

        val right = restoreLeft + restoreWidth
        val bottom = restoreTop + RESTORE_HEIGHT

        graphics.drawButtonPanel(
            restoreLeft, restoreTop, right, bottom,
            hovered = overRestore(mouseX.toDouble(), mouseY.toDouble()),
            fill = Customization.palette.background or OPAQUE,
            frame = Customization.palette.border or OPAQUE
        )

        val label = Component.literal(ellipsised(font, RESTORE_LABEL, restoreWidth - Common.UI.TEXT_X_PAD * 2))
        graphics.text(
            font,
            label,
            restoreLeft + (restoreWidth - font.width(label)) / 2,
            restoreTop + (RESTORE_HEIGHT - font.lineHeight) / 2,
            Customization.palette.text or OPAQUE,
            false
        )
    }

    // ------------------------------------------------------------------ input

    private fun overMain(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, clipLeft, clipTop, clipRight - clipLeft, clipBottom - clipTop)

    private fun overBar(mouseX: Double, mouseY: Double): Boolean =
        maxScroll > 0 && mouseX.toInt() >= clipRight - Common.UI.SCROLLBAR_WIDTH - 3 && overMain(mouseX, mouseY)

    /** The event moved into content coordinates, which the blocks live in. */
    private fun shifted(event: MouseButtonEvent): MouseButtonEvent = event.at(event.x, event.y + scroll)

    /** The window's coordinates in the units this screen lays out in. */
    private fun scaled(event: MouseButtonEvent): MouseButtonEvent =
        event.at(event.x / drawScale, event.y / drawScale)

    override fun onMouseClicked(rawEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        // the second event of a double click is the same click again; acting on it would undo the first
        if (doubled) return true

        // the window's coordinates are turned into the ones this screen laid itself out in
        val event = scaled(rawEvent)
        mouseHeld = true

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

        if (overClose(event.x, event.y)) {
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
            return super.onMouseClicked(event, doubled)
        }

        // read off the screen rather than the scrolled content, since the button does not scroll
        if (overRestore(event.x, event.y)) {
            Customization.restoreDefaults()
            return true
        }

        val content = shifted(event)
        // an open list takes the click if it lands inside it; anywhere else closes every list and
        // the click goes on to the settings underneath
        if (overlaysMouseClicked(content, doubled)) return true
        if (overlays.isNotEmpty()) closeOverlays()

        var handled = false
        shownBlocks().forEach { if (it.mouseClicked(content, doubled)) handled = true }
        return handled
    }

    override fun onMouseReleased(rawEvent: MouseButtonEvent): Boolean {
        val event = scaled(rawEvent)
        mouseHeld = false

        if (draggingBar) {
            draggingBar = false
            return true
        }
        return shownBlocks().any { it.mouseReleased(shifted(event)) }
    }

    override fun onMouseDragged(rawEvent: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val event = scaled(rawEvent)

        if (draggingBar) {
            scroll = (((event.y - clipTop) / viewHeight) * contentHeight - viewHeight / 2).toInt().coerceIn(0, maxScroll)
            return true
        }
        return shownBlocks().any { it.mouseDragged(shifted(event), dragX / drawScale, dragY / drawScale) }
    }

    override fun onMouseMoved(rawX: Double, rawY: Double) {
        val mouseX = rawX / drawScale
        val contentY = rawY / drawScale + scroll

        overlaysMouseMoved(mouseX, contentY)
        shownBlocks().forEach { it.mouseMoved(mouseX, contentY) }
    }

    override fun onMouseScrolled(rawX: Double, rawY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mouseX = rawX / drawScale
        val mouseY = rawY / drawScale

        if (overDropdown(mouseX, mouseY)) {
            dropdownScroll = stepScroll(dropdownScroll, scrollY, searchResults.size, DROPDOWN_MAX_ROWS)
            return true
        }
        if (!overMain(mouseX, mouseY)) return false

        val contentY = mouseY + scroll
        if (overlaysMouseScrolled(mouseX, contentY, scrollX, scrollY)) return true
        if (shownBlocks().any { it.mouseScrolled(mouseX, contentY, scrollX, scrollY) }) return true

        scroll = (scroll - (scrollY * Common.UI.SCROLL_STEP).toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun onCharTyped(characterEvent: CharacterEvent): Boolean {
        if (search.charTyped(characterEvent)) return true
        if (overlaysCharTyped(characterEvent)) return true
        return shownBlocks().any { it.charTyped(characterEvent) }
    }

    override fun onKeyPressed(keyEvent: KeyEvent): Boolean {
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
                searchResults.firstOrNull()?.let { navigate(it) }
                return true
            }
            if (search.keyPressed(keyEvent)) return true
        }
        if (overlaysKeyPressed(keyEvent)) return true
        if (shownBlocks().any { it.keyPressed(keyEvent) }) return true
        return super.onKeyPressed(keyEvent)
    }

    override fun finishClose() {
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

        /** The close button in the header, and how far its cross sits inside it. */
        const val CLOSE_SIZE: Int = 16
        const val CLOSE_INSET: Int = 5
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

        /** How long a row found by the search stays framed. */
        const val FLASH_MS: Long = 1500

        const val RESTORE_LABEL: String = "Restore Defaults"
        const val RESTORE_HEIGHT: Int = 18

        /** A palette colour drawn at full strength, whatever the transparency settings say. */
        const val OPAQUE: Int = 0xFF000000.toInt()
    }
}
