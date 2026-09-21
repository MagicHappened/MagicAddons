package org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cropie {
    val definition = CropDefinition(
        name = "Cropie",
        tier = CropTier.RareCrop,
        skyblockId = SkyBlockItemId.item("CROPIE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "dd01cba23ede2cd2895107f0c0258e971d2485538fe9649ef2853bd26e6232dc" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            // placed rather than grown, so it has the one look it is put down with
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "dd01cba23ede2cd2895107f0c0258e971d2485538fe9649ef2853bd26e6232dc"
                    )
                ),
                1..1,
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
    )
}