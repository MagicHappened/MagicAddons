package org.magic.magicaddons.features.farming.fortune

import org.magic.magicaddons.features.farming.greenhousePresets.lookups.StatsWidget

object FortuneAudit {

    enum class Verdict { Matches, Differs, NotInTab }

    class Line(val label: String, val ours: Double, val tab: Int?, val verdict: Verdict) {
        val text: String
            get() = when (verdict) {
                Verdict.Matches -> "$label: matches tab list (${format(ours)})"
                Verdict.Differs -> "$label: ours ${format(ours)}, tab list ${format(tab!!.toDouble())}"
                Verdict.NotInTab -> "$label: ${format(ours)}, not in tab list"
            }
    }

    fun lines(snapshot: FortuneSnapshot): List<Line> = buildList {
        add(line(StatsWidget.FARMING_FORTUNE, snapshot.general))
        FortuneCrops.representatives.forEach { crop -> add(line(FortuneCrops.tabStat(crop), snapshot.forCrop(crop))) }
    }

    private fun line(stat: String, ours: Double): Line {
        val tab = StatsWidget.value(stat) ?: return Line(stat, ours, null, Verdict.NotInTab)
        val verdict = if (ours.toInt() == tab) Verdict.Matches else Verdict.Differs
        return Line(stat, ours, tab, verdict)
    }

    fun format(value: Double): String =
        if (value % 1.0 == 0.0) "%,d".format(value.toLong()) else "%,.1f".format(value)
}
