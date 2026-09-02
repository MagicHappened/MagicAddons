package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.sugarcaneState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

/**
 * A stack of sugar cane with a melon on top, climbing a hundred and twenty stages by repeating one
 * twelve-stage cycle. The stages are generated from that cycle rather than written out.
 */
object MagicJellybean : CropDefinitionProvider {

    /** The sugar cane, whose head every length of the stack carries. */
    private const val CANE_HASH = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"

    /** The melon riding on top, which is there until it becomes the next length of cane. */
    private const val MELON_HASH = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"

    /** How far above its own block a cane's head hangs. */
    private const val CANE_STAND_Y = -0.21875

    /**
     * The three cane head poses, picked by (x + z + height) mod 3 rather than the world's mod-4 rule.
     */
    private val CANE_POSES = listOf(
        Rotations(22.5f, -22.5f, 0.0f),
        Rotations(-22.5f, 22.5f, -22.5f),
        Rotations(-22.5f, 0.0f, 22.5f)
    )

    /** Stages to a cycle, and how many cycles before the plant stops growing. */
    private const val CYCLE = 12
    private const val MAX_STAGE = 120

    /** The tallest the cane gets, after which the melon on top stops being replaced. */
    private const val MAX_CANE = 10

    /**
     * The top of the plant at one point in the cycle: the stem's own growth, how high the melon hangs,
     * and whether the next cane already has its head. Unseen positions are left undescribed.
     */
    private data class Top(
        val stemAge: Int,
        val melonStandY: Double? = null,
        val extraCaneStand: Boolean = false
    )

    /**
     * The cycle as observed, keyed by runs of positions: the plant looks identical for the first
     * three of every cycle, so one stage covers all three. All twelve positions are known.
     */
    private val cycle: List<Pair<IntRange, Top>> = listOf(
        0..2 to Top(stemAge = 3),
        3..4 to Top(stemAge = 5, melonStandY = 0.28125),
        5..6 to Top(stemAge = 7, melonStandY = 0.59375),
        7..8 to Top(stemAge = 7, melonStandY = 0.6875),
        9..10 to Top(stemAge = 6, melonStandY = 0.78125),
        11..11 to Top(stemAge = 6, extraCaneStand = true)
    )

    /** One stage per known run, at every height: position k of cycle c is always stage c * 12 + k. */
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

                val caneStands = (0 until standCount).map { height ->
                    CropArmorStand(
                        offset = Vec3(0.0, CANE_STAND_Y + height, 0.0),
                        // a cane still arriving hangs its head its own way rather than where the cycle
                        // puts it, as stage 23 shows
                        headRotation = if (top.extraCaneStand && height == caneHeight) {
                            Rotations(22.5f, 22.5f, 22.5f)
                        } else {
                            null
                        },
                        hashString = CANE_HASH,
                        isSmall = false
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
                    )
                )
            }
        }

        add(topless(MAX_CANE))
    }

    /** The finished plant: ten canes wearing all ten heads, no stem and no melon. */
    private fun topless(caneHeight: Int): CropStage = CropStage(
        blocks = (1..caneHeight).map {
            CropBlockState(offset = BlockPos(0, it, 0), blockState = sugarcaneState())
        },
        armorStands = (0 until caneHeight).map {
            CropArmorStand(
                offset = Vec3(0.0, CANE_STAND_Y + it, 0.0),
                hashString = CANE_HASH,
                isSmall = false
            )
        },
        stageRange = MAX_STAGE..MAX_STAGE
    )

    override val definition = CropDefinition(
        name = "Magic Jellybean",
        effects = setOf(
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("MAGIC_JELLYBEAN"),
        stageDefs = generateStages(),
        standPoses = mapOf(
            CANE_HASH to StandPose.Cycle(CANE_POSES),
            MELON_HASH to StandPose.Fixed(Rotations(0.0f, 22.5f, 22.5f))
        ),
        maxStage = MAX_STAGE,
        decayTimeMs = NEVER_DECAYS,
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}
