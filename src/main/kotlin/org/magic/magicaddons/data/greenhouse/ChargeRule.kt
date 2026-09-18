package org.magic.magicaddons.data.greenhouse

import kotlin.math.roundToInt

/** a plant that charges as it grows and destroys itself on reaching [limit] */
data class ChargeRule(
    val perStage: Int,
    val limit: Int
) {
    fun stagesUntilOverload(charge: Int): Int = (limit - charge) / perStage

    /** the most a plant at [stage] can hold if it has never been discharged: nothing at stage 1 */
    fun chargeImpliedBy(stage: Int): Int = perStage * (stage - 1).coerceAtLeast(0)

    /** the charge a bar filled to [percent] stands for, which can only ever be whole stages */
    fun chargeShownBy(percent: Int): Int = (limit * percent / 100.0 / perStage).roundToInt() * perStage
}
