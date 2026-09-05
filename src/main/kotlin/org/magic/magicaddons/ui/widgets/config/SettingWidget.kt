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
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.ScreenUtil.eased

/**
 * One setting as a row: its name, the description under it, and the control for its type on the
 * right. The settings under it unfold beneath in a framed group, each in its own box, behind a
 * chevron that counts them.
 */
abstract class SettingWidget<T>(
    val node: SettingNode<T>,
    protected val overlays: OverlayContext
) {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0

    /** The row alone; the group under it is not counted. */
    var height: Int = 0

    /** The row and the group under it, as far as it has unfolded. */
    private var treeHeight: Int = 0

    var hovered: Boolean = false

    /** Whether the group is open or opening; while closing it is still drawn until it has shrunk away. */
    var expanded: Boolean = false
        private set

    /** When the group last started opening or closing, and how far open it was then. */
    private var foldStartedAt: Long = 0
    private var foldFrom: Float = 0f

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

    /** Where the count and chevron were laid, zero wide when there is nothing to unfold. */
    private var chevronLeft = 0
    private var chevronTop = 0
    private var chevronWidth = 0

    /** The text, the control and the chevron side by side; the extra part goes under them. */
    private var topHeight = 0

    /** The group of unfolded settings: its top, and its height as far as it has opened. */
    private var groupTop = 0
    var groupShown = 0
        private set

    private fun rightColumnWidth(): Int = maxOf(controlWidth, if (hasChildren()) chevronWidthFor() else 0)

    private fun chevronWidthFor(): Int = font.width(descendantCount().toString()) + TEXT_GAP + CHEVRON_SIZE

    protected fun textLeft(): Int = x + ROW_PAD
    protected fun textWidth(): Int = width - ROW_PAD * 2 - rightColumnWidth().let { if (it > 0) it + ROW_PAD else 0 }

    /** The description, the codes for plain text dropped so it stays in its own quiet colour. */
    private fun description(): String = node.description.replace("§f", "").replace("§r", "")

    /** Where the control's top left goes: on the right, level with the name. */
    protected fun controlLeft(): Int = x + width - ROW_PAD - controlWidth
    protected fun controlTop(): Int = y + ROW_PAD

    /** Whatever the type draws under the text and control, given the row width. Zero when nothing. */
    protected open fun extraHeight(): Int = 0

    protected fun extraTop(): Int = y + ROW_PAD + topHeight + EXTRA_GAP

    /** Room under the extra part; a type whose extra part ends on the row's edge has none. */
    protected open val bottomPad: Int = ROW_PAD

    /** Told when the group holding this row opens, for a type that tidies itself when shown. */
    open fun onShown() {}
    protected fun extraLeft(): Int = x + ROW_PAD
    protected fun extraWidth(): Int = width - ROW_PAD * 2

    private fun detailTop(): Int = extraTop() + extraHeight().let { if (it > 0) it + Common.UI.SPACING else 0 }

    private fun detailHeight(): Int {
        val detail = node.detail?.invoke() ?: return 0
        return detail.height(font, extraWidth()) + Common.UI.SPACING
    }

    /** How far open the group is, from closed at zero to open at one, moving after a fold. */
    private fun openness(): Float {
        val moved = eased(foldStartedAt, FOLD_MS)
        return if (expanded) foldFrom + (1f - foldFrom) * moved else foldFrom * (1f - moved)
    }

    /** Whether the group is drawn at all: open, opening, or still closing. */
    private fun groupVisible(): Boolean = childrenWidgets.isNotEmpty() && (expanded || openness() > 0f)

    /** The group starts in from the row and runs to the row's right edge. */
    fun groupLeft(): Int = x + INDENT

    /** Lays the row and, unfolded, the group under it. Returns the height of it all. */
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
        height = ROW_PAD + topHeight + extraHeight().let { if (it > 0) it + EXTRA_GAP else 0 } + detailHeight() + bottomPad

        if (!groupVisible()) {
            groupShown = 0
            treeHeight = height
            return treeHeight
        }

        // the group hangs right under the row: a line across its top, one down its left, and the
        // rows stacked inside with a line between each pair. Whatever follows closes it at the bottom
        groupTop = y + height
        var currentY = groupTop + GROUP_FRAME
        childrenWidgets.forEachIndexed { index, child ->
            if (index > 0) currentY += ROW_LINE
            currentY += child.layoutTree(groupLeft() + GROUP_FRAME, currentY, x + width - groupLeft() - GROUP_FRAME)
        }
        val fullHeight = currentY - groupTop

        groupShown = kotlin.math.round(fullHeight * openness()).toInt()
        treeHeight = height + groupShown
        return treeHeight
    }

    /** The row and the group under it, as last laid out. */
    fun totalHeight(): Int = treeHeight

    /** Puts the control at [controlLeft], [controlTop] once the row's width is known. */
    protected open fun layoutControl() {}

    protected abstract fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)

    /** Draws the part under the text, when the type has one. */
    protected open fun renderExtra(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // an open row shares its group's shade, so the two read as one thing
        if (groupShown > 0) graphics.fill(x, y, x + width, y + height, Common.UI.GROUP_SHADE)
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

        if (groupShown > 0) renderGroup(graphics, mouseX, mouseY, delta)
    }

    /**
     * The unfolded settings under a light line and a line down the indent, a light line between
     * rows and a full width one where a row's group ends. Clipped to how far the group has opened,
     * so it slides.
     */
    private fun renderGroup(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val left = groupLeft()
        val right = x + width
        val bottom = groupTop + groupShown

        graphics.enableScissor(x, groupTop, right, bottom)
        graphics.fill(left, groupTop, right, bottom, Common.UI.GROUP_SHADE)
        childrenWidgets.forEachIndexed { index, child ->
            if (index > 0) {
                val above = childrenWidgets[index - 1]
                if (above.groupShown > 0) {
                    graphics.fill(left + GROUP_FRAME, child.y - ROW_LINE, right, child.y, Common.UI.BORDER_COLOR)
                } else {
                    graphics.fill(left + GROUP_FRAME, child.y - ROW_LINE, right, child.y, Common.UI.THIN_DIVIDER_COLOR)
                }
            }
            child.render(graphics, mouseX, mouseY, delta)
        }
        graphics.fill(left + GROUP_FRAME, groupTop, right, groupTop + GROUP_FRAME, Common.UI.THIN_DIVIDER_COLOR)
        graphics.fill(left, groupTop, left + GROUP_FRAME, bottom, Common.UI.BORDER_COLOR)
        graphics.disableScissor()
    }

    /** The count of settings under this row and a chevron pointing where they open, lit under the mouse. */
    private fun renderChevron(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val color = if (overChevron(mouseX.toDouble(), mouseY.toDouble())) Common.UI.SELECTED_FRAME_COLOR else Common.UI.TEXT_DIM_COLOR

        graphics.text(font, Component.literal(descendantCount().toString()), chevronLeft, chevronTop + (CHEVRON_HEIGHT - font.lineHeight) / 2 + 1, color, false)

        // the chevron turns over as the group opens
        val left = chevronLeft + chevronWidth - CHEVRON_SIZE
        val midX = left + CHEVRON_SIZE / 2
        val midY = chevronTop + CHEVRON_HEIGHT / 2
        val lift = kotlin.math.round(CHEVRON_SIZE / 4 * (openness() * 2f - 1f)).toInt()
        graphics.drawLine(left, midY - lift, midX, midY + lift, 1, color)
        graphics.drawLine(midX, midY + lift, left + CHEVRON_SIZE, midY - lift, 1, color)
    }

    private fun overChevron(mouseX: Double, mouseY: Double): Boolean =
        chevronWidth > 0 && mouseX.toInt() in chevronLeft - TEXT_GAP until chevronLeft + chevronWidth + TEXT_GAP &&
                mouseY.toInt() in chevronTop until chevronTop + CHEVRON_HEIGHT

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    /** Builds the widgets under this row, so an enum can swap them when its value changes. */
    protected fun buildChildren() {
        childrenWidgets.clear()
        childNodes().forEach { childrenWidgets.add(SettingWidgetFactory.create(it, overlays)) }
    }

    fun unfold(open: Boolean) {
        if (!hasChildren() || open == expanded) return
        if (open && childrenWidgets.isEmpty()) buildChildren()
        if (open) childrenWidgets.forEach { it.onShown() }
        if (!open) overlays.closeOverlays()
        foldFrom = openness()
        foldStartedAt = System.currentTimeMillis()
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

        if (hasChildren() && (overChevron(event.x, event.y) || (event.button() == 1 && isMouseOver(event.x, event.y)))) {
            unfold(!expanded)
            return true
        }
        return controlClicked(event, doubled)
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

        /** How far the group under a row is pushed in. */
        const val INDENT: Int = 10

        /** Room between the description and whatever the type draws under it. */
        const val EXTRA_GAP: Int = 7

        /** The group's top and left lines, and the line between its rows. */
        const val GROUP_FRAME: Int = 1
        const val ROW_LINE: Int = 1

        const val CHEVRON_SIZE: Int = 8
        const val CHEVRON_HEIGHT: Int = 12
        private const val TEXT_GAP: Int = 3

        /** A control of one text line, the height of a small field. */
        const val FIELD_HEIGHT: Int = 14

        /** How long a group takes to slide open or shut. */
        const val FOLD_MS: Long = 180
    }
}
