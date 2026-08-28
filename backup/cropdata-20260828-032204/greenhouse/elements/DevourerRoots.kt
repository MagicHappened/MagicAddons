package org.magic.magicaddons.data.greenhouse.elements

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage

object DevourerRoots : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "DevourerRoots",
        skyblockId = null,
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.1875, -0.75, 0.0),
                        hashString = "438788f3e6237fa486cc01e256496bc7a80cbc34f48935a1e1764be1ba69377a"
                    )
                ),
                1..1,
                allowRotation = true
            )
        ),
        needsWater = false,
        requiredSoil = setOf(Blocks.FARMLAND, Blocks.SOUL_SAND)

    )
}