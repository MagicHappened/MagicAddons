package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.sugarcaneState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

/**
 * A jellybean is a stack of sugar cane with a melon growing on top of it, and it climbs a hundred
 * and twenty stages doing the same twelve-stage thing over and over: the melon on top ripens, turns
 * into another length of cane, and a new melon starts above it.
 *
 * So the stages are generated from one twelve-row cycle rather than written out. Writing them out
 * would take a hundred and twenty near-identical blocks that differ only in how many times the same
 * two lines repeat, and every one of them would have to be re-exported to fix a single mistake.
 */
object MagicJellybean : CropDefinitionProvider {

    /** The sugar cane, whose head every length of the stack carries. */
    private const val CANE_HASH = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"

    /** The melon riding on top, which is there until it becomes the next length of cane. */
    private const val MELON_HASH = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"

    /** How far above its own block a cane's head hangs. */
    private const val CANE_STAND_Y = -0.21875

    /** Stages to a cycle, and how many cycles before the plant stops growing. */
    private const val CYCLE = 12
    private const val MAX_STAGE = 120

    /** The tallest the cane gets, after which the melon on top stops being replaced. */
    private const val MAX_CANE = 10

    /**
     * What the top of the plant looks like at one point in the cycle.
     *
     * [stemAge] is the melon stem's own growth, [melonStandY] is how far above the cane below it the
     * melon's head hangs, and [extraCaneStand] covers the one stage where the next length of cane
     * has its head but not yet its block.
     *
     * A cycle position with no row is a stage we have not seen, and stages we have not seen are not
     * described rather than guessed at: a wrong stage matches the wrong plant, which is worse than
     * not matching.
     */
    private data class Top(
        val stemAge: Int,
        val melonStandY: Double? = null,
        val extraCaneStand: Boolean = false
    )

    /**
     * The cycle, as far as it has been observed.
     *
     * Keyed by a run of positions rather than by one, because the plant does not change every
     * stage. It spends the first three positions of every cycle looking exactly the same, so those
     * three stages cannot be told apart by looking, and a stage that covers all three says so
     * instead of claiming to be the first of them.
     *
     * Read off exported stages 1, 2, 9, 18, 90, 94, 95 and 96, which agree with each other wherever
     * two of them landed on the same position.
     */
    private val cycle: List<Pair<IntRange, Top>> = listOf(
        0..2 to Top(stemAge = 3),
        6..6 to Top(stemAge = 7, melonStandY = 0.59375),
        9..10 to Top(stemAge = 6, melonStandY = 0.78125),
        11..11 to Top(stemAge = 6, extraCaneStand = true)
    )

    /**
     * One stage per run of cycle positions we know, at every height the plant reaches.
     *
     * A run covers consecutive stages because a cycle is consecutive: position k of cycle c is
     * always stage `c * 12 + k`, and the whole cycle shares one cane height.
     */
    private fun generateStages(): List<CropStage> = buildList {
        for (cycleNumber in 0 until MAX_CANE) {
            val caneHeight = cycleNumber

            for ((positions, top) in cycle) {
                // stage zero is not a stage, and the top of the plant is its own stage
                val first = (cycleNumber * CYCLE + positions.first).coerceAtLeast(1)
                val last = (cycleNumber * CYCLE + positions.last).coerceAtMost(MAX_STAGE - 1)

                if (first > last) continue

                val canes = (1..caneHeight).map {
                    CropBlockState(offset = BlockPos(0, it, 0), blockState = sugarcaneState())
                }

                val standCount = if (top.extraCaneStand) caneHeight + 1 else caneHeight

                val caneStands = (0 until standCount).map {
                    CropArmorStand(
                        offset = Vec3(0.0, CANE_STAND_Y + it, 0.0),
                        hashString = CANE_HASH
                    )
                }

                val melonStand = top.melonStandY?.let {
                    CropArmorStand(
                        offset = Vec3(0.0, caneHeight + it, 0.0),
                        hashString = MELON_HASH
                    )
                }

                add(
                    CropStage(
                        blocks = canes + CropBlockState(
                            offset = BlockPos(0, caneHeight + 1, 0),
                            blockState = melonStemState(top.stemAge)
                        ),
                        armorStands = caneStands + listOfNotNull(melonStand),
                        stageRange = first..last,
                        allowRotation = true
                    )
                )
            }
        }

        add(topless(MAX_CANE))
    }

    /** The finished plant: cane the whole way up and nothing growing above it. */
    private fun topless(caneHeight: Int): CropStage = CropStage(
        blocks = (1..caneHeight).map {
            CropBlockState(offset = BlockPos(0, it, 0), blockState = sugarcaneState())
        },
        armorStands = (0 until caneHeight).map {
            CropArmorStand(
                offset = Vec3(0.0, CANE_STAND_Y + it, 0.0),
                hashString = CANE_HASH
            )
        },
        stageRange = MAX_STAGE..MAX_STAGE,
        allowRotation = true
    )

    override val definition = CropDefinition(
        name = "Magic Jellybean",
        effects = setOf(
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("MAGIC_JELLYBEAN"),
        stageDefs = generateStages(),
        maxStage = MAX_STAGE,
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}
