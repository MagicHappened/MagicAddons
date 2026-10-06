package org.magic.magicaddons.data.greenhouse.crops

class DecayOutlook(val kind: Kind, val mutationsLeft: Int, val spotsThatCanSpawn: Int) {

    enum class Kind { OnTime, AfterSpawns, AfterUncountedSpawns, Never }

    val isCertain: Boolean get() = kind == Kind.OnTime

    val canDecay: Boolean get() = kind != Kind.Never
}
