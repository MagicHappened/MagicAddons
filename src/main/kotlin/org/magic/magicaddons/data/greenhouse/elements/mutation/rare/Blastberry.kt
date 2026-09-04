package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import net.minecraft.core.Rotations

object Blastberry : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Blastberry",
        effects = setOf(
            CropEffect.Immunity,
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("BLASTBERRY"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "bacae0e87ffeadb750260c2e6531004d69d14473376cb22577fafe70569349f3" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "a3b6c45e028b8b70befb6feb9d320dbda86c6f925961e23babbc7ff250918d0f",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = Blocks.REDSTONE_TORCH.defaultBlockState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "bacae0e87ffeadb750260c2e6531004d69d14473376cb22577fafe70569349f3",
                        isSmall = false
                    )
                ),
                6..6
            )

        ),
        maxStage = 6,
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}