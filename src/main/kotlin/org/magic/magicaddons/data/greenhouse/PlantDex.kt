package org.magic.magicaddons.data.greenhouse

/**
 * What the definitions know and what they are missing, crop by crop. Coverage is read off the stage
 * ranges; stages needing a normalized re-export are learned by watching matches fall back this session.
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
                val unturned = rotationGaps(def)

                total += def.maxStage
                recorded += def.maxStage - missing.size

                // a crop wanting for nothing, rotations included, is left out of the listing
                if (missing.isEmpty() && legacy.isEmpty() && unturned.isEmpty()) return@forEach
                incomplete++

                val parts = mutableListOf<String>()
                if (missing.isNotEmpty()) parts += "stages ${ranges(missing)} unrecorded"
                if (legacy.isNotEmpty()) parts += "stages ${ranges(legacy)} need normalization"
                if (unturned.isNotEmpty()) parts += "stages ${ranges(unturned)} need rotation data"

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

    /** What one crop is missing, in the listing's own words, or null when it wants for nothing. */

    /**
     * Whether a stage knows how every stand is turned, role poses counted: one good sample of a
     * skull covers every stage that shows it.
     */
    private fun hasRotation(def: CropDefinition, stage: CropStage): Boolean =
        stage.armorStands.orEmpty().all {
            (it.headRotation != null && it.xRotation != null && it.yRotation != null) ||
                    def.standPoses.containsKey(it.hashString)
        }

    /** The stages of [def] recorded without the way their stands are turned. */
    fun rotationGaps(def: CropDefinition): List<Int> = def.stageDefs
        .filterNot { hasRotation(def, it) }
        .flatMap { it.stageRange }
        .distinct()
        .sorted()

    /** Whether the stage [stage] of [def] was recorded without its rotations. */
    fun needsRotation(def: CropDefinition, stage: Int): Boolean = def.stageDefs
        .any { stage in it.stageRange && !hasRotation(def, it) }

    fun reportFor(def: CropDefinition): String? {
        val covered = def.stageDefs.flatMap { it.stageRange }.toSet()
        val missing = (1..def.maxStage).filterNot { it in covered }
        val legacy = legacySeen[def.name].orEmpty().filter { it in covered }.sorted()
        val unturned = rotationGaps(def)

        val parts = mutableListOf<String>()
        if (missing.isNotEmpty()) parts += "stages ${ranges(missing)} unrecorded"
        if (legacy.isNotEmpty()) parts += "stages ${ranges(legacy)} need normalization"
        if (unturned.isNotEmpty()) parts += "stages ${ranges(unturned)} need rotation data"

        return parts.joinToString("; ").takeIf { it.isNotEmpty() }
    }

    /** How much of one crop is described, as a percentage of the stages it has. */
    fun percentFor(def: CropDefinition): Int {
        val covered = def.stageDefs.flatMap { it.stageRange }.toSet().count { it in 1..def.maxStage }

        return if (def.maxStage == 0) 100 else covered * 100 / def.maxStage
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
