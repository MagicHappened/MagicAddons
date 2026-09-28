package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot

enum class MarkChoice(private val label: String, val marking: LayoutSlot.Marking?, val applies: Boolean) {
    Off("Mark off", null, false),
    Target("Target", LayoutSlot.Marking.Target, true),
    Ingredient("Ingredient", LayoutSlot.Marking.Ingredient, true),
    Clear("Clear mark", null, true);

    override fun toString(): String = label
}
