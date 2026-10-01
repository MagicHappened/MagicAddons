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
import org.magic.magicaddons.config.ConfigShare
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
import org.magic.magicaddons.ui.background.ScreenBackground
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.ui.widgets.ServerCableIcon
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.easedProgress
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.stepScroll
import org.magic.magicaddons.util.VersionChecker
import org.magic.magicaddons.util.compat.McCompat

class ConfigScreen(val parent: Screen?) : MagicAddonsScreen(Component.literal("Magic Addons Config"), "the config screen"), OverlayContext, ScrollView {

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    private val categories = FeatureManager.categories()

    private var selectedCategory: FeatureManager.Category =
        categories.firstOrNull { it.key == lastCategoryKey } ?: categories.first()

    private val blockByFeature = mutableMapOf<Feature, SettingWidget<Boolean>>()

    private val searchBox = TextField(0, SEARCH_HEIGHT, Component.literal(Common.UI.SEARCH_HINT)).also {
        it.setMaxLength(64)
        it.setResponder { rebuildSearchResults() }
    }

    private class SearchResult(val category: FeatureManager.Category, val feature: Feature, val path: List<SettingNode<*>>) {
        val label: String = path.joinToString(" › ") { it.displayName }
    }

    private var searchResults: List<SearchResult> = emptyList()
    private var isDropdownOpen = false
    private var dropdownOpenedAt = 0L
    private var dropdownScroll = 0

    private var contentScroll = 0
    private var isScrollRestored = false
    private var contentHeight = 0
    private var isDraggingScrollBar = false

    private var navigatedWidget: SettingWidget<*>? = null

    private var headerTop = 0
    private var headerBottom = 0
    private var panelsTop = 0
    private var panelsBottom = 0
    private var sideLeft = 0
    private var sideRight = 0
    private var mainLeft = 0
    private var mainRight = 0

    private val clipLeft: Int get() = mainLeft + Common.UI.BORDER_SIZE
    private val clipRight: Int get() = mainRight - Common.UI.BORDER_SIZE
    private val clipTop: Int get() = panelsTop + Common.UI.BORDER_SIZE
    private val clipBottom: Int get() = panelsBottom - Common.UI.BORDER_SIZE

    private val contentLeft: Int get() = clipLeft + MAIN_PADDING
    private val contentRight: Int get() = clipRight - MAIN_PADDING - Common.UI.SCROLLBAR_WIDTH - 2
    private val contentTop: Int get() = clipTop + MAIN_PADDING

    private val viewHeight: Int get() = clipBottom - clipTop
    private val maxScroll: Int get() = (contentHeight - viewHeight).coerceAtLeast(0)

    override val viewLeft: Int get() = clipLeft
    override val viewRight: Int get() = clipRight
    override val viewTop: Int get() = clipTop + contentScroll
    override val viewBottom: Int get() = clipBottom + contentScroll

    private val closeLeft: Int get() = width - MARGIN - HEADER_PADDING - HEADER_BUTTON_SIZE
    private val headerButtonTop: Int get() = headerTop + (HEADER_HEIGHT - HEADER_BUTTON_SIZE) / 2
    private val exportLeft: Int get() = closeLeft - HEADER_PADDING - HEADER_BUTTON_SIZE
    private val importLeft: Int get() = exportLeft - HEADER_PADDING - HEADER_BUTTON_SIZE

    private class CategoryRow(val category: FeatureManager.Category, val top: Int, val hasDividerAbove: Boolean)

    private var categoryRows: List<CategoryRow> = emptyList()

    private var drawScale: Float = 1f

    private var isMouseHeld: Boolean = false

    private var shareNote: String = ""
    private var shareNoteUntil: Long = 0

    private var restoreButtonBottom: Int = 0

    private val sideButtonLeft: Int get() = sideLeft + Common.UI.BORDER_SIZE + SIDE_PADDING
    private val sideButtonWidth: Int get() = sideRight - Common.UI.BORDER_SIZE - SIDE_PADDING - sideButtonLeft
    private val restoreButtonTop: Int get() = restoreButtonBottom - SIDE_BUTTON_HEIGHT
    private val copyUiButtonTop: Int get() = restoreButtonTop - Common.UI.SPACING - SIDE_BUTTON_HEIGHT
    private val importUiButtonTop: Int get() = copyUiButtonTop - Common.UI.SPACING - SIDE_BUTTON_HEIGHT

