package org.magic.magicaddons.data.greenhouse.crops

enum class CropEffect(val kind: EffectKind, val percent: Int, val label: String) {

    HarvestBoost(EffectKind.Yield, 20, "Harvest Boost"),
    ImprovedHarvestBoost(EffectKind.Yield, 30, "Improved Harvest Boost"),
    HarvestLoss(EffectKind.Yield, -20, "Harvest Loss"),

    XpBoost(EffectKind.Xp, 20, "XP Boost"),
    ImprovedXpBoost(EffectKind.Xp, 30, "Improved XP Boost"),
    XpLoss(EffectKind.Xp, -20, "XP Loss"),

    WaterRetain(EffectKind.Water, 50, "Water Retain"),
    ImprovedWaterRetain(EffectKind.Water, 100, "Improved Water Retain"),
    WaterDrain(EffectKind.Water, -30, "Water Drain"),

    BonusDrops(EffectKind.Drops, 0, "Bonus Drops"),

    Immunity(EffectKind.Immunity, 0, "Immunity"),

    EffectSpread(EffectKind.Spread, 0, "Effect Spread");

    enum class EffectKind {
        Yield,
        Xp,
        Water,
        Drops,
        Immunity,
        Spread
    }


    companion object {
        fun appliedEffect(effects: Iterable<CropEffect>, kind: EffectKind): Int {
            val percents = effects.filter { it.kind == kind }.map { it.percent }
            val maxEffect = percents.maxOrNull() ?: 0
            if (maxEffect <= 0) return maxEffect
            return maxEffect + (percents.minOrNull()?.coerceAtMost(0) ?: 0)
        }

    }
}
