package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.sunflowerState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import net.minecraft.core.Rotations
import org.magic.magicaddons.data.greenhouse.StandPose

object Startlevine : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Startlevine",
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("STARTLEVINE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "98bef15a64354093d26b8f002e476b8012ed3ad9b061796953b6b1dad447d7" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.59375, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.4375, 0.0),
                            hashString = "98bef15a64354093d26b8f002e476b8012ed3ad9b061796953b6b1dad447d7",
                            isSmall = false
                        )
                    )
                ,
                12..12
            )




        ),
        // five days decay time
        maxStage = 12,
        isMutation = true
    )
}