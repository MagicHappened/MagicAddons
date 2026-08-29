package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Puffercloud : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Puffercloud",
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("PUFFERCLOUD"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.75, 0.0),
                            hashString = "a9ceff4063b495dbe5c42885f5f81b022d5b37322255b71e30963c489f936985",
                        )
                    )
                ,
                1..1
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
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "a9ceff4063b495dbe5c42885f5f81b022d5b37322255b71e30963c489f936985",
                    )
                ),
                2..2
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
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "a9ceff4063b495dbe5c42885f5f81b022d5b37322255b71e30963c489f936985",
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04"
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
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04"
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
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04",
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
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04"
                    )
                ),
                7..7
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
                        offset = Vec3(0.5, -0.65625, 0.5),
                        hashString = "2f9502a6895a90bbbb25921842fa6a213744ba967bbdfd861a44c92a79530aa0"
                    )
                ),
                8..8
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
                        hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.59375, 0.0),
                            hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b",
                        )
                    )
                ,
                11..11
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.53125, 0.0),
                            hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b",
                        )
                    )
                ,
                12..12
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b"
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "9a488340c3d9131b6e97bae6279aac852037367cb93a6b50c60a7d328aec173e",
                    )
                ),
                14..14
            )





        ),
        // five days decay time
        maxStage = 14,
        isMutation = true
    )
}