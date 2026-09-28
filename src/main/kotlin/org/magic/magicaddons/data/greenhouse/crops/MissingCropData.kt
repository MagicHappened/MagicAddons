package org.magic.magicaddons.data.greenhouse.crops

object MissingCropData {

    class CropGap(val crop: CropDefinition, val missingParts: List<String>)

    fun gapsByTier(): Map<CropTier, List<CropGap>> = CropRegistry.allCrops
        .sortedWith(compareBy({ it.tier }, { it.name }))
        .mapNotNull { crop -> missingStages(crop).takeIf { it.isNotEmpty() }?.let { CropGap(crop, it) } }
        .groupBy { it.crop.tier }
        .toSortedMap()

    class CropDataReport(val recordedStages: Int, val totalStages: Int, val incompleteCrops: Int, val listing: String) {
        val percent: Int get() = if (totalStages == 0) 100 else recordedStages * 100 / totalStages
    }

    fun cropDataReport(): CropDataReport {
        val recordedStages = CropRegistry.allCrops.sumOf { it.maxStage - unrecordedStages(it).size }
        val totalStages = CropRegistry.allCrops.sumOf { it.maxStage }
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

    private fun unrecordedStages(crop: CropDefinition): List<Int> {
        val covered = crop.stages.flatMap { it.stageRange }.toSet()
        return (1..crop.maxStage).filterNot { it in covered }
    }

    private fun missingStages(crop: CropDefinition): List<String> {
        val missing = unrecordedStages(crop)

        val parts = mutableListOf<String>()
        if (missing.isNotEmpty()) parts += "stages ${asRanges(missing)}"
        return parts
    }

    fun missingStagesSummary(crop: CropDefinition): String? =
        missingStages(crop).joinToString("; ").takeIf { it.isNotEmpty() }

    fun recordedPercent(crop: CropDefinition): Int {
        val covered = crop.stages.flatMap { it.stageRange }.toSet().count { it in 1..crop.maxStage }

        return if (crop.maxStage == 0) 100 else covered * 100 / crop.maxStage
    }

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
