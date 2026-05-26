package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object PlantBoyAdvance : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "PlantBoy Advance",
        skyblockId = SkyBlockItemId.item("PLANTBOY_ADVANCE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 6
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,1,1),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 2
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(1,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 6
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(1,1,1),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(1.0, -0.5625, 1.0),
                        matcher = {
                            it == "1822281949d048a10d54ed72cdd4c222312a86fbf946ba56aea35f5142d0ee7a"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(1.5, -0.65625, 1.0),
                        matcher = {
                            it == "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, 1.0),
                        matcher = {
                            it == "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337"
                        }
                    )
                ),
                9..9
            )
        ),
        maxStage = 12,
        footprint = Footprint(2,2),
        needsWater = false,
        isMutation = true
    )
}