package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Chloronite : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Chloronite",
        skyblockId = SkyBlockItemId.item("CHLORONITE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:green_stained_glass")
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashMatches = {
                            it == "98056b960ff385c20cffc3d1524500fcd3bf8c31b6dcafd8520f41dfa749dd28"
                        },
                    )
                ),
                10..10
            )

        ),
        maxStage = 10,
        isMutation = true
    )
}