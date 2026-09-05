package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.StandPose
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Veilshroom : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Veilshroom",
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("VEILSHROOM"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "266754af4859ef6f0adb03e6c58e9e348a507debce6b5a7f660d1269401de674" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "266754af4859ef6f0adb03e6c58e9e348a507debce6b5a7f660d1269401de674"
                    )
                ),
                1..1
            )

        ,
            // as placed, the same as it grows to
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "266754af4859ef6f0adb03e6c58e9e348a507debce6b5a7f660d1269401de674",
                        isSmall = false
                    )
                ),
                1..1,
                placed = true
            )),
        requiredSoil = setOf(Blocks.MYCELIUM),
        needsWater = false,
        isMutation = true
    )
}