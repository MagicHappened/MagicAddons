package org.magic.magicaddons.features.farming.fortune

import kotlin.math.abs

object FortuneAudit {

    private const val TOLERANCE: Double = 0.05

    class Comparison(val stat: String, val hypixel: Double, val ours: Double) {
        val matches: Boolean get() = abs(ours - hypixel) < TOLERANCE
        val difference: Double get() = ours - hypixel
    }

    fun comparisons(snapshot: FortuneSnapshot): List<Comparison> {
        val totals = FortuneReadings.statTotals() ?: return emptyList()
        val byName = totals.entries.associate { it.name to it.value }
        return buildList {
            byName["Farming Fortune"]?.let { add(Comparison("Farming Fortune", it, snapshot.general)) }
            FortuneCrops.representatives.forEach { crop ->
                val stat = FortuneCrops.tabStat(crop)
                byName[stat]?.let { add(Comparison(stat, it, snapshot.cropOnly(crop))) }
            }
        }
    }

    class CategoryGap(val category: String, val ours: Double, val breakdown: Double)

    fun categoryGaps(snapshot: FortuneSnapshot): List<CategoryGap> {
        val breakdown = FortuneReadings.breakdown(FortuneStat.General) ?: return emptyList()
        val theirs = breakdown.entries.filter { it.category in FarmingFortune.LIVE_CATEGORIES || it.name == FarmingFortune.PLOTS_ENTRY }
            .groupBy { if (it.name == FarmingFortune.PLOTS_ENTRY) FarmingFortune.PLOTS_ENTRY else it.category }
            .mapValues { (_, entries) -> entries.sumOf { it.value } }
        val ours = snapshot.sources.filter { it.stat == FortuneStat.General && it.category != null }
            .groupBy { it.category!! }
            .mapValues { (_, sources) -> sources.sumOf { it.value } }
        return (theirs.keys + ours.keys).sorted().mapNotNull { category ->
            val gap = CategoryGap(category, ours[category] ?: 0.0, theirs[category] ?: 0.0)
            gap.takeIf { abs(it.ours - it.breakdown) >= TOLERANCE }
        }
    }

    fun signed(value: Double): String = (if (value >= 0) "+" else "-") + format(abs(value))

    fun format(value: Double): String =
        if (value % 1.0 == 0.0) "%,d".format(value.toLong()) else "%,.1f".format(value)
}
