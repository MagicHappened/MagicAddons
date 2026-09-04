package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.StandPose
import net.minecraft.core.Rotations
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Squash : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Squash",
        skyblockId = SkyBlockItemId.item("SQUASH"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "36ae076649ef22f60e8511831c68fd2b6ea63c32164dab33a8aebc18ff2a54c8" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            // placed rather than grown, so it has the one look it is put down with
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
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
        isRareCrop = true
    )
}