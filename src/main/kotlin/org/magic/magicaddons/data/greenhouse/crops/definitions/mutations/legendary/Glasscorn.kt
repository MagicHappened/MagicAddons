package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.stateOf
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.sunflowerState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.Startlevine
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Chloronite
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Glasscorn {
    val definition = CropDefinition(
        name = "Glasscorn",
        tier = CropTier.Legendary,
        dropMultiplier = 16.0,
        effects = setOf(
            CropEffect.Immunity,
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("GLASSCORN"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "a9f8488c7566989ff5b52a23b47058d4f75b3c178e8a3651bbf70b546ad2e64" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "ac18dc1867944dd869d085e4084e3e4013a6be4860608e3c54f0d6e542c5149f" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "297de27338b9f876e570d1cc01fe1beccfc940467c5c97c467e93e79c81c25ee" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "c85e9fa773337f43de3afc9e2a60b26299f5eea9fd8490d19ed2ab34fc0c9cbb" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            glasscornStage(glass("white"), 1..2),
            glasscornStage(glass("lime"), 3..3),
            glasscornStage(glass("yellow"), 4..4),
            glasscornStage(glass("orange"), 5..5),
            glasscornStage(glass("red"), 6..6),
            // an assumption based on placing down a glasscorn, if this turns out to be wrong fix it
            glasscornStage(Blocks.AIR.defaultBlockState(), 7..8)
        ),
        // glasscorn thats stage 8 and grows a stage will roll back to stage 1
        maxStage = 8,
        footprint = Footprint(2, 2),
        requiredSoil = setOf(Blocks.SAND, Blocks.RED_SAND),
        isMutation = true,
        resetsToFirstStage = true,
        spawnRule = SpawnRule(weight = 20, requiredNeighbourCells = mapOf("Startlevine" to 6, "Chloronite" to 6))
    )

    private fun glass(color: String): BlockState = stateOf("minecraft:${color}_stained_glass")

    private fun glasscornStage(glass: BlockState, stageRange: IntRange): CropStage = CropStage(
        blocks = StageBlock.atPositions(
            positions = listOf(
                BlockPos(0, 1, 0),
                BlockPos(0, 1, 1),
                BlockPos(1, 1, 0),
                BlockPos(1, 1, 1)
            ),
            blockState = sunflowerState()
        ) + StageBlock.atPositions(
            positions = listOf(
                BlockPos(0, 2, 0),
                BlockPos(0, 2, 1),
                BlockPos(1, 2, 0),
                BlockPos(1, 2, 1)
            ),
            blockState = glass
        ),
        armorStands = StageStand.atOffsets(
            offsets = listOf(
                Vec3(-0.5, 0.9375, -0.5),
                Vec3(0.5, 0.9375, -0.5),
                Vec3(-0.5, 0.9375, 0.5),
                Vec3(0.5, 0.9375, 0.5)
            ),
            hashString = "a9f8488c7566989ff5b52a23b47058d4f75b3c178e8a3651bbf70b546ad2e64",
            isSmall = false
        ) + StageStand.atOffsets(
            offsets = listOf(
                Vec3(-0.5, 0.375, -0.5),
                Vec3(0.5, 0.375, -0.5),
                Vec3(-0.5, 0.375, 0.5),
                Vec3(0.5, 0.375, 0.5)
            ),
            hashString = "ac18dc1867944dd869d085e4084e3e4013a6be4860608e3c54f0d6e542c5149f",
            isSmall = false
        ),
        stageRange = stageRange
    )
}