    override fun onInit() {
        super.onInit()
        VersionChecker.check()

        drawScale = Customization.uiScale
        width = (width / drawScale).toInt()
        height = (height / drawScale).toInt()

        closeOverlays()
        closeDropdown()
        layoutPanels()

        if (!isScrollRestored) {
            contentScroll = lastScroll
            isScrollRestored = true
        }
    }

    private fun relayoutScreen() {
        val window = minecraft.window

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

        searchBox.width = (width / 3).coerceIn(SEARCH_MIN_WIDTH, SEARCH_MAX_WIDTH)
        searchBox.x = (width - searchBox.width) / 2
        searchBox.y = headerTop + (HEADER_HEIGHT - SEARCH_HEIGHT) / 2

        var rowTop = panelsTop + Common.UI.BORDER_SIZE + Common.UI.SPACING
        var isDividerPlaced = false
        categoryRows = categories.map { category ->
            val isFirstUnrelated = category.isUnrelatedToGame && !isDividerPlaced
            if (isFirstUnrelated) {
                isDividerPlaced = true
                rowTop += Common.UI.SPACING * 2 + THICK_DIVIDER_HEIGHT
            }
            CategoryRow(category, rowTop, isFirstUnrelated).also { rowTop += CATEGORY_ROW_HEIGHT }
        }
    }

    private fun blockFor(feature: Feature): SettingWidget<Boolean> =
        blockByFeature.getOrPut(feature) { BooleanSettingWidget(feature.baseSetting, this) }

    private fun shownBlocks(): List<SettingWidget<Boolean>> = selectedCategory.features.map { blockFor(it) }

    private fun layoutBlocks() {
        val blockWidth = contentRight - contentLeft
        var currentY = contentTop
        shownBlocks().forEach { block ->
            val blockInnerHeight = block.layoutTree(contentLeft + Common.UI.BORDER_SIZE, currentY + Common.UI.BORDER_SIZE, blockWidth - Common.UI.BORDER_SIZE * 2)
            currentY += blockInnerHeight + Common.UI.BORDER_SIZE * 2 + BLOCK_GAP
        }
        contentHeight = currentY - BLOCK_GAP + MAIN_PADDING - contentTop

        navigatedWidget?.let { widget ->
            contentScroll = widget.y - contentTop - Common.UI.SPACING
            navigatedWidget = null
        }
        contentScroll = contentScroll.coerceIn(0, maxScroll)
    }

    private fun flashAndScrollTo(widget: SettingWidget<*>) {
        widget.flashUntil = System.currentTimeMillis() + NAVIGATION_FLASH_MS
        navigatedWidget = widget
    }

    fun showSetting(feature: Feature, path: List<SettingNode<*>>) {
        selectedCategory = categories.firstOrNull { feature in it.features } ?: return
        blockFor(feature).revealPath(path)?.let { flashAndScrollTo(it) }
    }

    fun showFeature(feature: Feature) {
        selectedCategory = categories.firstOrNull { feature in it.features } ?: return
        val block = blockFor(feature)
        block.unfold(true)
        flashAndScrollTo(block)
    }

    private fun selectCategory(category: FeatureManager.Category) {
        if (category == selectedCategory) return
        shownBlocks().forEach { it.dropFocus() }
        closeOverlays()
        selectedCategory = category
        contentScroll = 0
    }

    private fun rebuildSearchResults() {
        val query = searchBox.value.trim()
        if (query.isEmpty()) {
            searchResults = emptyList()
            closeDropdown()
            return
        }

        val matches = mutableListOf<SearchResult>()
        fun collectMatches(category: FeatureManager.Category, feature: Feature, node: SettingNode<*>, parentPath: List<SettingNode<*>>) {
            val path = parentPath + node
            if (node.displayName.contains(query, ignoreCase = true)) matches.add(SearchResult(category, feature, path))
            val childNodes = node.availableChildren + ((node as? EnumSetting<*>)?.providedChildren ?: emptyList())
            childNodes.forEach { collectMatches(category, feature, it, path) }
        }
        categories.forEach { category ->
            category.features.forEach { feature -> collectMatches(category, feature, feature.baseSetting, emptyList()) }
        }

        searchResults = matches.sortedBy { it.path.size }
        dropdownScroll = 0
        openDropdown()
    }

