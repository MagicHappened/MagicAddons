package org.magic.magicaddons.data.greenhouse.crops

import kotlin.math.abs
import kotlin.math.floor
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils

data class StageMatch(
    val score: Int,
    val usedStands: List<Entity>,
    val matchedBlocks: Map<BlockPos, BlockState>,
    val matchingHeadPoses: Int
)

data class StageStand(
    /** feet position from the footprint centre at soil height */
    val offset: Vec3,
    val isSmall: Boolean,
    val headRotation: Rotations? = null,
    val xRotation: Float? = null,
    val yRotation: Float? = null,
    val hashString: String? = null,
    val nameContains: String? = null,
    /** a held item other than a skull, as "minecraft:gold_block" */
    val heldItemId: String? = null,
    val itemSlot: EquipmentSlot = EquipmentSlot.HEAD,
) {
    companion object {
        fun atOffsets(
            offsets: List<Vec3>,
            isSmall: Boolean,
            rotations: List<Rotations>? = null,
            xRotations: List<Float>? = null,
            yRotations: List<Float>? = null,
            hashString: String? = null,
            nameContains: String? = null,
            heldItemId: String? = null,
            itemSlot: EquipmentSlot = EquipmentSlot.HEAD
        ): List<StageStand> = offsets.mapIndexed { i, offset ->
            StageStand(
                offset = offset,
                isSmall = isSmall,
                headRotation = rotations?.getOrNull(i),
                xRotation = xRotations?.getOrNull(i),
                yRotation = yRotations?.getOrNull(i),
                hashString = hashString,
                nameContains = nameContains,
                heldItemId = heldItemId,
                itemSlot = itemSlot
            )
        }
    }
}

data class StageBlock(
    val offset: BlockPos,
    val blockState: BlockState,
    val required: Boolean = true
){

    companion object {
        fun atPositions(
            positions: List<BlockPos>,
            blockState: BlockState,
            required: Boolean = true
        ): List<StageBlock> = positions.map { StageBlock(it, blockState, required) }
    }
}

