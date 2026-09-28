package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.max

abstract class ListContextMenu<T>(
    x: Int,
    y: Int,
    val values: List<T>,
    private val title: String
) : ContextMenu() {

    final override var overlayX: Int = x
        private set
    final override var overlayY: Int = y
        private set

    override var hoveredElement: GuiEventListener? = null

    protected val font = Minecraft.getInstance().font

    protected open val rowHeight = 20

    protected val rows: MutableList<RowWidget<T>> = mutableListOf()

    override val overlayWidth: Int
        get() {
            val longest = values.maxOfOrNull { font.width(it.toString()) } ?: 0
            return max(longest, font.width(title)) + TEXT_PADDING * 2
        }

    private val titleHeight: Int get() = font.lineHeight + TITLE_PADDING * 2

    protected open val footerHeight: Int = 0

    override val overlayHeight: Int
        get() = titleHeight + rows.sumOf { it.height } + footerHeight

    open fun init() {
        buildRows()

        val (x, y) = OverlayRenderable.placeOnScreen(overlayX, overlayY, overlayWidth, overlayHeight)
        overlayX = x
        overlayY = y
        layoutRows()
    }

    private fun buildRows() {
        rows.clear()
        values.forEach { rows.add(RowWidget(value = it, onClick = { value -> onValueSelected(value) })) }
        rows.lastOrNull()?.hasDividerBelow = false
    }

    private fun layoutRows() {
        var currentY = overlayY + titleHeight

        rows.forEach { row ->
            row.x = overlayX
            row.y = currentY
            row.width = overlayWidth
            row.fitHeight(rowHeight)
            currentY += row.height
        }
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BACKGROUND_COLOR)

        graphics.text(
            font,
            Component.literal(title),
            overlayX + TEXT_PADDING,
            overlayY + TITLE_PADDING,
            Common.UI.TEXT_COLOR,
            false
        )

        val lineY = overlayY + titleHeight - 1
        graphics.fill(overlayX, lineY, overlayX + overlayWidth, lineY + 1, Common.UI.DIVIDER_COLOR)

        rows.forEach { it.extractRenderState(graphics, mouseX, mouseY) }
        if (footerHeight > 0) {
            val footerTop = overlayY + overlayHeight - footerHeight
            graphics.fill(overlayX, footerTop, overlayX + overlayWidth, footerTop + 1, Common.UI.DIVIDER_COLOR)
            renderFooter(graphics, footerTop, mouseX, mouseY)
        }
        graphics.drawBorder(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        rows.toList().forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }
        if (footerHeight > 0 && mouseButtonEvent.y >= overlayY + overlayHeight - footerHeight) onFooterClicked()
        return true
    }

    protected open fun renderFooter(graphics: GuiGraphicsExtractor, footerTop: Int, mouseX: Int, mouseY: Int) {}

    protected open fun onFooterClicked() {}

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        rows.forEach {
            it.mouseMoved(mouseX, mouseY)
            if (hoveredElement == null && it.isMouseOver(mouseX, mouseY)) {
                hoveredElement = it
            }
        }
    }

    abstract fun onValueSelected(value: T)

    private companion object {
        val TEXT_PADDING: Int = Common.UI.TEXT_X_PAD
        val TITLE_PADDING: Int = Common.UI.SPACING
    }
}
