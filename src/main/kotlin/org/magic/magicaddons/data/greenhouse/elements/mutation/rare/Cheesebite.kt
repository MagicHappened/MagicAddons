package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.sunflowerState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cheesebite : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cheesebite",
        skyblockId = SkyBlockItemId.item("CHEESEBITE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.9375, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717"
                    )
                ),
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
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717"
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717"
                    )
                ),
                3..3
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                    )
                ),
                4..4 //todo figure out rats thing
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
                            offset = Vec3(0.0, -0.4375, 0.0),
                            hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        )
                    )
                ,
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.15625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717"
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717"
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717"
                    )
                ),
                10..10
            )

        ),
        maxStage = 10,
        isMutation = true
    )
}