open class CropStage(
    val blocks: List<StageBlock>? = null,
    val armorStands: List<StageStand>? = null,
    val stageRange: IntRange,
    val traits: Map<String, Int> = emptyMap(),
    val readers: List<StandReader> = emptyList()
) {

    /** the highest value when several stands are found */
    fun readValues(stands: List<ArmorStand>): Map<String, Int> = readers.mapNotNull { reader ->
        stands.filter { reader.matches(it) }
            .mapNotNull { reader.read(it) }
            .maxOrNull()
            ?.let { reader.key to it }
    }.toMap()

    class StandCache {
        class CachedStand(
            val position: Vec3,
            val skullHash: String?,
            val name: String?,
            val stand: ArmorStand
        ) {
            private val items = HashMap<EquipmentSlot, String?>()

            fun itemIn(slot: EquipmentSlot): String? =
                items.getOrPut(slot) { EntityUtils.itemIdIn(stand, slot) }
        }

        private val cachedStands = HashMap<Int, CachedStand>()

        fun lookUp(stand: ArmorStand): CachedStand = cachedStands.getOrPut(stand.id) {
            CachedStand(
                stand.position(),
                PlayerUtils.getSkullHash(stand),
                stand.customName?.string,
                stand
            )
        }
    }

    fun matchesStage(
        origin: BlockPos,
        remainingStands: List<ArmorStand>,
        footprint: Footprint,
        rotatesWithPlot: Boolean = true,
        ignoreStemAge: Boolean = false,
        standCache: StandCache = StandCache()
    ): StageMatch? {
        val level = Minecraft.getInstance().level ?: return null

        var score = 0
        val matchedBlocks = mutableMapOf<BlockPos, BlockState>()

        this.blocks?.forEach { recordedBlock ->
            if (!recordedBlock.required) return@forEach

            val blockPos = origin.offset(recordedBlock.offset)
            val worldBlockState = level.getBlockState(blockPos)

            if (!blocksMatch(worldBlockState, recordedBlock.blockState, ignoreStemAge)) return null

            matchedBlocks[blockPos] = worldBlockState
            score += 1
        }
        val footprintCenter = Vec3(
            origin.x + footprint.width / 2.0,
            origin.y.toDouble(),
            origin.z + footprint.height / 2.0
        )

        val quarterTurns = if (rotatesWithPlot) WorldRotation.quarterTurnsAt(origin.x, origin.z) else 0
        val matchedStands = mutableListOf<Entity>()

        for (recordedStand in this.armorStands.orEmpty()) {
            val expectedOffset = WorldRotation.turned(recordedStand.offset, quarterTurns)

            val matchingStand = remainingStands.firstOrNull { entity ->
                val cachedStand = standCache.lookUp(entity)

                offsetsMatch(cachedStand.position.subtract(footprintCenter), expectedOffset) &&
                        (recordedStand.hashString?.let { it == cachedStand.skullHash } ?: true) &&
                        (recordedStand.nameContains?.let { cachedStand.name?.contains(it) == true } ?: true) &&
                        (recordedStand.heldItemId?.let { it == cachedStand.itemIn(recordedStand.itemSlot) } ?: true)
            } ?: return null

            matchedStands.add(matchingStand)
            score += 2
        }

        val matchingHeadPoses = this.armorStands.orEmpty().zip(matchedStands).count { (recordedStand, stand) ->
            val recordedHeadPose = recordedStand.headRotation ?: return@count false
            (stand as? ArmorStand)?.headPose?.let { headPosesMatch(it, recordedHeadPose) } == true
        }

        return StageMatch(
            score = score,
            usedStands = matchedStands,
            matchedBlocks = matchedBlocks,
            matchingHeadPoses = matchingHeadPoses
        )
    }

    /** with [ignoreStemAge] a stem matches at any age */
    private fun blocksMatch(world: BlockState, recorded: BlockState, ignoreStemAge: Boolean): Boolean =
        world == recorded || (ignoreStemAge && recorded.block is StemBlock && world.block == recorded.block)

    private fun headPosesMatch(worldPose: Rotations, recordedPose: Rotations): Boolean =
        abs(Mth.wrapDegrees(worldPose.x() - recordedPose.x())) < HEAD_POSE_TOLERANCE_DEGREES &&
                abs(Mth.wrapDegrees(worldPose.y() - recordedPose.y())) < HEAD_POSE_TOLERANCE_DEGREES &&
                abs(Mth.wrapDegrees(worldPose.z() - recordedPose.z())) < HEAD_POSE_TOLERANCE_DEGREES

    private fun offsetsMatch(actual: Vec3, expected: Vec3): Boolean {
        return abs(actual.x - expected.x) < OFFSET_TOLERANCE &&
                abs(actual.y - expected.y) < OFFSET_TOLERANCE &&
                abs(actual.z - expected.z) < OFFSET_TOLERANCE
    }

    /** the stands and blocks that stand in for this stage where nothing is planted */
    fun hologramAt(
        level: Level,
        baseBlock: BlockPos,
        footprint: Footprint,
        standPoses: Map<String, StandPose> = emptyMap(),
        rotatesWithPlot: Boolean = true
    ): HologramStage {
        val renderStands = mutableListOf<ArmorStand>()
        val blockMap = mutableMapOf<BlockPos, BlockState>()

        val quarterTurns = if (rotatesWithPlot) WorldRotation.quarterTurnsAt(baseBlock.x, baseBlock.z) else 0
        val footprintCenter = Vec3(
            baseBlock.x + footprint.width / 2.0,
            baseBlock.y.toDouble(),
            baseBlock.z + footprint.height / 2.0
        )

        blocks?.forEach { recordedBlock ->
            blockMap[baseBlock.offset(recordedBlock.offset)] = recordedBlock.blockState
        }
        armorStands?.forEach { recordedStand ->
            val heldItem = recordedStand.hashString?.let { PlayerUtils.getItemFromHash(it) }
                ?: recordedStand.heldItemId?.let { EntityUtils.itemStackOf(it) }
                ?: return@forEach
            val turnedOffset = WorldRotation.turned(recordedStand.offset, quarterTurns)
            val stand = ArmorStand(
                level,
                footprintCenter.x + turnedOffset.x,
                footprintCenter.y + turnedOffset.y,
                footprintCenter.z + turnedOffset.z
            )

            // isSmall has no public setter
            if (recordedStand.isSmall) {
                stand.entityData.set(
                    ArmorStand.DATA_CLIENT_FLAGS,
                    ArmorStand.CLIENT_FLAG_SMALL.toByte()
                )
            }

            // rendering a held item needs an entity id the world never hands out
            stand.id = FAKE_ENTITY_ID

            stand.isInvisible = true
            // the stand's own rotation wins over the crop's standPoses
            val cropStandPose = recordedStand.hashString?.let { standPoses[it] }
            val headPose = recordedStand.headRotation
                ?: cropStandPose?.headAt(baseBlock.x, baseBlock.z, recordedStand.offset)

            headPose?.let { stand.headPose = it }
            val yaw = Mth.wrapDegrees((recordedStand.yRotation ?: cropStandPose?.yRotation ?: 0f) + 90f * quarterTurns)

            // never ticked, so the previous-tick yaws are set too
            stand.yRot = yaw
            stand.yRotO = yaw
            stand.yBodyRot = yaw
            stand.yBodyRotO = yaw
            stand.yHeadRot = yaw
            stand.yHeadRotO = yaw
            stand.xRot = recordedStand.xRotation ?: cropStandPose?.xRotation ?: 0f
            stand.setItemSlot(if (recordedStand.hashString != null) EquipmentSlot.HEAD else recordedStand.itemSlot, heldItem)
            renderStands.add(stand)
        }
        return HologramStage(
            renderStands.toList(),
            blockMap.toMap()
        )
    }


    data class HologramStage(
        val stands: List<ArmorStand>,
        val blockMap: Map<BlockPos, BlockState>
    )

    companion object {
        private const val FAKE_ENTITY_ID: Int = -1
        private const val OFFSET_TOLERANCE: Double = 0.01

        /** recorded head poses are several degrees apart at the closest */
        private const val HEAD_POSE_TOLERANCE_DEGREES: Float = 1f
    }
}

sealed interface StandPose {

    fun headAt(x: Int, z: Int, offset: Vec3): Rotations

    val xRotation: Float get() = 0f
    val yRotation: Float get() = 0f

    data class Fixed(
        val headRotation: Rotations,
        override val xRotation: Float = 0f,
        override val yRotation: Float = 0f
    ) : StandPose {
        override fun headAt(x: Int, z: Int, offset: Vec3): Rotations = headRotation
    }

    /** poses[(x + z + height) mod size], as the jellybean's canes do */
    data class Cycle(val poses: List<Rotations>) : StandPose {
        override fun headAt(x: Int, z: Int, offset: Vec3): Rotations =
            poses[Math.floorMod(x + z + floor(offset.y + 0.5).toInt(), poses.size)]
    }
}
