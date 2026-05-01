package org.magic.magicaddons.data.greenhouse


import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.util.MathUtils.rotateOffset
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId

data class Footprint(val width: Int, val height: Int)

data class CropArmorStand(val offset: Vec3, val matcher: (String?) -> Boolean) //offset is defined from the soil top left block
data class CropBlockState(val offset: BlockPos, val matcher: (BlockState) -> Boolean)

data class CropStage(
    val blocks: List<CropBlockState>? = null,
    val armorStands: List<CropArmorStand>? = null,  // make sure on the matcher if its NULL it shouldnt have the respective thing on it!
    val stageRange: IntRange, // eg if its a wheat crop it CANNOT have any armor stands on it otherwise it will be considered something
    val allowRotation: Boolean = false // else at runtime (eg ashwreath having partially grown wheat block)
){
    fun matchesRotation(
        origin: Vec3,
        stands: List<ArmorStand>
    ): Boolean {
        val rotations = if (allowRotation) 0..3 else 0..0

        for (rot in rotations) {
            val allMatch = armorStands?.all { cropStand ->
                stands.any { stand ->
                    val hash = PlayerUtils.getSkinHash(
                        stand.getItemBySlot(EquipmentSlot.HEAD)
                    )

                    val relative = stand.position().subtract(origin)
                    val expected = rotateOffset(cropStand.offset, rot)

                    relative.distanceTo(expected) < 0.01 &&
                            cropStand.matcher(hash)
                }
            } ?: true

            if (allMatch) return true
        }

        return false
    }

}

abstract class GreenhouseElement {

    abstract val name: String
    abstract val skyBlockId: SkyBlockId?
    
    open val stageDefs: List<CropStage> = mutableListOf()

    open val footprint: Footprint = Footprint(1,1)
    open val requiredSoil: Block = Blocks.FARMLAND


    // abstract val buffs: todo later lol
    val needsWater: Boolean = false
    val isBaseCrop : Boolean = false
    val isMutation : Boolean = false
    val isRareCrop : Boolean = false


}