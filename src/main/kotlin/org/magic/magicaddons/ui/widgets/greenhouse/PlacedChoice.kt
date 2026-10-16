package org.magic.magicaddons.ui.widgets.greenhouse

enum class PlacedChoice(private val label: String, val placed: Boolean?) {
    Off("Placed off", null),
    Mark("Mark placed", true),
    Unmark("Unmark placed", false);

    override fun toString(): String = label
}
