package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Shadevine : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Shadevine",
        skyblockId = SkyBlockItemId.item("SHADEVINE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.75, 0.1875),
                        matcher = {
                            it == "c3c6d9dcb8fbd73de6171a2c2155314d097a9c99d09c9fce9cba068d7e5aedf7"
                        }
                    )
                ),
                1..1
                //todo check if rotation needed
            )



        ),
        requiredSoil = setOf(Blocks.FARMLAND, Blocks.SAND),
        needsWater = false,
        isMutation = true
    )
}