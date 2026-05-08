package org.magic.magicaddons.data.greenhouse

data class GreenhousePreset(
    val id: Int, // id number to attach to layout id (layout id is string) for preset => preset_{id}
    val layout: GreenhouseLayout
) {

}