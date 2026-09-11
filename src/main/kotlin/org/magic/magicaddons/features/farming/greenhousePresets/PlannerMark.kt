package org.magic.magicaddons.features.farming.greenhousePresets

/**
 * What a marked block is told to do, and the colour it is told in: the default, and whatever the
 * player has set instead.
 */
enum class PlannerMark(val displayName: String, val defaultColor: Int) {
    Wrong("Wrong Block", 0xFFFF3333.toInt()),
    Adjust("Needs Adjusting", 0xFFFF9922.toInt()),
    Remove("To Remove", 0xFFAA44EE.toInt()),
    Missing("Missing", 0xFF3399FF.toInt()),
    Ready("Harvestable", 0xFF33FF66.toInt()),
    Blocking("Blocking A Target", 0xFFFF3333.toInt());

    val color: Int get() = GreenhousePresets.plannerColor(this)
}
