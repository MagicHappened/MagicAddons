package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import net.minecraft.core.Rotations
import org.magic.magicaddons.data.greenhouse.StandPose

object Shellfruit : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Shellfruit",
        effects = setOf(
            CropEffect.WaterRetain,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("SHELLFRUIT"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "72d802cd207f1971a2eb826f1a7477740833c920db00cd5c992176c67672dbf5" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        hashString = "72d802cd207f1971a2eb826f1a7477740833c920db00cd5c992176c67672dbf5",
                        isSmall = false
                    )
                ),
                1..1
            )

        ),
        needsWater = false,
        isMutation = true
    )
}