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

    private val standOffsets = listOf(
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

    private val flatYaws = listOf(0.0f, 0.0f, 0.0f, 0.0f)

    /** One look of a stage: the shared geometry wearing the skull of what it craves. */
    private fun look(stage: Int, hash: String, craving: Int): CropStage = CropStage(
        blocks = CropBlockState.blockStatePattern(
            positions = wheatPositions,
            blockState = wheatState(6)
        ),
        armorStands = CropArmorStand.matcherPattern(
            offsets = standOffsets,
            rotations = standRotations,
            xRotations = flatYaws,
            yRotations = flatYaws,
            hashString = hash
        ),
        stageRange = stage..stage,
        traits = mapOf(CropStandReader.CRAVES to craving)
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
            look(
                stage = 4,
                hash = "5cdd8c3d5d76a1dc07cdbedc5fd0bb230852df9c1864896f8893f5bfdf3d4c96",
                craving = CropStandReader.CRAVES_DAY
            ),
            look(
                stage = 4,
                hash = "b1b18493d50ff8972f7ef359893d9063fdc54cb822c679002957c294fc8b0005",
                craving = CropStandReader.CRAVES_NIGHT
            )
        ),
        decayTimeMs = SIX_DAY_DECAY_TIME_MS,
        maxStage = 4,
        footprint = Footprint(2, 2),
        isMutation = true
    )
}
