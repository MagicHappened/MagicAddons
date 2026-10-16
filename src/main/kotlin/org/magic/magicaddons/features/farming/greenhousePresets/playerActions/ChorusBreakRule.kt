package org.magic.magicaddons.features.farming.greenhousePresets.playerActions

enum class ChorusBreakRule(private val label: String) {
    Off("Off"),
    PlannedBreaks("Planned Breaks"),
    AnyYoungChorus("Any Young Chorus");

    override fun toString(): String = label
}