    private fun openDropdown() {
        if (isDropdownOpen) return
        isDropdownOpen = true
        dropdownOpenedAt = System.currentTimeMillis()
    }

    private fun closeDropdown() {
        isDropdownOpen = false
    }

    private val dropdownWidth: Int get() = (searchBox.width + DROPDOWN_EXTRA_WIDTH).coerceAtMost(width - MARGIN * 2)
    private val dropdownLeft: Int get() = (searchBox.x + searchBox.width / 2 - dropdownWidth / 2).coerceIn(MARGIN, width - MARGIN - dropdownWidth)
    private val dropdownTop: Int get() = searchBox.y + searchBox.height + Common.UI.SPACING_SMALL
    private val dropdownRows: Int get() = searchResults.size.coerceIn(1, DROPDOWN_MAX_ROWS)
    private val dropdownHeight: Int get() = dropdownRows * DROPDOWN_ROW_HEIGHT + Common.UI.BORDER_SIZE * 2

    private fun isOverDropdown(mouseX: Double, mouseY: Double): Boolean =
        isDropdownOpen && inRect(mouseX, mouseY, dropdownLeft, dropdownTop, dropdownWidth, dropdownHeight)

    private fun searchResultAt(mouseX: Double, mouseY: Double): SearchResult? {
        if (!isOverDropdown(mouseX, mouseY)) return null
        val row = (mouseY.toInt() - dropdownTop - Common.UI.BORDER_SIZE) / DROPDOWN_ROW_HEIGHT
        return searchResults.getOrNull(dropdownScroll + row)
    }

    private fun goToSearchResult(result: SearchResult) {
        selectCategory(result.category)
        blockFor(result.feature).revealPath(result.path)?.let { flashAndScrollTo(it) }
        closeDropdown()
        searchBox.isFocused = false
    }

    private fun renderSearchDropdown(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!isDropdownOpen) return
        val left = dropdownLeft
        val top = dropdownTop

        val openProgress = easedProgress(dropdownOpenedAt, DROPDOWN_MS)
        val stillOpening = openProgress < 1f
        if (stillOpening) {
            val shownHeight = kotlin.math.round(dropdownHeight * openProgress).toInt()
            graphics.enableScissor(left, top, left + dropdownWidth, top + shownHeight)
        }
        graphics.drawPanel(left, top, left + dropdownWidth, top + dropdownHeight)

        val hovered = searchResultAt(mouseX.toDouble(), mouseY.toDouble())
        val labelWidthRoom = dropdownWidth - Common.UI.BORDER_SIZE * 2 - Common.UI.TEXT_X_PAD * 2 - Common.UI.SCROLLBAR_WIDTH
        var rowTop = top + Common.UI.BORDER_SIZE

        if (searchResults.isEmpty()) {
            graphics.modText(font, Component.literal("Nothing matches"), left + Common.UI.BORDER_SIZE + Common.UI.TEXT_X_PAD, rowTop + (DROPDOWN_ROW_HEIGHT - font.lineHeight) / 2, Common.UI.DISABLED_TEXT_COLOR)
            if (stillOpening) graphics.disableScissor()
            return
        }

        searchResults.drop(dropdownScroll).take(DROPDOWN_MAX_ROWS).forEach { result ->
            if (result === hovered) graphics.fill(left + Common.UI.BORDER_SIZE, rowTop, left + dropdownWidth - Common.UI.BORDER_SIZE, rowTop + DROPDOWN_ROW_HEIGHT, Common.UI.HOVER_WASH)

            val textY = rowTop + (DROPDOWN_ROW_HEIGHT - font.lineHeight) / 2
            var textX = left + Common.UI.BORDER_SIZE + Common.UI.TEXT_X_PAD
            val prefix = "${result.category.name} › "
            graphics.modText(font, Component.literal(prefix), textX, textY, Common.UI.TEXT_DIM_COLOR)
            textX += font.width(prefix)

            val label = ellipsised(font, result.label, labelWidthRoom - font.width(prefix))
            graphics.modText(font, Component.literal(label), textX, textY, Common.UI.TEXT_COLOR)
            rowTop += DROPDOWN_ROW_HEIGHT
        }

