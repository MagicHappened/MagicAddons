package org.magic.magicaddons.data.greenhouse

/**
 * A buff or debuff a crop carries.
 *
 * Several of these are the same effect at a different strength: harvest boost and improved harvest
 * boost both move yield, water retain and improved water retain both slow drying. They are kept as
 * separate entries because that is how the game names them, but each says which [Kind] it belongs
 * to and by how much, so anything working out what a plot actually does can add up a kind rather
 * than listing every name it might meet.
 *
 * [percent] is signed: a loss is negative. Effects that do not scale carry zero.
 */
enum class CropEffect(val kind: Kind, val percent: Int) {

    HarvestBoost(Kind.Yield, 20),
    ImprovedHarvestBoost(Kind.Yield, 30),
    HarvestLoss(Kind.Yield, -20),

    XpBoost(Kind.Xp, 20),
    ImprovedXpBoost(Kind.Xp, 30),
    XpLoss(Kind.Xp, -20),

    /** Slows how fast a neighbour dries out, which is what the growth prediction has to honour. */
    WaterRetain(Kind.Water, 50),
    ImprovedWaterRetain(Kind.Water, 100),
    WaterDrain(Kind.Water, -30),

    /** Harvested items roll from an extra loot pool. */
    BonusDrops(Kind.Drops, 0),

    /** Shrugs off the negative effects of its neighbours. */
    Immunity(Kind.Immunity, 0),

    /** Passes whatever this crop carries on to the crops beside it. */
    EffectSpread(Kind.Spread, 0);

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
