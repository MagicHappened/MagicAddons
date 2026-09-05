package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

/**
 * Craves either day or night, shown by which skull it carries. It only advances while the garden's
 * clock matches that craving, and the craving flips on every advance, so each stage has two skulls.
 */
object Noctilume : CropDefinitionProvider {

    private val wheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 1),
    )

    /** At the first stage all four stands ride high; the fourth settles a stage later. */
    private val seedOffsets = listOf(
        Vec3(-0.21875, 0.6875, 0.15625),
        Vec3(0.375, 0.84375, -0.3125),
        Vec3(0.28125, 0.78125, 0.125),
        Vec3(-0.125, 0.71875, -0.40625)
    )

    /** The four stands of a young plant sit high on the stalks, then settle as it grows. */
    private val youngOffsets = listOf(
        Vec3(-0.21875, 0.6875, 0.15625),
        Vec3(0.375, 0.84375, -0.3125),
        Vec3(0.28125, 0.78125, 0.125),
        Vec3(-0.125, -0.03125, -0.40625)
    )

    /** At the third stage three stands have settled and the third still rides high. */
    private val settlingOffsets = listOf(
        Vec3(-0.21875, -0.0625, 0.15625),
        Vec3(0.375, 0.09375, -0.3125),
        Vec3(0.28125, 0.78125, 0.125),
        Vec3(-0.125, -0.03125, -0.40625)
    )

    private val grownOffsets = listOf(
        Vec3(-0.21875, -0.0625, 0.15625),
        Vec3(0.375, 0.09375, -0.3125),
        Vec3(0.28125, 0.03125, 0.125),
        Vec3(-0.125, -0.03125, -0.40625)
    )

    private val standRotations = listOf(
        Rotations(22.5f, 0.0f, -22.5f),
        Rotations(-22.5f, 0.0f, 22.5f),
        Rotations(22.5f, 0.0f, 22.5f),
        Rotations(-22.5f, 0.0f, -22.5f)
    )

    /** One look of a stage: the shared geometry wearing the skull of what it craves, if anything. [fullSized] lists the stands not small. */
    private fun look(
        stage: Int,
        hash: String,
        craving: Int?,
        wheatAge: Int,
        offsets: List<Vec3>,
        fullSized: Set<Int> = emptySet()
    ): CropStage = CropStage(
        blocks = CropBlockState.blockStatePattern(
            positions = wheatPositions,
            blockState = wheatState(wheatAge)
        ),
        armorStands = offsets.indices.map { index ->
            CropArmorStand(
                offset = offsets[index],
                headRotation = standRotations[index],
                xRotation = 0f,
                yRotation = 0f,
                hashString = hash,
                isSmall = index !in fullSized
            )
        },
        stageRange = stage..stage,
        traits = craving?.let { mapOf(CropStandReader.CRAVES to it) } ?: emptyMap()
    )

    override val definition = CropDefinition(
        name = "Noctilume",
        effects = setOf(
            CropEffect.EffectSpread,
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("NOCTILUME"),
        stageDefs = listOf(
            // the only seedling recorded so far craves night; a day one is expected
            look(
                stage = 1,
                hash = "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09",
                craving = CropStandReader.CRAVES_NIGHT,
                wheatAge = 3,
                offsets = seedOffsets
            ),
            look(
                stage = 1,
                hash = "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8",
                craving = CropStandReader.CRAVES_DAY,
                wheatAge = 3,
                offsets = seedOffsets
            ),
            look(
                stage = 2,
                hash = "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8",
                craving = CropStandReader.CRAVES_DAY,
                wheatAge = 4,
                offsets = youngOffsets,
                fullSized = setOf(3)
            ),
            look(
                stage = 2,
                hash = "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09",
                craving = CropStandReader.CRAVES_NIGHT,
                wheatAge = 4,
                offsets = youngOffsets,
                fullSized = setOf(3)
            ),
            look(
                stage = 3,
                hash = "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09",
                craving = CropStandReader.CRAVES_NIGHT,
                wheatAge = 5,
                offsets = settlingOffsets,
                fullSized = setOf(0, 1, 3)
            ),
            look(
                stage = 4,
                hash = "5cdd8c3d5d76a1dc07cdbedc5fd0bb230852df9c1864896f8893f5bfdf3d4c96",
                craving = CropStandReader.CRAVES_DAY,
                wheatAge = 6,
                offsets = grownOffsets
            ),
            look(
                stage = 4,
                hash = "b1b18493d50ff8972f7ef359893d9063fdc54cb822c679002957c294fc8b0005",
                craving = CropStandReader.CRAVES_NIGHT,
                wheatAge = 6,
                offsets = grownOffsets,
                fullSized = setOf(3)
            )
        ),
        decayTimeMs = SIX_DAY_DECAY_TIME_MS,
        maxStage = 4,
        footprint = Footprint(2, 2),
        isMutation = true
    )
}