        graphics.drawScrollBar(left + dropdownWidth - Common.UI.BORDER_SIZE - Common.UI.SCROLLBAR_WIDTH, top + Common.UI.BORDER_SIZE, dropdownRows * DROPDOWN_ROW_HEIGHT, searchResults.size, DROPDOWN_MAX_ROWS, dropdownScroll)
        if (stillOpening) graphics.disableScissor()
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (Customization.uiScale != drawScale && !isMouseHeld) {
            relayoutScreen()
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
        renderSettingsPanel(graphics, scaledMouseX, scaledMouseY, delta)
        renderSearchDropdown(graphics, scaledMouseX, scaledMouseY)
        renderShareNote(graphics, scaledMouseX, scaledMouseY)

        graphics.pose().popMatrix()
    }

    private fun renderShareNote(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (System.currentTimeMillis() < shareNoteUntil && shareNote.isNotEmpty()) {
            graphics.drawSimpleTooltip(shareNote, MARGIN + HEADER_PADDING, headerBottom + Common.UI.SPACING)
        }

        if (isOverExportButton(mouseX.toDouble(), mouseY.toDouble())) {
            graphics.drawTooltipAtCursor(EXPORT_SHARE_TOOLTIP, mouseX, mouseY)
        }
        if (isOverImportButton(mouseX.toDouble(), mouseY.toDouble())) {
            graphics.drawTooltipAtCursor(IMPORT_SHARE_TOOLTIP, mouseX, mouseY)
        }
    }

    private fun showShareNote(text: String) {
        shareNote = text
        shareNoteUntil = System.currentTimeMillis() + SHARE_NOTE_MS
    }

    private fun copyConfig(configType: ConfigShare.ConfigType) {
        val copied = ConfigShare.exportConfig(configType)

        showShareNote(if (copied == null) "There is no ${configType.label} to copy" else "Copied your ${configType.label} to the clipboard")
    }

    private fun pasteConfig(configType: ConfigShare.ConfigType) {
        when (val pasted = ConfigShare.importConfig(configType)) {
            is ConfigShare.Pasted.Applied -> {
                showShareNote("Loaded ${pasted.author}'s ${configType.label}, ${pasted.settings} settings")
                relayoutScreen()
            }

            is ConfigShare.Pasted.Failed -> showShareNote(pasted.reason)
        }
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.drawPanel(MARGIN, headerTop, width - MARGIN, headerBottom)

        val titleX = MARGIN + HEADER_PADDING
        val titleY = headerTop + (HEADER_HEIGHT - font.lineHeight * TITLE_SCALE) / 2f
        graphics.pose().pushMatrix()
        graphics.pose().translate(titleX.toFloat(), titleY)
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE)
        graphics.modText(font, Component.literal(Common.MOD_NAME), 0, 0, Common.UI.TEXT_COLOR)
        graphics.pose().popMatrix()

        val subtitleX = titleX + (font.width(Common.MOD_NAME) * TITLE_SCALE).toInt() + Common.UI.SPACING
        graphics.modText(font, Component.literal("Config"), subtitleX, (titleY + (font.lineHeight * TITLE_SCALE - font.lineHeight)).toInt(), Common.UI.TEXT_DIM_COLOR)

        searchBox.render(graphics)

        graphics.drawButtonPanel(closeLeft, headerButtonTop, closeLeft + HEADER_BUTTON_SIZE, headerButtonTop + HEADER_BUTTON_SIZE, isOverCloseButton(mouseX.toDouble(), mouseY.toDouble()))
        graphics.drawLine(closeLeft + HEADER_ICON_INSET, headerButtonTop + HEADER_ICON_INSET, closeLeft + HEADER_BUTTON_SIZE - HEADER_ICON_INSET, headerButtonTop + HEADER_BUTTON_SIZE - HEADER_ICON_INSET, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(closeLeft + HEADER_BUTTON_SIZE - HEADER_ICON_INSET, headerButtonTop + HEADER_ICON_INSET, closeLeft + HEADER_ICON_INSET, headerButtonTop + HEADER_BUTTON_SIZE - HEADER_ICON_INSET, 1, Common.UI.TEXT_COLOR)

        drawArrowButton(graphics, exportLeft, pointsUp = true, hovered = isOverExportButton(mouseX.toDouble(), mouseY.toDouble()))
        drawArrowButton(graphics, importLeft, pointsUp = false, hovered = isOverImportButton(mouseX.toDouble(), mouseY.toDouble()))
    }

