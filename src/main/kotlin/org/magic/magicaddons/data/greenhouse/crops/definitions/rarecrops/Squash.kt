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

object Squash {
    val definition = CropDefinition(
        name = "Squash",
        tier = CropTier.RareCrop,
        skyblockId = SkyBlockItemId.item("SQUASH"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "36ae076649ef22f60e8511831c68fd2b6ea63c32164dab33a8aebc18ff2a54c8" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            // placed rather than grown, so it has the one look it is put down with
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "36ae076649ef22f60e8511831c68fd2b6ea63c32164dab33a8aebc18ff2a54c8"
                    )
                ),
                1..1,
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
    )
}