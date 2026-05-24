package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Thunderling : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Thunderling",
        skyblockId = SkyBlockItemId.item("THUNDERLING"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        matcher = {
                            it == "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8"
                        }
                    )
                ),
                2..2
            )

        ),
        maxStage = 16,
        needsWater = false,
        isMutation = true
    )
}