    private fun drawArrowButton(graphics: GuiGraphicsExtractor, left: Int, pointsUp: Boolean, hovered: Boolean) {
        graphics.drawButtonPanel(left, headerButtonTop, left + HEADER_BUTTON_SIZE, headerButtonTop + HEADER_BUTTON_SIZE, hovered)

        val middleX = left + HEADER_BUTTON_SIZE / 2
        val headY = if (pointsUp) headerButtonTop + HEADER_ICON_INSET else headerButtonTop + HEADER_BUTTON_SIZE - HEADER_ICON_INSET
        val tailY = if (pointsUp) headerButtonTop + HEADER_BUTTON_SIZE - HEADER_ICON_INSET else headerButtonTop + HEADER_ICON_INSET

        graphics.drawLine(middleX, headY, middleX, tailY, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(middleX - ARROW_HEAD_SIZE, headY + if (pointsUp) ARROW_HEAD_SIZE else -ARROW_HEAD_SIZE, middleX, headY, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(middleX, headY, middleX + ARROW_HEAD_SIZE, headY + if (pointsUp) ARROW_HEAD_SIZE else -ARROW_HEAD_SIZE, 1, Common.UI.TEXT_COLOR)
    }

    private fun renderSidePanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.drawPanel(sideLeft, panelsTop, sideRight, panelsBottom)

        val rowLeft = sideLeft + Common.UI.BORDER_SIZE
        val rowRight = sideRight - Common.UI.BORDER_SIZE

        categoryRows.forEachIndexed { index, row ->
            if (row.hasDividerAbove) {
                val lineTop = row.top - Common.UI.SPACING - THICK_DIVIDER_HEIGHT
                graphics.fill(rowLeft + SIDE_PADDING, lineTop, rowRight - SIDE_PADDING, lineTop + THICK_DIVIDER_HEIGHT, Common.UI.BORDER_COLOR)
            } else if (index > 0) {
                graphics.fill(rowLeft + SIDE_PADDING, row.top, rowRight - SIDE_PADDING, row.top + 1, Common.UI.THIN_DIVIDER_COLOR)
            }

            val isSelected = row.category == selectedCategory
            val isHovered = mouseX in rowLeft until rowRight && mouseY in row.top until row.top + CATEGORY_ROW_HEIGHT
            if (isSelected) {
                graphics.fill(rowLeft, row.top, rowRight, row.top + CATEGORY_ROW_HEIGHT, Common.UI.PRESSED_SHADE)
                graphics.fill(rowLeft, row.top, rowLeft + SELECTED_STRIP_WIDTH, row.top + CATEGORY_ROW_HEIGHT, Common.UI.SELECTED_FRAME_COLOR)
            } else if (isHovered) {
                graphics.fill(rowLeft, row.top, rowRight, row.top + CATEGORY_ROW_HEIGHT, Common.UI.HOVER_WASH)
            }

            graphics.text(
                font,
                Component.literal(row.category.name),
                rowLeft + SIDE_PADDING + SELECTED_STRIP_WIDTH,
                row.top + (CATEGORY_ROW_HEIGHT - font.lineHeight) / 2,
                if (isSelected) Common.UI.TEXT_COLOR else Common.UI.TEXT_DIM_COLOR,
                false
            )
        }

        val textWidth = rowRight - rowLeft - SIDE_PADDING * 2
        var lineY = panelsBottom - Common.UI.BORDER_SIZE - Common.UI.SPACING - font.lineHeight
        val versionLines = font.splitMod(Component.literal(VersionChecker.currentVersion()), textWidth)
        versionLines.asReversed().forEach {
            graphics.modText(font, it, rowLeft + SIDE_PADDING, lineY, Common.UI.DISABLED_TEXT_COLOR)
            lineY -= font.lineHeight
        }
        VersionChecker.lastCheck?.takeIf { it.isOutdated }?.let { newerVersion ->
            lineY -= Common.UI.SPACING
            font.splitMod(Component.literal(newerVersion.updateHeadline()), textWidth).asReversed().forEach {
                graphics.modText(font, it, rowLeft + SIDE_PADDING, lineY, Common.UI.SELECTED_FRAME_COLOR)
                lineY -= font.lineHeight
            }
        }

        restoreButtonBottom = lineY - Common.UI.SPACING
        renderCustomizationButtons(graphics, mouseX, mouseY)
    }

    private fun renderSettingsPanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(mainLeft, panelsTop, mainRight, panelsBottom)
        if (Customization.backgroundShowsOn(Customization.CONFIG_SCREEN)) {
            ScreenBackground.drawBackground(graphics, clipLeft, clipTop, clipRight, clipBottom)
        }

        val contentMouseY = mouseY + contentScroll
        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom)
        graphics.pose().pushMatrix()
        graphics.pose().translate(0f, -contentScroll.toFloat())

