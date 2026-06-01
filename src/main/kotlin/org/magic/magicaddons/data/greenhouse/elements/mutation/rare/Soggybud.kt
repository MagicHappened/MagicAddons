package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
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
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
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
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
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
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, 0.0),
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
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
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.1875, 0.0),
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.09375, 0.0),
                            hashMatches = {
                                it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                            },
                        )
                    ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashMatches = {
                            it == "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356"
                        }
                    )
                ),
                10..10
            )



        ),
        isMutation = true
    )
}