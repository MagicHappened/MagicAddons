package org.magic.magicaddons.data.greenhouse.crops.definitions.misc

import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.StageStand

object DevourerRoots {
    val definition = CropDefinition(
        name = "DevourerRoots",
        tier = CropTier.Other,
        skyblockId = null,
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.1875),
                        headRotation = Rotations(-45.0f, 0.0f, 0.0f),
                        hashString = "438788f3e6237fa486cc01e256496bc7a80cbc34f48935a1e1764be1ba69377a",
                        isSmall = false
                    )
                ),
                1..1
            )),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
        requiredSoil = setOf(Blocks.FARMLAND, Blocks.SOUL_SAND)

    )
}