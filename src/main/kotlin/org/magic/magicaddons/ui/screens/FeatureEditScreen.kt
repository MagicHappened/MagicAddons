package org.magic.magicaddons.ui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.widgets.config.SettingWidget
import org.magic.magicaddons.ui.widgets.config.SettingWidgetFactory
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import org.magic.magicaddons.util.compat.McCompat

class FeatureEditScreen(
    val feature: Feature,
    val parent: Screen?
) : Screen(Component.literal(feature.displayName)), HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    var needsRelayout = false

    val childrenSettings: List<SettingNode<*>> = feature.baseSetting.children
        ?: throw IllegalStateException("Cannot construct a feature edit screen for a feature with no nested settings")

    val baseChildrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    val screenDisplayTitle: String = "Editing ${feature.displayName}"

    val screenPaddingX: Int = 100
    val screenPaddingY: Int = 50

    val settingSpacingX: Int = 20 // setting childs CANNOT be larger than the base

    override fun init() {
        super.init()
        layoutElements()

    }

    fun layoutElements(){
        val count = childrenSettings.size
        if (count == 0) return

        val settingsTotalWidth = width - 2 * screenPaddingX
        val totalSpacing = (count - 1) * settingSpacingX
        val widgetWidth = (settingsTotalWidth - totalSpacing) / count

        childrenSettings.forEachIndexed { index, setting ->
            val widget = SettingWidgetFactory.create(setting).apply {
                requestRelayout = { layoutChildren() }
            }

            val xOffset = index * (widgetWidth + settingSpacingX)

            widget.width = widgetWidth
            widget.x = screenPaddingX + xOffset
            widget.y = screenPaddingY
            widget.baseWidget = true
            widget.layout()
            baseChildrenWidgets.add(widget)
            addRenderableWidget(widget)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)

        graphics.drawMultilineBoxCentered(
            screenDisplayTitle,
            width / 2,
            20
        )

        (hoveredElement as? SettingWidget<*>)?.renderTooltip(graphics, mouseX, mouseY)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, deltaTick)
        }
        this.extractMenuBackground(graphics)
        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if ((focused as? SettingWidget<*>)?.charTyped(characterEvent) == true) return true
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if ((focused as? SettingWidget<*>)?.keyPressed(keyEvent) == true) return true
        return super.keyPressed(keyEvent)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        baseChildrenWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
        hoveredElement = baseChildrenWidgets.firstNotNullOfOrNull { it.hoveredWidget() }
    }


    /**
     * Drag, release and the wheel, passed on the same way clicks are: mouseClicked never defers to
     * the vanilla screen, so no widget would otherwise receive a drag.
     */
    override fun mouseDragged(
        mouseButtonEvent: MouseButtonEvent,
        dragX: Double,
        dragY: Double
    ): Boolean = baseChildrenWidgets.any { it.mouseDragged(mouseButtonEvent, dragX, dragY) } ||
            super.mouseDragged(mouseButtonEvent, dragX, dragY)

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean =
        baseChildrenWidgets.any { it.mouseReleased(mouseButtonEvent) } ||
                super.mouseReleased(mouseButtonEvent)

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double
    ): Boolean = baseChildrenWidgets.any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) } ||
            super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        var handled = false

        // every widget still sees the click so the previously focused one can let the keyboard go,
        // the last one that took it keeps the focus
        baseChildrenWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) {
                setFocused(it)
                handled = true
            }
        }
        return handled
    }

    override fun onClose() {
        parent ?: return
        Minecraft.getInstance().setScreenAndShow(parent)
    }

    override fun removed() {
        MagicAddonsConfigJsonHandler.save()
    }
}