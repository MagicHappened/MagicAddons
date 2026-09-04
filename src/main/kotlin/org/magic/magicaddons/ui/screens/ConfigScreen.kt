package org.magic.magicaddons.ui.screens


import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.ui.widgets.config.ConfigCategoryWidget
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.boxHeight
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import org.magic.magicaddons.util.VersionChecker
import org.magic.magicaddons.util.compat.McCompat

class ConfigScreen(title: Component, val parent: Screen?) : ScrollableScreen(title) {

    val categoryWidgets = mutableListOf<ConfigCategoryWidget>()
    lateinit var categories: MutableMap<String, MutableList<Feature>>

    private val categoryPadding: Int get() = scaled(10)
    private val featurePadding: Int get() = scaled(5)

    val helpText: String = """
        Welcome to MagicAddons!
        Features are togglable by the check mark
        You can toggle in depth settings by right clicking the objects
    """.trimIndent()

    /** The centre of the help box and, under it, of the update line when there is one. */
    private var helpY = 0
    private var noticeY = 0

    private var columnsCenterX = 0

    override var contentWidth: Int = 0
        private set
    override var contentHeight: Int = 0
        private set

    override fun init() {
        super.init()
        MagicAddonsConfigJsonHandler.load()
        VersionChecker.check()
        categories = FeatureManager.features
            .groupBy { it.category }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()

        if (categories.isEmpty()) {
            ChatUtils.sendWithPrefix("Unexpected empty category map. Cannot initialize screen.")
            return
        }

        categoryWidgets.clear()

        categories.forEach { (categoryName, featureList) ->
            categoryWidgets.add(ConfigCategoryWidget(categoryName, featureList))
        }

        layoutColumns()
    }

    /** One column per category on a single row, sharing the width; too many scroll sideways. */
    private fun layoutColumns() {
        val count = categoryWidgets.size
        val minWidth = categoryWidgets.maxOf { it.minWidth() }
        // columns stop growing once every name fits on one line, so wide screens get side space
        val naturalWidth = categoryWidgets.maxOf { it.naturalWidth() }
        val columnWidth = columnWidth(count, categoryPadding, minWidth).coerceAtMost(naturalWidth.coerceAtLeast(minWidth))
        val totalWidth = count * columnWidth + (count - 1) * categoryPadding

        var currentX = columnsStartX(totalWidth)

        // the help box, then the update line when there is one, then the columns
        val helpHeight = boxHeight(helpText)
        helpY = scaled(8) + helpHeight / 2
        var baseY = helpY + helpHeight / 2
        VersionChecker.result?.takeIf { it.outdated }?.let { found ->
            val noticeHeight = boxHeight(found.headline())
            noticeY = baseY + scaled(2) + noticeHeight / 2
            baseY = noticeY + noticeHeight / 2
        }
        baseY += scaled(4)

        columnsCenterX = currentX + totalWidth / 2

        // every feature row shares the tallest height, so the columns line up. The checkbox is as
        // tall as the row and takes width from the text, so the height is settled by repeating
        var rowHeight = 0
        repeat(5) {
            val needed = categoryWidgets.maxOf { it.neededRowHeight(columnWidth, rowHeight) }
            if (needed == rowHeight) return@repeat
            rowHeight = needed
        }

        categoryWidgets.forEach { category ->
            category.init(currentX, baseY, columnWidth, rowHeight, featurePadding)
            currentX += columnWidth + categoryPadding
        }

        contentWidth = extentFor(currentX - categoryPadding, width)
        contentHeight = extentFor(baseY + (categoryWidgets.maxOfOrNull { it.height } ?: 0), height)
        clampScroll()
    }

    override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawMultilineBoxCentered(helpText, columnsCenterX, helpY)

        // whatever the last check found, so opening the config says it as well as chat did
        VersionChecker.result?.takeIf { it.outdated }?.let { found ->
            graphics.drawMultilineBoxCentered(found.headline(), columnsCenterX, noticeY)
        }

        categoryWidgets.forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        if (this.minecraft.level == null) {
            this.extractBackground(graphics, mouseX, mouseY, deltaTick)
        }
        this.extractMenuBackground(graphics)
        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    override fun contentMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        var handled = false
        categoryWidgets.forEach {
            if (it.mouseClicked(event, doubled)) handled = true
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
