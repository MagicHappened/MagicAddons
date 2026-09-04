package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu

/** What a plant in a preset stands for: the target, an ingredient, a unique crop, or nothing. */
class MarkContext(
    override val overlayX: Int,
    override val overlayY: Int,
    private val overlayContext: OverlayContext,
    options: List<Option>,
    private val onPick: (LayoutSlot.Marking?) -> Unit
) : AbstractSelectorContextMenu<MarkContext.Option>(options, "Mark as:", withSearch = false) {

    enum class Option(private val label: String, val marking: LayoutSlot.Marking?) {
        Target("Target", LayoutSlot.Marking.Target),
        Ingredient("Ingredient", LayoutSlot.Marking.Ingredient),
        Unique("Unique crop", LayoutSlot.Marking.UniqueCrop),
        None("No mark", null);

        override fun toString(): String = label
    }

    override fun onValueSelected(value: Option) {
        overlayContext.removeOverlay(this)
        onPick(value.marking)
    }
}
