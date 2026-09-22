package org.magic.magicaddons.data.greenhouse.crops

import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.Zombud
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary.Devourer

/** what the definitions still miss, crop by crop */
object CropDataGaps {

    /** per crop, the stages whose stands stood at the other size, with the isSmall the definition needs */
    private val sizeCorrections: MutableMap<String, MutableMap<Int, Boolean>> = mutableMapOf()

    fun noteSizeCorrection(cropName: String, stage: Int, needsSmall: Boolean) {
        sizeCorrections.getOrPut(cropName) { mutableMapOf() }[stage] = needsSmall
    }

    /** null when no run this session found a size to correct */
    fun neededIsSmall(crop: CropDefinition, stage: Int): Boolean? = sizeCorrections[crop.name]?.get(stage)

    class CropGap(val crop: CropDefinition, val missingParts: List<String>)

    fun gapsByTier(): Map<CropTier, List<CropGap>> = CropRegistry.all
        .sortedWith(compareBy({ it.tier }, { it.name }))
        .mapNotNull { crop -> missingParts(crop).takeIf { it.isNotEmpty() }?.let { CropGap(crop, it) } }
        .groupBy { it.crop.tier }
        .toSortedMap()

    class CropDataReport(val recordedStages: Int, val totalStages: Int, val incompleteCrops: Int, val listing: String) {
        val percent: Int get() = if (totalStages == 0) 100 else recordedStages * 100 / totalStages
    }

    fun cropDataReport(): CropDataReport {
        val recordedStages = CropRegistry.all.sumOf { it.maxStage - unrecordedStages(it).size }
        val totalStages = CropRegistry.all.sumOf { it.maxStage }
        val gapsByTier = gapsByTier()

        val listing = buildString {
            gapsByTier.forEach { (tier, gaps) ->
                appendLine("== ${tier.listingName} ==")
                gaps.forEach { appendLine("${it.crop.name} -> ${it.missingParts.joinToString("; ")}") }
                appendLine()
            }
        }.trimEnd()

        return CropDataReport(recordedStages, totalStages, gapsByTier.values.sumOf { it.size }, listing)
    }
    
    private fun missingVariants(crop: CropDefinition): List<String> {
        val parts = mutableListOf<String>()
        val looks = crop.stages

        if (crop.sleepStages.isNotEmpty()) {
            val sleeping = crop.sleepStages.filter { it <= crop.maxStage }.sorted()
            val asleepMissing = sleeping.filter { stage -> looks.none { stage in it.stageRange && it.readers.any { r -> r.key == StandReader.ASLEEP } } }
            val awakeMissing = sleeping.filter { stage -> looks.none { stage in it.stageRange && it.readers.none { r -> r.key == StandReader.ASLEEP } } }
            if (asleepMissing.isNotEmpty()) parts += "asleep look unrecorded at stages ${asRanges(asleepMissing)}"
            if (awakeMissing.isNotEmpty()) parts += "awake look unrecorded at stages ${asRanges(awakeMissing)}"
        }

        if (looks.any { StandReader.NEEDS_TIME in it.traits }) {
            val stages = (1..crop.maxStage).toList()
            val dayMissing = stages.filter { stage -> looks.none { stage in it.stageRange && it.traits[StandReader.NEEDS_TIME] == StandReader.NEEDS_DAY } }
            val nightMissing = stages.filter { stage -> looks.none { stage in it.stageRange && it.traits[StandReader.NEEDS_TIME] == StandReader.NEEDS_NIGHT } }
            if (dayMissing.isNotEmpty()) parts += "day look unrecorded at stages ${asRanges(dayMissing)}"
            if (nightMissing.isNotEmpty()) parts += "night look unrecorded at stages ${asRanges(nightMissing)}"
        }

        return parts
    }

    private fun unrecordedStages(crop: CropDefinition): List<Int> {
        val covered = crop.stages.flatMap { it.stageRange }.toSet()
        return (1..crop.maxStage).filterNot { it in covered }
    }

    private fun missingParts(crop: CropDefinition): List<String> {
        val missing = unrecordedStages(crop)
        val unturned = stagesWithoutRotation(crop)
        val sizes = sizeCorrections[crop.name].orEmpty()
        val oversized = sizes.filterValues { !it }.keys.sorted()
        val undersized = sizes.filterValues { it }.keys.sorted()

        val parts = mutableListOf<String>()
        if (missing.isNotEmpty()) parts += "stages ${asRanges(missing)} unrecorded"
        parts += missingVariants(crop)
        if (unturned.isNotEmpty()) parts += "stages ${asRanges(unturned)} need rotation data"
        if (oversized.isNotEmpty()) parts += "stages ${asRanges(oversized)} need isSmall = false"
        if (undersized.isNotEmpty()) parts += "stages ${asRanges(undersized)} need isSmall = true"
        return parts
    }

    /** listed by hand, by crop name */
    private val STAGES_WITHOUT_ROTATION: Map<String, Set<Int>> = mapOf(
        "Zombud" to setOf(7) + (10..15)
    )

    fun stagesWithoutRotation(crop: CropDefinition): List<Int> =
        STAGES_WITHOUT_ROTATION[crop.name].orEmpty().filter { it in 1..crop.maxStage }.sorted()

    fun isMissingRotation(crop: CropDefinition, stage: Int): Boolean =
        stage in STAGES_WITHOUT_ROTATION[crop.name].orEmpty()

    /** null when the crop misses nothing */
    fun missingSummary(crop: CropDefinition): String? =
        missingParts(crop).joinToString("; ").takeIf { it.isNotEmpty() }

    fun recordedPercent(crop: CropDefinition): Int {
        val covered = crop.stages.flatMap { it.stageRange }.toSet().count { it in 1..crop.maxStage }

        return if (crop.maxStage == 0) 100 else covered * 100 / crop.maxStage
    }

    /** 1, 2, 3, 7 written as 1-3, 7 */
    private fun asRanges(sortedStages: List<Int>): String = buildString {
        var i = 0
        while (i < sortedStages.size) {
            var j = i
            while (j + 1 < sortedStages.size && sortedStages[j + 1] == sortedStages[j] + 1) j++

            if (isNotEmpty()) append(", ")
            append(if (j > i) "${sortedStages[i]}-${sortedStages[j]}" else "${sortedStages[i]}")
            i = j + 1
        }
    }
}
