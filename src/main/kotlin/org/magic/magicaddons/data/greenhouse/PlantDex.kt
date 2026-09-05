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

    /** Per crop, the stages whose stands stood at the other size, with the isSmall the definition needs. */
    private val sizeSeen: MutableMap<String, MutableMap<Int, Boolean>> = mutableMapOf()

    fun noteSize(cropName: String, stage: Int, needsSmall: Boolean) {
        sizeSeen.getOrPut(cropName) { mutableMapOf() }[stage] = needsSmall
    }

    /** The isSmall a run this session found the stage [stage] of [def] needs, or null when none did. */
    fun neededSize(def: CropDefinition, stage: Int): Boolean? = sizeSeen[def.name]?.get(stage)

    /** Reading order of the dex: base crops first, then mutations by rarity, then the rest. */
    private val TIER_NAMES = listOf(
        "base crops", "common mutations", "uncommon mutations", "rare mutations",
        "epic mutations", "legendary mutations", "rare crops", "other"
    )

    /** The same tiers as chat headings. */
    val TIER_TITLES: List<String> = listOf(
        "Base Crops", "Common", "Uncommon", "Rare", "Epic", "Legendary", "Rare Crops", "Misc"
    )

    /** One crop with something still missing, in the listing's words. */
    class Gap(val def: CropDefinition, val parts: List<String>)

    /** Every crop with a gap, grouped by tier in reading order. */
    fun gapsByTier(): Map<Int, List<Gap>> = CropRegistry.all
        .sortedWith(compareBy({ CropRegistry.tierOf[it] ?: 7 }, { it.name }))
        .mapNotNull { def -> partsFor(def).takeIf { it.isNotEmpty() }?.let { Gap(def, it) } }
        .groupBy { CropRegistry.tierOf[it.def] ?: 7 }
        .toSortedMap()

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
                total += def.maxStage
                recorded += def.maxStage - unrecorded(def).size

                // a crop wanting for nothing, rotations included, is left out of the listing
                val parts = partsFor(def)
                if (parts.isEmpty()) return@forEach
                incomplete++

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

    /**
     * Whether a mutation has no recording of how it looks when bought and put down. Not a stage of
     * its own and not counted against the dex, only listed: only the hologram and the collector want it.
     */
    fun lacksPlacedLook(def: CropDefinition): Boolean = def.isMutation && def.stageDefs.none { it.placed }

    /** The stages of [def] no recording covers. */
    private fun unrecorded(def: CropDefinition): List<Int> {
        val covered = def.stageDefs.flatMap { it.stageRange }.toSet()
        return (1..def.maxStage).filterNot { it in covered }
    }

    /** What one crop is missing, one part per kind of gap; empty when it wants for nothing. */
    private fun partsFor(def: CropDefinition): List<String> {
        val covered = def.stageDefs.flatMap { it.stageRange }.toSet()
        val missing = unrecorded(def)
        val legacy = legacySeen[def.name].orEmpty().filter { it in covered }.sorted()
        val unturned = rotationGaps(def)
        val sizes = sizeSeen[def.name].orEmpty()
        val oversized = sizes.filterValues { !it }.keys.sorted()
        val undersized = sizes.filterValues { it }.keys.sorted()

        val parts = mutableListOf<String>()
        if (missing.isNotEmpty()) parts += "stages ${ranges(missing)} unrecorded"
        if (lacksPlacedLook(def)) parts += "placed look unrecorded"
        if (legacy.isNotEmpty()) parts += "stages ${ranges(legacy)} need normalization"
        if (unturned.isNotEmpty()) parts += "stages ${ranges(unturned)} need rotation data"
        if (oversized.isNotEmpty()) parts += "stages ${ranges(oversized)} need isSmall = false"
        if (undersized.isNotEmpty()) parts += "stages ${ranges(undersized)} need isSmall = true"
        return parts
    }

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

    /** What one crop is missing, in the listing's own words, or null when it wants for nothing. */
    fun reportFor(def: CropDefinition): String? =
        partsFor(def).joinToString("; ").takeIf { it.isNotEmpty() }

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
