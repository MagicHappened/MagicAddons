package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.LayoutSlot

/** What a plant in a preset stands for: the target, an ingredient, or nothing. */
enum class MarkOption(private val label: String, val marking: LayoutSlot.Marking?) {
    Target("Target", LayoutSlot.Marking.Target),
    Ingredient("Ingredient", LayoutSlot.Marking.Ingredient),
    None("No mark", null);

    override fun toString(): String = label
}
