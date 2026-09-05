package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.pumpkinStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Choconut : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Choconut",
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("CHOCONUT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = pumpkinStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(-0.15625, 0.28125, -0.15625),
                        headRotation = Rotations(45.0F, 0.0F, 45.0F),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "2a8d74b77a0e510d058c544c7292a8844e70b9293880caffc562ce5ab5a49ad8"
                    )
                ),
                1..1,
            )

        ,
            // as placed, the same as it grows to
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = pumpkinStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.15625, 0.28125, -0.15625),
                        headRotation = Rotations(45.0f, 0.0f, 45.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "2a8d74b77a0e510d058c544c7292a8844e70b9293880caffc562ce5ab5a49ad8",
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