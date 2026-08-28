package org.magic.magicaddons.data.greenhouse

/**
 * What the definitions know and what they are missing, crop by crop.
 *
 * Coverage is read straight off the stage ranges. Whether a recorded stage still needs a
 * normalized re-export cannot be seen in the data itself, so that part is learned by watching:
 * every match that only succeeded through the legacy rotation fallback is noted here, and the dex
 * reports the union of what has been seen this session.
 */
object PlantDex {

    /** Stage numbers per crop seen matching only through the legacy rotation fallback. */
    private val legacySeen: MutableMap<String, MutableSet<Int>> = mutableMapOf()

    fun noteLegacy(cropName: String, range: IntRange) {
        legacySeen.getOrPut(cropName) { mutableSetOf() }.addAll(range)
    }

    /** Reading order of the dex: base crops first, then mutations by rarity, then the rest. */
    private val TIER_NAMES = listOf(
        "base crops", "common mutations", "uncommon mutations", "rare mutations",
        "epic mutations", "legendary mutations", "rare crops", "other"
    )

    class Report(val recorded: Int, val total: Int, val incompleteCrops: Int, val missingList: String) {
        val percent: Int get() = if (total == 0) 100 else recorded * 100 / total
    }

    fun report(): Report {
        var total = 0
        var recorded = 0
        var incomplete = 0
        val sections = LinkedHashMap<Int, MutableList<String>>()

        CropRegistry.all
            .sortedWith(compareBy({ CropRegistry.tierOf[it] ?: 7 }, { it.name }))
            .forEach { def ->
                val covered = def.stageDefs.flatMap { it.stageRange }.toSet()
                val missing = (1..def.maxStage).filterNot { it in covered }
                val legacy = legacySeen[def.name].orEmpty().filter { it in covered }.sorted()

                total += def.maxStage
                recorded += def.maxStage - missing.size

                if (missing.isEmpty() && legacy.isEmpty()) return@forEach
                incomplete++

                val parts = mutableListOf<String>()
                if (missing.isNotEmpty()) parts += "stages ${ranges(missing)} unrecorded"
                if (legacy.isNotEmpty()) parts += "stages ${ranges(legacy)} need normalization"

                sections.getOrPut(CropRegistry.tierOf[def] ?: 7) { mutableListOf() }
                    .add("${def.name} -> ${parts.joinToString("; ")}")
            }

        val text = buildString {
            sections.forEach { (tier, lines) ->
                appendLine("== ${TIER_NAMES[tier]} ==")
                lines.forEach { appendLine(it) }
                appendLine()
            }
        }.trimEnd()

        return Report(recorded, total, incomplete, text)
    }

    /** 1, 2, 3, 7 written as 1-3, 7, so a crop missing forty stages is one line, not forty. */
    private fun ranges(sorted: List<Int>): String = buildString {
        var i = 0
        while (i < sorted.size) {
            var j = i
            while (j + 1 < sorted.size && sorted[j + 1] == sorted[j] + 1) j++

            if (isNotEmpty()) append(", ")
            append(if (j > i) "${sorted[i]}-${sorted[j]}" else "${sorted[i]}")
            i = j + 1
        }
    }
}
