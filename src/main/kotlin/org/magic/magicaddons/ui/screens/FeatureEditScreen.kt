package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.config.SettingWidget
import org.magic.magicaddons.ui.widgets.config.SettingWidgetFactory
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ScreenUtil.boxHeight
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import org.magic.magicaddons.util.compat.McCompat

class FeatureEditScreen(
    val feature: Feature,
    val parent: Screen?
) : ScrollableScreen(Component.literal(feature.displayName)), OverlayContext {
    var hoveredWidget: SettingWidget<*>? = null

    /** Open lists and menus, drawn over the settings and offered every input first. */
    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    var needsRelayout = false

    val childrenSettings: List<SettingNode<*>> = feature.baseSetting.children
        ?: throw IllegalStateException("Cannot construct a feature edit screen for a feature with no nested settings")

    val baseChildrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    val screenDisplayTitle: String = "Editing ${feature.displayName}"

    /** The centre of the title box; the settings start under it. */
    private var titleY = 0
    private var settingsTop = 0

    private val settingSpacingX: Int get() = scaled(10) // setting childs CANNOT be larger than the base

    /** A column is never squeezed below this, however many settings share the screen. */
    private val columnMinWidth: Int = 120

    private var columnsCenterX = 0
    private var columnsRight = 0

    override var contentWidth: Int = 0
        private set

    /** Measured every time: expanded children and open lists change how far the content reaches. */
    override val contentHeight: Int
        get() {
            val settingsBottom = baseChildrenWidgets.maxOfOrNull { it.y + it.getTotalHeight() } ?: 0
            val overlaysBottom = overlays.maxOfOrNull { it.overlayY + it.overlayHeight } ?: 0
            return extentFor(maxOf(settingsBottom, overlaysBottom), height)
        }

    override fun init() {
        super.init()
        layoutElements()

    }

    fun layoutElements(){
        // init runs again on every resize and gui scale change, and vanilla clears only its own
        // lists: keeping the old widgets here left two trees alive, both taking every click
        baseChildrenWidgets.clear()
        closeOverlays()

        val count = childrenSettings.size
        if (count == 0) return

        val titleHeight = boxHeight(screenDisplayTitle)
        titleY = scaled(6) + titleHeight / 2
        settingsTop = titleY + titleHeight / 2 + scaled(10)

        val widgetWidth = columnWidth(count, settingSpacingX, columnMinWidth)
        val totalWidth = count * widgetWidth + (count - 1) * settingSpacingX
        val startX = columnsStartX(totalWidth)

        columnsCenterX = startX + totalWidth / 2
        columnsRight = startX + totalWidth
        contentWidth = extentFor(columnsRight, width)

        childrenSettings.forEachIndexed { index, setting ->
            val widget = SettingWidgetFactory.create(setting).apply {
                requestRelayout = { layoutChildren() }
            }

            val xOffset = index * (widgetWidth + settingSpacingX)

            widget.width = widgetWidth
            widget.x = startX + xOffset
            widget.y = settingsTop
            widget.baseWidget = true
            widget.layout()
            baseChildrenWidgets.add(widget)
        }
        clampScroll()
    }

    override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawMultilineBoxCentered(
            screenDisplayTitle,
            columnsCenterX,
            titleY
        )

        baseChildrenWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }

        overlays.asReversed().forEach { it.renderOverlay(graphics, mouseX, mouseY, delta) }
    }

    override fun extractFixed(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (overlays.isEmpty()) hoveredWidget?.renderTooltip(graphics, mouseX, mouseY)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, deltaTick)
        }
        this.extractMenuBackground(graphics)
        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (overlays.any { it.charTyped(characterEvent) }) return true

        baseChildrenWidgets.forEach {
            it.charTyped(characterEvent)
        }
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (overlays.any { it.keyPressed(keyEvent) }) return true

        baseChildrenWidgets.forEach {
            it.keyPressed(keyEvent)
        }
        return super.keyPressed(keyEvent)
    }

    override fun contentMouseMoved(mouseX: Double, mouseY: Double) {
        hoveredWidget = null
        overlays.forEach { it.mouseMoved(mouseX, mouseY) }
        baseChildrenWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
    }

    override fun contentMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        overlays.any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) } ||
                baseChildrenWidgets.any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) }

    override fun contentMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        // the second event of a double click is the same click again: acting on it would undo
        // whatever the first one did, so it is swallowed here
        if (doubled) return true

        // an open list takes the click if it lands inside it; anywhere else closes every list and
        // the click then goes on to the settings underneath
        val overlayTook = overlays.toList().any { it.mouseClicked(event, doubled) }
        if (overlayTook) return true
        if (overlays.isNotEmpty()) closeOverlays()

        var handled = false
        baseChildrenWidgets.forEach {
            if (it.mouseClicked(event, doubled))
                handled = true
        }
        return handled
    }

    override fun onClose() {
        McCompat.setScreen(parent)
    }

    override fun removed() {
        MagicAddonsConfigJsonHandler.save()
    }
}
