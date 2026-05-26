package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object MagicJellybean : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Magic Jellybean",
        skyblockId = SkyBlockItemId.item("MAGIC_JELLYBEAN"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 6
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.78125, 0.0),
                        matcher = {
                            it == "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.19677734375, 3.714111328125, -0.290771484375),
                        matcher = {
                            true
                        }
                    )
                ),
                9..9,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 7
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.21875, 0.0),
                        matcher = {
                            it == "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.59375, 0.0),
                        matcher = {
                            it == "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"
                        }
                    )
                ),
                18..18,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,3,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,4,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,5,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,6,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,7,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,8,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,9,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,10,0),
                        matcher = {
                            it.isBlock("minecraft:sugar_cane") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.21875, 0.0),
                        matcher = {
                            it == "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.78125, 0.0),
                        matcher = {
                            it == "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.78125, 0.0),
                        matcher = {
                            it == "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 2.78125, 0.0),
                        matcher = {
                            it == "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 3.78125, 0.0),
                        matcher = {
                            it == "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                        }
                    )
                ),
                120..120,
                allowRotation = true
            )



        ),
        maxStage = 120,
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}