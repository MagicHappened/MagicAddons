package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Carrot
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Potato
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Scourroot {
    val definition = CropDefinition(
        name = "Scourroot",
        tier = CropTier.Common,
        dropMultiplier = 0.17,
        effects = setOf(
            CropEffect.XpBoost,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("SCOURROOT"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "a9da3b8dcffbb5dd9708b83e54746fced475f0ee16c6c0ce4668cca7999c4d1e" to StandPose.Fixed(Rotations(45.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.75, -0.125),
                        hashString = "a9da3b8dcffbb5dd9708b83e54746fced475f0ee16c6c0ce4668cca7999c4d1e"
                    )
                ),
                1..1
            )
        ),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Potato" to 1, "Carrot" to 1))
    )
}