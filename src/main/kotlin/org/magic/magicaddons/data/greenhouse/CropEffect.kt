package org.magic.magicaddons.data.greenhouse

/**
 * A buff or debuff a crop carries. Several are the same effect at a different strength, so each
 * says which kind it belongs to and by how much. The percentage is signed, zero when it does not scale.
 */
enum class CropEffect(val kind: Kind, val percent: Int, val label: String) {

    HarvestBoost(Kind.Yield, 20, "Harvest Boost"),
    ImprovedHarvestBoost(Kind.Yield, 30, "Improved Harvest Boost"),
    HarvestLoss(Kind.Yield, -20, "Harvest Loss"),

    XpBoost(Kind.Xp, 20, "XP Boost"),
    ImprovedXpBoost(Kind.Xp, 30, "Improved XP Boost"),
    XpLoss(Kind.Xp, -20, "XP Loss"),

    /** Slows how fast a neighbour dries out, which is what the growth prediction has to honour. */
    WaterRetain(Kind.Water, 50, "Water Retain"),
    ImprovedWaterRetain(Kind.Water, 100, "Improved Water Retain"),
    WaterDrain(Kind.Water, -30, "Water Drain"),

    /** Harvested items roll from an extra loot pool. */
    BonusDrops(Kind.Drops, 0, "Bonus Drops"),

    /** Shrugs off the negative effects of its neighbours. */
    Immunity(Kind.Immunity, 0, "Immunity"),

    /** Passes whatever this crop carries on to the crops beside it. */
    EffectSpread(Kind.Spread, 0, "Effect Spread");

    /** What an effect acts on, so effects of one kind can be totalled together. */
    enum class Kind {
        Yield,
        Xp,
        Water,
        Drops,
        Immunity,
        Spread
    }

    companion object {
        /** How much a kind adds up to across [effects], as a signed percentage. */
        fun total(effects: Iterable<CropEffect>, kind: Kind): Int =
            effects.filter { it.kind == kind }.sumOf { it.percent }
    }
}
