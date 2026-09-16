package org.magic.magicaddons.data.greenhouse

data class SpawnRule(
    val weight: Int,
    val requiredNeighbourCells: Map<String, Int> = emptyMap(),
    val needsNoNeighbours: Boolean = false,
    val needsAllPositiveEffects: Boolean = false
)
