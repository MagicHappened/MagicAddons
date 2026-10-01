package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.util.ScreenUtil.splitMod
import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.ServerCableIcon
import org.magic.magicaddons.util.ScreenUtil.inModFont
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.ScreenUtil.easedProgress
import org.magic.magicaddons.util.ScreenUtil.inRect

abstract class SettingWidget<T>(
    val node: SettingNode<T>,
    protected val overlays: OverlayContext
) {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    private var treeHeight: Int = 0

    var isHovered: Boolean = false

    var isExpanded: Boolean = false
        private set

    private var foldStartedAt: Long = 0
    private var opennessAtFoldStart: Float = 0f

    val childWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    var flashUntil: Long = 0

    protected val font get() = Minecraft.getInstance().font

    protected open val controlWidth: Int = 0
    protected open val controlHeight: Int = 0

    open fun childNodes(): List<SettingNode<*>> = node.availableChildren

    fun hasChildren(): Boolean = childNodes().isNotEmpty()

    fun descendantCount(): Int = childNodes().sumOf { 1 + descendantCountOf(it) }

    private fun descendantCountOf(node: SettingNode<*>): Int {
        val childNodes = node.availableChildren + ((node as? EnumSetting<*>)?.providedChildren ?: emptyList())
        return childNodes.sumOf { 1 + descendantCountOf(it) }
    }

    private var nameLines: List<FormattedCharSequence> = emptyList()
    private var descriptionLines: List<FormattedCharSequence> = emptyList()
    private var iconLeftByDescriptionLine: Map<Int, Int> = emptyMap()

    private var chevronLeft = 0
    private var chevronTop = 0
    private var chevronWidth = 0

    private var textAndControlHeight = 0

    private var groupTop = 0
    var shownGroupHeight = 0
        private set

    private fun rightColumnWidth(): Int = maxOf(controlWidth, if (hasChildren()) chevronAndCountWidth() else 0)

    private fun chevronAndCountWidth(): Int = font.width(descendantCount().toString()) + COUNT_GAP + CHEVRON_SIZE

    protected fun textLeft(): Int = x + ROW_PADDING
    protected fun textWidth(): Int = width - ROW_PADDING * 2 - rightColumnWidth().let { if (it > 0) it + ROW_PADDING else 0 }

    private fun plainDescription(): String = node.description.replace("§f", "").replace("§r", "")

    private fun modFontWidth(text: String): Int = font.width(inModFont(Component.literal(text)))

    private fun iconGapText(): String {
        var gap = ICON_GAP_CHARACTER
        while (modFontWidth(gap) < ServerCableIcon.SIZE + ICON_MARGIN * 2 && gap.length < MAX_ICON_GAP_CHARACTERS) gap += ICON_GAP_CHARACTER
        return gap
    }

    private fun plainTextOf(line: FormattedCharSequence): String {
        val text = StringBuilder()
        line.accept { _, _, codePoint ->
            text.appendCodePoint(codePoint)
            true
        }
        return text.toString()
    }

    private fun iconLeftIn(line: FormattedCharSequence, gap: String): Int? {
        val text = plainTextOf(line)
        val gapStart = text.indexOf(gap)
        if (gapStart < 0) return null

        return modFontWidth(text.substring(0, gapStart)) + (modFontWidth(gap) - ServerCableIcon.SIZE) / 2
    }

    protected fun controlLeft(): Int = x + width - ROW_PADDING - controlWidth
    protected fun controlTop(): Int = y + ROW_PADDING

    protected open fun belowTextHeight(): Int = 0

    protected fun belowTextTop(): Int = y + ROW_PADDING + textAndControlHeight + BELOW_TEXT_GAP
    protected fun belowTextLeft(): Int = x + ROW_PADDING
    protected fun belowTextWidth(): Int = width - ROW_PADDING * 2

    protected open val bottomPadding: Int = ROW_PADDING

    open fun onGroupOpened() {}

    private fun detailTop(): Int = belowTextTop() + belowTextHeight().let { if (it > 0) it + Common.UI.SPACING else 0 }

    private fun detailHeight(): Int {
        val detail = node.detail?.invoke() ?: return 0
        return detail.height(font, belowTextWidth()) + Common.UI.SPACING
    }

    private fun groupOpenness(): Float {
        val foldProgress = easedProgress(foldStartedAt, FOLD_MS)
        return if (isExpanded) opennessAtFoldStart + (1f - opennessAtFoldStart) * foldProgress else opennessAtFoldStart * (1f - foldProgress)
    }

    private fun isGroupVisible(): Boolean = childWidgets.isNotEmpty() && (isExpanded || groupOpenness() > 0f)

    private fun groupLeft(): Int = x + GROUP_INDENT

    fun layoutTree(x: Int, y: Int, width: Int): Int {
        this.x = x
        this.y = y
        this.width = width

        val nameIconWidth = if (node.requiresServer) ServerCableIcon.SIZE + ICON_MARGIN else 0
        val iconGap = iconGapText()

        nameLines = font.splitMod(Component.literal(node.displayName), (textWidth() - nameIconWidth).coerceAtLeast(font.width("W")))
        descriptionLines = plainDescription().takeIf { it.isNotBlank() }
            ?.replace(SettingNode.SERVER_ICON_TOKEN, iconGap)
            ?.lines()
            ?.flatMap { font.splitMod(Component.literal(it), textWidth().coerceAtLeast(font.width("W"))) }
            ?: emptyList()
        iconLeftByDescriptionLine = descriptionLines
            .mapIndexedNotNull { index, line -> iconLeftIn(line, iconGap)?.let { index to it } }
            .toMap()

        val textHeight = nameLines.size * font.lineHeight +
                if (descriptionLines.isEmpty()) 0 else Common.UI.SPACING_SMALL + descriptionLines.size * font.lineHeight

        var rightColumnHeight = controlHeight
        if (hasChildren()) {
            chevronWidth = chevronAndCountWidth()
            chevronLeft = x + width - ROW_PADDING - chevronWidth
            chevronTop = y + ROW_PADDING + if (controlHeight > 0) controlHeight + Common.UI.SPACING else 0
            rightColumnHeight += (if (controlHeight > 0) Common.UI.SPACING else 0) + CHEVRON_HEIGHT
        } else {
            chevronWidth = 0
        }
        textAndControlHeight = maxOf(textHeight, rightColumnHeight)

        layoutControl()
        height = ROW_PADDING + textAndControlHeight + belowTextHeight().let { if (it > 0) it + BELOW_TEXT_GAP else 0 } + detailHeight() + bottomPadding

        if (!isGroupVisible()) {
            shownGroupHeight = 0
            treeHeight = height
            return treeHeight
        }

        groupTop = y + height
        var currentY = groupTop + GROUP_FRAME_THICKNESS
        childWidgets.forEachIndexed { index, child ->
            if (index > 0) currentY += ROW_DIVIDER_THICKNESS
            currentY += child.layoutTree(groupLeft() + GROUP_FRAME_THICKNESS, currentY, x + width - groupLeft() - GROUP_FRAME_THICKNESS)
        }
        val fullHeight = currentY - groupTop

        shownGroupHeight = kotlin.math.round(fullHeight * groupOpenness()).toInt()
        treeHeight = height + shownGroupHeight
        return treeHeight
    }

    fun totalHeight(): Int = treeHeight

    protected open fun layoutControl() {}

    protected open fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    protected open fun renderBelowText(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (shownGroupHeight > 0) graphics.fill(x, y, x + width, y + height, Common.UI.GROUP_SHADE)
        if (isHovered) graphics.fill(x, y, x + width, y + height, Common.UI.HOVER_WASH)
        if (System.currentTimeMillis() < flashUntil) {
            graphics.drawBorder(x, y, x + width, y + height, 1, Common.UI.SELECTED_FRAME_COLOR)
        }

        var textY = y + ROW_PADDING
        nameLines.forEach {
            graphics.modText(font, it, textLeft(), textY, Common.UI.TEXT_COLOR)
            textY += font.lineHeight
        }
        if (node.requiresServer && nameLines.isNotEmpty()) {
            ServerCableIcon.draw(graphics, textLeft() + font.width(nameLines.last()) + ICON_MARGIN, textY - font.lineHeight + ICON_RISE, mouseX, mouseY)
        }
        textY += Common.UI.SPACING_SMALL
        descriptionLines.forEachIndexed { index, line ->
            graphics.modText(font, line, textLeft(), textY, Common.UI.TEXT_DIM_COLOR)
            iconLeftByDescriptionLine[index]?.let { iconLeft -> ServerCableIcon.draw(graphics, textLeft() + iconLeft, textY + ICON_RISE, mouseX, mouseY) }
            textY += font.lineHeight
        }

        renderControl(graphics, mouseX, mouseY, delta)
        if (hasChildren()) renderChevron(graphics, mouseX, mouseY)
        renderBelowText(graphics, mouseX, mouseY, delta)

        node.detail?.invoke()?.render(graphics, font, belowTextLeft(), detailTop(), belowTextWidth())

        if (shownGroupHeight > 0) renderGroup(graphics, mouseX, mouseY, delta)
    }

    private fun renderGroup(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val left = groupLeft()
        val right = x + width
        val bottom = groupTop + shownGroupHeight

        graphics.enableScissor(x, groupTop, right, bottom)
        graphics.fill(left, groupTop, right, bottom, Common.UI.GROUP_SHADE)
        childWidgets.forEachIndexed { index, child ->
            if (index > 0) {
                val rowAbove = childWidgets[index - 1]
                if (rowAbove.shownGroupHeight > 0) {
                    graphics.fill(left + GROUP_FRAME_THICKNESS, child.y - ROW_DIVIDER_THICKNESS, right, child.y, Common.UI.BORDER_COLOR)
                } else {
                    graphics.fill(left + GROUP_FRAME_THICKNESS, child.y - ROW_DIVIDER_THICKNESS, right, child.y, Common.UI.THIN_DIVIDER_COLOR)
                }
            }
            child.render(graphics, mouseX, mouseY, delta)
        }
        graphics.fill(left + GROUP_FRAME_THICKNESS, groupTop, right, groupTop + GROUP_FRAME_THICKNESS, Common.UI.THIN_DIVIDER_COLOR)
        graphics.fill(left, groupTop, left + GROUP_FRAME_THICKNESS, bottom, Common.UI.BORDER_COLOR)
        graphics.disableScissor()
    }

    private fun renderChevron(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val color = if (isOverChevron(mouseX.toDouble(), mouseY.toDouble())) Common.UI.SELECTED_FRAME_COLOR else Common.UI.TEXT_DIM_COLOR

        graphics.modText(font, Component.literal(descendantCount().toString()), chevronLeft, chevronTop + (CHEVRON_HEIGHT - font.lineHeight) / 2 + 1, color)

        val left = chevronLeft + chevronWidth - CHEVRON_SIZE
        val midX = left + CHEVRON_SIZE / 2
        val midY = chevronTop + CHEVRON_HEIGHT / 2
        val chevronTipOffset = kotlin.math.round(CHEVRON_SIZE / 4 * (groupOpenness() * 2f - 1f)).toInt()
        graphics.drawLine(left, midY - chevronTipOffset, midX, midY + chevronTipOffset, 1, color)
        graphics.drawLine(midX, midY + chevronTipOffset, left + CHEVRON_SIZE, midY - chevronTipOffset, 1, color)
    }

    private fun isOverChevron(mouseX: Double, mouseY: Double): Boolean =
        chevronWidth > 0 && mouseX.toInt() in chevronLeft - COUNT_GAP until chevronLeft + chevronWidth + COUNT_GAP &&
                mouseY.toInt() in chevronTop until chevronTop + CHEVRON_HEIGHT

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, width, height)

    protected fun buildChildWidgets() {
        childWidgets.forEach { it.dropFocus() }
        childWidgets.clear()
        childNodes().forEach { childWidgets.add(SettingWidgetFactory.create(it, overlays)) }
        if (isExpanded) childWidgets.forEach { it.onGroupOpened() }
    }

    fun unfold(shouldOpen: Boolean) {
        if (!hasChildren() || shouldOpen == isExpanded) return
        if (shouldOpen && childWidgets.isEmpty()) buildChildWidgets()
        if (shouldOpen) {
            childWidgets.forEach { it.onGroupOpened() }
        } else {
            childWidgets.forEach { it.dropFocus() }
            overlays.closeOverlays()
        }
        opennessAtFoldStart = groupOpenness()
        foldStartedAt = System.currentTimeMillis()
        isExpanded = shouldOpen
    }

    fun revealPath(path: List<SettingNode<*>>): SettingWidget<*>? {
        if (path.firstOrNull() !== node) return null
        if (path.size == 1) return this
        unfold(true)
        return childWidgets.firstOrNull { it.node === path[1] }?.revealPath(path.drop(1))
    }

    protected abstract fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean

    open fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        var handled = false
        if (isExpanded) childWidgets.forEach { if (it.mouseClicked(event, doubled)) handled = true }
        if (handled) return true

        if (hasChildren() && (isOverChevron(event.x, event.y) || (event.button() == 1 && isMouseOver(event.x, event.y)))) {
            unfold(!isExpanded)
            return true
        }
        return controlClicked(event, doubled)
    }

    open fun mouseMoved(mouseX: Double, mouseY: Double) {
        isHovered = isMouseOver(mouseX, mouseY)
        if (isExpanded) childWidgets.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    open fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean =
        isExpanded && childWidgets.any { it.mouseDragged(event, dragX, dragY) }

    open fun mouseReleased(event: MouseButtonEvent): Boolean {
        var handled = false
        childWidgets.forEach { if (it.mouseReleased(event)) handled = true }
        return handled
    }

    open fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        isExpanded && childWidgets.any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) }

    open fun dropFocus() {
        childWidgets.forEach { it.dropFocus() }
    }

    open fun charTyped(event: CharacterEvent): Boolean =
        isExpanded && childWidgets.any { it.charTyped(event) }

    open fun keyPressed(event: KeyEvent): Boolean =
        isExpanded && childWidgets.any { it.keyPressed(event) }

    override fun toString(): String = "${node.displayName}: ${node.value}"

    companion object {
        const val ROW_PADDING: Int = 6
        const val GROUP_INDENT: Int = 10
        const val BELOW_TEXT_GAP: Int = 7

        const val GROUP_FRAME_THICKNESS: Int = 1
        const val ROW_DIVIDER_THICKNESS: Int = 1

        private const val CHEVRON_SIZE: Int = 8
        private const val CHEVRON_HEIGHT: Int = 12
        private const val COUNT_GAP: Int = 3

        const val FIELD_HEIGHT: Int = 14

        private const val ICON_GAP_CHARACTER: String = "\u00A0"
        private const val MAX_ICON_GAP_CHARACTERS: Int = 8
        private const val ICON_MARGIN: Int = 2
        private const val ICON_RISE: Int = -1

        const val FOLD_MS: Long = 180
    }
}