        shownBlocks().forEach { block ->
            val left = block.x - Common.UI.BORDER_SIZE
            val top = block.y - Common.UI.BORDER_SIZE
            graphics.drawPanel(left, top, left + (contentRight - contentLeft), top + block.totalHeight() + Common.UI.BORDER_SIZE * 2)
            block.render(graphics, mouseX, contentMouseY, delta)
        }
        renderOverlays(graphics, mouseX, contentMouseY, delta)

        graphics.pose().popMatrix()
        graphics.disableScissor()

        val isMouseInsidePanel = mouseX in clipLeft until clipRight && mouseY in clipTop until clipBottom
        ServerCableIcon.drawTooltipIfHovered(graphics, mouseX, mouseY, isMouseInsidePanel)

        graphics.drawScrollBar(clipRight - Common.UI.SCROLLBAR_WIDTH - 1, clipTop, viewHeight, contentHeight, viewHeight, contentScroll)
    }

    private fun isCustomizationShown(): Boolean = selectedCategory.key == Customization.CATEGORY

    private fun isOverSideButton(mouseX: Double, mouseY: Double, top: Int): Boolean =
        isCustomizationShown() && inRect(mouseX, mouseY, sideButtonLeft, top, sideButtonWidth, SIDE_BUTTON_HEIGHT)

    private fun isOverRestoreButton(mouseX: Double, mouseY: Double): Boolean = isOverSideButton(mouseX, mouseY, restoreButtonTop)
    private fun isOverCopyUiButton(mouseX: Double, mouseY: Double): Boolean = isOverSideButton(mouseX, mouseY, copyUiButtonTop)
    private fun isOverImportUiButton(mouseX: Double, mouseY: Double): Boolean = isOverSideButton(mouseX, mouseY, importUiButtonTop)

    private fun renderCustomizationButtons(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!isCustomizationShown()) return

        drawSidePanelButton(graphics, restoreButtonTop, RESTORE_LABEL, isOverRestoreButton(mouseX.toDouble(), mouseY.toDouble()))
        drawSidePanelButton(graphics, copyUiButtonTop, COPY_UI_LABEL, isOverCopyUiButton(mouseX.toDouble(), mouseY.toDouble()))
        drawSidePanelButton(graphics, importUiButtonTop, IMPORT_UI_LABEL, isOverImportUiButton(mouseX.toDouble(), mouseY.toDouble()))
    }

    private fun drawSidePanelButton(graphics: GuiGraphicsExtractor, top: Int, text: String, hovered: Boolean) {
        graphics.drawButtonPanel(
            sideButtonLeft, top, sideButtonLeft + sideButtonWidth, top + SIDE_BUTTON_HEIGHT,
            hovered = hovered,
            fill = Customization.palette.background or Common.UI.OPAQUE_ALPHA,
            frame = Customization.palette.border or Common.UI.OPAQUE_ALPHA
        )

        val label = Component.literal(ellipsised(font, text, sideButtonWidth - Common.UI.TEXT_X_PAD * 2))
        graphics.text(
            font,
            label,
            sideButtonLeft + (sideButtonWidth - font.width(label)) / 2,
            top + (SIDE_BUTTON_HEIGHT - font.lineHeight) / 2,
            Customization.palette.text or Common.UI.OPAQUE_ALPHA,
            false
        )
    }

    private fun isOverCloseButton(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, closeLeft, headerButtonTop, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE)

    private fun isOverExportButton(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, exportLeft, headerButtonTop, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE)

    private fun isOverImportButton(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, importLeft, headerButtonTop, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE)

    private fun isOverSettingsView(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, clipLeft, clipTop, clipRight - clipLeft, clipBottom - clipTop)

    private fun isOverScrollBar(mouseX: Double, mouseY: Double): Boolean =
        maxScroll > 0 && mouseX.toInt() >= clipRight - Common.UI.SCROLLBAR_WIDTH - 3 && isOverSettingsView(mouseX, mouseY)

    private fun inContentCoordinates(event: MouseButtonEvent): MouseButtonEvent = event.at(event.x, event.y + contentScroll)

    private fun inLayoutUnits(event: MouseButtonEvent): MouseButtonEvent =
        event.at(event.x / drawScale, event.y / drawScale)

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val scaledEvent = inLayoutUnits(event)
        isMouseHeld = true

        searchResultAt(scaledEvent.x, scaledEvent.y)?.let {
            goToSearchResult(it)
            return true
        }
        if (isOverDropdown(scaledEvent.x, scaledEvent.y)) return true

        if (searchBox.mouseClicked(scaledEvent, doubled)) {
            if (searchBox.value.isNotBlank()) openDropdown()
            return true
        }
        closeDropdown()

        if (isOverCloseButton(scaledEvent.x, scaledEvent.y)) {
            onClose()
            return true
        }

        if (isOverExportButton(scaledEvent.x, scaledEvent.y)) {
            copyConfig(ConfigShare.ConfigType.Features)
            return true
        }
        if (isOverImportButton(scaledEvent.x, scaledEvent.y)) {
            pasteConfig(ConfigShare.ConfigType.Features)
            return true
        }

        if (isOverRestoreButton(scaledEvent.x, scaledEvent.y)) {
            Customization.restoreDefaults()
            return true
        }
        if (isOverCopyUiButton(scaledEvent.x, scaledEvent.y)) {
            copyConfig(ConfigShare.ConfigType.Ui)
            return true
        }
        if (isOverImportUiButton(scaledEvent.x, scaledEvent.y)) {
            pasteConfig(ConfigShare.ConfigType.Ui)
            return true
        }

        categoryRows.firstOrNull {
            scaledEvent.x.toInt() in sideLeft until sideRight && scaledEvent.y.toInt() in it.top until it.top + CATEGORY_ROW_HEIGHT
        }?.let {
            selectCategory(it.category)
            return true
        }

        if (scaledEvent.button() == 0 && isOverScrollBar(scaledEvent.x, scaledEvent.y)) {
            isDraggingScrollBar = true
            return true
        }

        if (!isOverSettingsView(scaledEvent.x, scaledEvent.y)) {
            shownBlocks().forEach { it.dropFocus() }
            return super.onMouseClicked(scaledEvent, doubled)
        }

        val contentEvent = inContentCoordinates(scaledEvent)
        if (overlaysMouseClicked(contentEvent, doubled)) return true
        if (overlays.isNotEmpty()) closeOverlays()

        var handled = false
        shownBlocks().forEach { if (it.mouseClicked(contentEvent, doubled)) handled = true }
        return handled
    }

    override fun onMouseReleased(event: MouseButtonEvent): Boolean {
        val scaledEvent = inLayoutUnits(event)
        isMouseHeld = false

        if (isDraggingScrollBar) {
            isDraggingScrollBar = false
            return true
        }
        val contentEvent = inContentCoordinates(scaledEvent)
        var handled = false
        shownBlocks().forEach { if (it.mouseReleased(contentEvent)) handled = true }
        return handled
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val scaledEvent = inLayoutUnits(event)

        if (isDraggingScrollBar) {
            contentScroll = (((scaledEvent.y - clipTop) / viewHeight) * contentHeight - viewHeight / 2).toInt().coerceIn(0, maxScroll)
            return true
        }
        return shownBlocks().any { it.mouseDragged(inContentCoordinates(scaledEvent), dragX / drawScale, dragY / drawScale) }
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        val scaledX = mouseX / drawScale
        val contentY = mouseY / drawScale + contentScroll

        overlaysMouseMoved(scaledX, contentY)
        shownBlocks().forEach { it.mouseMoved(scaledX, contentY) }
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val scaledX = mouseX / drawScale
        val scaledY = mouseY / drawScale

        if (isOverDropdown(scaledX, scaledY)) {
            dropdownScroll = stepScroll(dropdownScroll, scrollY, searchResults.size, DROPDOWN_MAX_ROWS)
            return true
        }
        if (!isOverSettingsView(scaledX, scaledY)) return false

        val contentY = scaledY + contentScroll
        if (overlaysMouseScrolled(scaledX, contentY, scrollX, scrollY)) return true
        if (shownBlocks().any { it.mouseScrolled(scaledX, contentY, scrollX, scrollY) }) return true

        contentScroll = (contentScroll - (scrollY * Common.UI.SCROLL_STEP).toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun onCharTyped(event: CharacterEvent): Boolean {
        if (searchBox.charTyped(event)) return true
        if (overlaysCharTyped(event)) return true
        return shownBlocks().any { it.charTyped(event) }
    }

    override fun onKeyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (searchBox.isFocused) {
                searchBox.isFocused = false
                closeDropdown()
                return true
            }
            if (overlays.isNotEmpty()) {
                closeOverlays()
                return true
            }
        }
        if (searchBox.isFocused) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                searchResults.firstOrNull()?.let { goToSearchResult(it) }
                return true
            }
            if (searchBox.keyPressed(event)) return true
        }
        if (overlaysKeyPressed(event)) return true
        if (shownBlocks().any { it.keyPressed(event) }) return true
        return super.onKeyPressed(event)
    }

    override fun onCloseFinished() {
        McCompat.setScreen(parent)
    }

    override fun removed() {
        lastCategoryKey = selectedCategory.key
        lastScroll = contentScroll
        MagicAddonsConfigJsonHandler.save()
    }

    private companion object {
        var lastCategoryKey: String? = null
        var lastScroll: Int = 0

        const val MARGIN: Int = 6
        const val PANEL_GAP: Int = Common.UI.SPACING
        const val HEADER_HEIGHT: Int = 30
        const val HEADER_PADDING: Int = 8
        const val TITLE_SCALE: Float = 1.3f

        const val HEADER_BUTTON_SIZE: Int = 16
        const val HEADER_ICON_INSET: Int = 5
        const val SEARCH_HEIGHT: Int = 16
        const val SEARCH_MIN_WIDTH: Int = 100
        const val SEARCH_MAX_WIDTH: Int = 240

        const val SIDE_MIN_WIDTH: Int = 90
        const val SIDE_MAX_WIDTH: Int = 150
        const val SIDE_PADDING: Int = 6
        const val CATEGORY_ROW_HEIGHT: Int = 18
        const val SELECTED_STRIP_WIDTH: Int = 3
        const val THICK_DIVIDER_HEIGHT: Int = 2

        const val MAIN_PADDING: Int = Common.UI.SPACING_LARGE
        const val BLOCK_GAP: Int = Common.UI.SPACING_LARGE

        const val DROPDOWN_ROW_HEIGHT: Int = 14
        const val DROPDOWN_MAX_ROWS: Int = 8
        const val DROPDOWN_EXTRA_WIDTH: Int = 120
        const val DROPDOWN_MS: Long = 150

        const val NAVIGATION_FLASH_MS: Long = 1500

        const val RESTORE_LABEL: String = "Restore Defaults"
        const val SIDE_BUTTON_HEIGHT: Int = 18

        const val COPY_UI_LABEL: String = "Copy UI Config"
        const val IMPORT_UI_LABEL: String = "Import UI Config"

        const val ARROW_HEAD_SIZE: Int = 3

        const val SHARE_UI_NOTE: String = "\n\n§7§oFor UI import and export, there are buttons above " +
                "the version reference while inside the customization category"

        const val EXPORT_SHARE_TOOLTIP: String = "Export config options (Excludes UI)$SHARE_UI_NOTE"

        const val IMPORT_SHARE_TOOLTIP: String = "Import config options (Excludes UI)$SHARE_UI_NOTE"

        const val SHARE_NOTE_MS: Long = 4000
    }
}
