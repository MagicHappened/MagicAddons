package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawLine

/**
 * One setting as a row: its name, the description under it, and the control for its type on the
 * right. Settings under it unfold beneath, indented, behind a chevron that counts them.
 */
abstract class SettingWidget<T>(
    val node: SettingNode<T>,
    protected val overlays: OverlayContext
) {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0

    /** The row alone; the unfolded settings under it are not counted. */
    var height: Int = 0

    var hovered: Boolean = false
    var expanded: Boolean = false

    val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    /** The row is framed amber until this passes, so a search landing here is seen. */
    var flashUntil: Long = 0

    protected val font get() = Minecraft.getInstance().font

    /** The control on the right; zero wide when the type draws itself under the text instead. */
    protected abstract val controlWidth: Int
    protected abstract val controlHeight: Int

    /** The settings the chevron unfolds. */
    open fun childNodes(): List<SettingNode<*>> = node.children.orEmpty()

    fun hasChildren(): Boolean = childNodes().isNotEmpty()

    /** Every setting under this one, at any depth. */
    fun descendantCount(): Int = childNodes().sumOf { 1 + countUnder(it) }

    private fun countUnder(node: SettingNode<*>): Int = node.children.orEmpty().sumOf { 1 + countUnder(it) }

    private var nameLines: List<FormattedCharSequence> = emptyList()
    private var descriptionLines: List<FormattedCharSequence> = emptyList()

    /** Where the chevron button was laid, zero wide when there is nothing to unfold. */
    private var chevronLeft = 0
    private var chevronTop = 0
    private var chevronWidth = 0

    /** The text, the control and the chevron side by side; the extra part goes under them. */
    private var topHeight = 0

    private fun rightColumnWidth(): Int = maxOf(controlWidth, if (hasChildren()) chevronWidthFor() else 0)

    private fun chevronWidthFor(): Int = font.width(descendantCount().toString()) + CHEVRON_SIZE + TEXT_GAP * 3

    protected fun textLeft(): Int = x + ROW_PAD
    protected fun textWidth(): Int = width - ROW_PAD * 2 - rightColumnWidth().let { if (it > 0) it + ROW_PAD else 0 }

    /** The description, the codes for plain text dropped so it stays in its own quiet colour. */
    private fun description(): String = node.tooltip.replace("§f", "").replace("§r", "")

    /** Where the control's top left goes: on the right, level with the name. */
    protected fun controlLeft(): Int = x + width - ROW_PAD - controlWidth
    protected fun controlTop(): Int = y + ROW_PAD

    /** Whatever the type draws under the text and control, given the row width. Zero when nothing. */
    protected open fun extraHeight(): Int = 0

    protected fun extraTop(): Int = y + ROW_PAD + topHeight
    protected fun extraLeft(): Int = x + ROW_PAD
    protected fun extraWidth(): Int = width - ROW_PAD * 2

    private fun detailTop(): Int = extraTop() + extraHeight().let { if (it > 0) it + Common.UI.SPACING else 0 }

    private fun detailHeight(): Int {
        val detail = node.detail?.invoke() ?: return 0
        return detail.height(font, extraWidth()) + Common.UI.SPACING
    }

    /** Lays the row and, unfolded, everything under it. Returns the height of it all. */
    fun layoutTree(x: Int, y: Int, width: Int): Int {
        this.x = x
        this.y = y
        this.width = width

        nameLines = font.split(Component.literal(node.displayName), textWidth().coerceAtLeast(font.width("W")))
        descriptionLines = description().takeIf { it.isNotBlank() }
            ?.lines()
            ?.flatMap { font.split(Component.literal(it), textWidth().coerceAtLeast(font.width("W"))) }
            ?: emptyList()

        val textHeight = nameLines.size * font.lineHeight +
                if (descriptionLines.isEmpty()) 0 else Common.UI.SPACING_SMALL + descriptionLines.size * font.lineHeight

        var rightHeight = controlHeight
        if (hasChildren()) {
            chevronWidth = chevronWidthFor()
            chevronLeft = x + width - ROW_PAD - chevronWidth
            chevronTop = y + ROW_PAD + if (controlHeight > 0) controlHeight + Common.UI.SPACING else 0
            rightHeight += (if (controlHeight > 0) Common.UI.SPACING else 0) + CHEVRON_HEIGHT
        } else {
            chevronWidth = 0
        }
        topHeight = maxOf(textHeight, rightHeight)

        layoutControl()
        height = ROW_PAD + topHeight + extraHeight().let { if (it > 0) it + Common.UI.SPACING else 0 } + detailHeight() + ROW_PAD

        if (!expanded) return height

        var currentY = y + height + Common.UI.SPACING
        childrenWidgets.forEach {
            currentY += it.layoutTree(x + INDENT, currentY, width - INDENT) + Common.UI.SPACING
        }
        return currentY - Common.UI.SPACING - y
    }

    /** The row and everything unfolded under it, as last laid out. */
    fun totalHeight(): Int {
        if (!expanded || childrenWidgets.isEmpty()) return height
        val last = childrenWidgets.last()
        return last.y + last.totalHeight() - y
    }

    /** Puts the control at [controlLeft], [controlTop] once the row's width is known. */
    protected open fun layoutControl() {}

    protected abstract fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)

    /** Draws the part under the text, when the type has one. */
    protected open fun renderExtra(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (hovered) graphics.fill(x, y, x + width, y + height, Common.UI.HOVER_WASH)
        if (System.currentTimeMillis() < flashUntil) {
            graphics.drawBorder(x, y, x + width, y + height, 1, Common.UI.SELECTED_FRAME_COLOR)
        }

        var textY = y + ROW_PAD
        nameLines.forEach {
            graphics.text(font, it, textLeft(), textY, Common.UI.TEXT_COLOR, false)
            textY += font.lineHeight
        }
        textY += Common.UI.SPACING_SMALL
        descriptionLines.forEach {
            graphics.text(font, it, textLeft(), textY, Common.UI.TEXT_DIM_COLOR, false)
            textY += font.lineHeight
        }

        renderControl(graphics, mouseX, mouseY, delta)
        if (hasChildren()) renderChevron(graphics, mouseX, mouseY)
        renderExtra(graphics, mouseX, mouseY, delta)

        node.detail?.invoke()?.render(graphics, font, extraLeft(), detailTop(), extraWidth())

        if (!expanded || childrenWidgets.isEmpty()) return

        // a line down the indent ties the unfolded settings to their row
        val guideX = x + INDENT / 2
        graphics.fill(guideX, y + height + Common.UI.SPACING, guideX + 1, y + totalHeight(), Common.UI.THIN_DIVIDER_COLOR)
        childrenWidgets.forEach { it.render(graphics, mouseX, mouseY, delta) }
    }

    /** A small button: how many settings are under this row, and a chevron pointing where they open. */
    private fun renderChevron(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val over = overChevron(mouseX.toDouble(), mouseY.toDouble())
        graphics.drawButtonPanel(chevronLeft, chevronTop, chevronLeft + chevronWidth, chevronTop + CHEVRON_HEIGHT, over, pressed = expanded)

        val count = descendantCount().toString()
        graphics.text(font, Component.literal(count), chevronLeft + TEXT_GAP, chevronTop + (CHEVRON_HEIGHT - font.lineHeight) / 2 + 1, Common.UI.TEXT_DIM_COLOR, false)

        val left = chevronLeft + chevronWidth - TEXT_GAP - CHEVRON_SIZE
        val midX = left + CHEVRON_SIZE / 2
        val midY = chevronTop + CHEVRON_HEIGHT / 2
        val half = CHEVRON_SIZE / 4
        if (expanded) {
            graphics.drawLine(left, midY + half, midX, midY - half, 1, Common.UI.TEXT_COLOR)
            graphics.drawLine(midX, midY - half, left + CHEVRON_SIZE, midY + half, 1, Common.UI.TEXT_COLOR)
        } else {
            graphics.drawLine(left, midY - half, midX, midY + half, 1, Common.UI.TEXT_COLOR)
            graphics.drawLine(midX, midY + half, left + CHEVRON_SIZE, midY - half, 1, Common.UI.TEXT_COLOR)
        }
    }

    private fun overChevron(mouseX: Double, mouseY: Double): Boolean =
        chevronWidth > 0 && mouseX.toInt() in chevronLeft until chevronLeft + chevronWidth &&
                mouseY.toInt() in chevronTop until chevronTop + CHEVRON_HEIGHT

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    /** Builds the widgets under this row, so an enum can swap them when its value changes. */
    protected fun buildChildren() {
        childrenWidgets.clear()
        childNodes().forEach { childrenWidgets.add(SettingWidgetFactory.create(it, overlays)) }
    }

    fun unfold(open: Boolean) {
        if (!hasChildren()) return
        if (open && childrenWidgets.isEmpty()) buildChildren()
        if (!open && expanded) overlays.closeOverlays()
        expanded = open
    }

    /** Unfolds along [path], which starts at this row's node, and returns the widget it ends at. */
    fun reveal(path: List<SettingNode<*>>): SettingWidget<*>? {
        if (path.firstOrNull() !== node) return null
        if (path.size == 1) return this
        unfold(true)
        return childrenWidgets.firstOrNull { it.node === path[1] }?.reveal(path.drop(1))
    }

    /** Whether the click landed on the control; the row itself takes the chevron and the right click. */
    protected abstract fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean

    /**
     * Every row under this one sees the click too, since a text field lets the keyboard go when a
     * click lands elsewhere. The chevron opens and closes, as does a right click on the row.
     */
    open fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        var handled = false
        if (expanded) childrenWidgets.forEach { if (it.mouseClicked(event, doubled)) handled = true }
        if (handled) return true

        if (controlClicked(event, doubled)) return true

        if (hasChildren() && (overChevron(event.x, event.y) || (event.button() == 1 && isMouseOver(event.x, event.y)))) {
            unfold(!expanded)
            return true
        }
        return false
    }

    open fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
        if (expanded) childrenWidgets.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    open fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean =
        expanded && childrenWidgets.any { it.mouseDragged(event, dragX, dragY) }

    open fun mouseReleased(event: MouseButtonEvent): Boolean =
        expanded && childrenWidgets.any { it.mouseReleased(event) }

    open fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        expanded && childrenWidgets.any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) }

    /** Lets go of the keyboard, this row and every row under it. */
    open fun dropFocus() {
        childrenWidgets.forEach { it.dropFocus() }
    }

    open fun charTyped(event: CharacterEvent): Boolean =
        expanded && childrenWidgets.any { it.charTyped(event) }

    open fun keyPressed(event: KeyEvent): Boolean =
        expanded && childrenWidgets.any { it.keyPressed(event) }

    override fun toString(): String = "${node.displayName}: ${node.value}"

    companion object {
        /** Room between a row's edge and what it holds. */
        const val ROW_PAD: Int = 6

        /** How far the settings under a row are pushed in. */
        const val INDENT: Int = 12

        const val CHEVRON_SIZE: Int = 8
        const val CHEVRON_HEIGHT: Int = 12
        private const val TEXT_GAP: Int = 3

        /** A control of one text line, the height of a small field. */
        const val FIELD_HEIGHT: Int = 14
    }
}
