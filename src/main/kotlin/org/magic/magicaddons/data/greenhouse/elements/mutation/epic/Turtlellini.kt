package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Turtlellini : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Turtlellini",
        effects = setOf(
            CropEffect.WaterRetain,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("TURTLELLINI"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "1d1bd06a6738d0da5053eae49a1362b89489d1ac004c222504536f7bcd07679d",
                        isSmall = false
                    )
                ),
                1..1
            )
        ,
            // as placed
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "1d1bd06a6738d0da5053eae49a1362b89489d1ac004c222504536f7bcd07679d",
                        isSmall = false
                    )
                ),
                1..1,
                placed = true
            )),
        needsWater = false,
        isMutation = true
    )
}