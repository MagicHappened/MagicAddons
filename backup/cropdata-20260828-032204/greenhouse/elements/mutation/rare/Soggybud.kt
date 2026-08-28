package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Soggybud : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Soggybud",
        skyblockId = SkyBlockItemId.item("SOGGYBUD"),
        maxStage = 10,
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                4..4
            )
            ,
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(0.0, -0.4375, 0.0),
                    headRotation = Rotations(0.0f, 0.0f, 0.0f),
                    hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                7..7
            )
            ,
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.1875, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.09375, 0.0),
                            hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        )
                    ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                    )
                ),
                10..10
            )



        ),
        isMutation = true
    )
}