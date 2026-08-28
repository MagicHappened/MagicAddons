package org.magic.magicaddons.features.farming.greenhousePresets

import java.util.UUID
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.Level
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.VoxelShape
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.render.WorldRender
import org.magic.magicaddons.util.ChatUtils

/**
 * Shows the player how to build a layout, one job at a time.
 *
 * The soil comes first and alone. A greenhouse is laid out on a single row of ground, and until
 * that row is right nothing planted on it can be. Only once every slot holds the ground its crop
 * wants does the plan move on to the crops themselves, each drawn at the first stage it grows
 * through, since that is what a freshly planted one looks like.
 *
 * Two things are ever drawn: a block that should be there and is not, drawn as a ghost of itself,
 * and a block that is in the way, outlined in red so it can be found and removed.
 */
object LayoutRenderState {
    init {
        EventBus.register(this)
    }

    /**
     * What a marked block is being told to do, which is the whole message: red is the wrong block
     * and has to be swapped, orange is the right sort of block in the wrong state and only needs
     * working on, purple is a block that should not be there at all, and blue is a block that
     * should be there and is not.
     */
    private enum class Mark(val color: Int) {
        Wrong(0xFFFF3333.toInt()),
        Adjust(0xFFFF9922.toInt()),
        Remove(0xFFAA44EE.toInt()),
        Missing(0xFF3399FF.toInt())
    }

    /** Enough colour to read the mark through, little enough to see the block under it. */
    private const val FILL_ALPHA: Int = 0x4D

    /**
     * How solid a ghosted block is drawn.
     *
     * Far more than a mark's fill, because the two fade differently: a fill is one flat colour laid
     * over a block, while a ghost is the block's own texture multiplied by the tint and then faded,
     * so the same figure that reads clearly as a fill leaves a model barely there.
     */
    private const val GHOST_ALPHA: Int = 0xC0

    /**
     * The wash over a ghosted block. Pale on purpose: the colour multiplies the block's texture, so
     * a saturated blue would hold its red and green near zero and drain the block of its own colour
     * rather than tinting it, leaving dirt as a dark smear instead of recognisably dirt.
     */
    private const val GHOST_TINT: Int = 0xFFB8CCFF.toInt()

    /** The blue of a missing block, worn by the armor stands a ghosted crop is made of. */
    const val GHOST_STAND_TINT: Int = 0xC0B8CCFF.toInt()

    /**
     * Ground a hoe turns into other ground. A dirt where farmland belongs is not the wrong block,
     * it is the right one left untilled, and saying so in red would send the player digging.
     */
    private val TILLABLE: Set<Block> = setOf(
        Blocks.DIRT,
        Blocks.GRASS_BLOCK,
        Blocks.COARSE_DIRT,
        Blocks.ROOTED_DIRT,
        Blocks.DIRT_PATH,
        Blocks.FARMLAND
    )

    /** The same warning worn by an entity, which is tinted rather than outlined. */
    const val RED_TINT: Int = 0x90FF0000.toInt()


    /** Which half of the job the player is on. */
    enum class Phase {
        Soil,
        Crops
    }

    @Volatile
    var phase: Phase = Phase.Soil
        private set

    /** Blocks to be dealt with, by the shape they occupy and what is wrong with them. */
    @Volatile
    private var marks: Map<BlockPos, Pair<VoxelShape, Mark>> = emptyMap()

    /** Blocks that have to be placed, as they would look once they are. */
    @Volatile
    private var ghosts: Map<BlockPos, BlockState> = emptyMap()

    /**
     * Armor stands standing where a crop has to go. They are tinted rather than outlined, since an
     * entity is rendered one at a time and can simply be drawn a different colour.
     */
    @Volatile
    var badStandsUUID: Set<UUID> = emptySet()
        private set

    /** The stands a ghosted crop is made of, drawn as part of showing what to plant. */
    @Volatile
    var ghostStands: List<ArmorStand> = emptyList()
        private set

    /** Crops with no first stage described yet, so each is only ever mentioned once. */
    private val reportedMissingStage = mutableSetOf<String>()

    /**
     * Draws the plan, from the frame's own render pass so the camera it is placed against is the
     * one the frame is actually drawn with.
     */
    fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        marks.forEach { (pos, mark) ->
            WorldRender.mark(
                poseStack, collector, cameraPos, pos, mark.first, mark.second.color, FILL_ALPHA
            )
        }

        ghosts.forEach { (pos, state) ->
            WorldRender.ghost(
                poseStack, collector, cameraPos, pos, state,
                GHOST_TINT, Mark.Missing.color, GHOST_ALPHA
            )
        }
    }

    /** Starts over on the plan of whichever greenhouse the player is in. */
    fun show() {
        phase = Phase.Soil
        reportedMissingStage.clear()

        refresh()
    }

    fun hide() {
        marks = emptyMap()
        ghosts = emptyMap()
        badStandsUUID = emptySet()
        ghostStands = emptyList()
        reportedMissingStage.clear()
    }

    /**
     * Works out what to draw from what the plot holds right now. Cheap enough to run whenever the
     * plot changes, and has to, since the whole point is to keep up with the player building.
     */
    fun refresh() {
        val grid = GreenhouseData.getCurrentGrid()

        // the plan is whatever this greenhouse was given, so standing in another one shows that
        // one's plan or nothing, rather than carrying the last one around the garden
        val layout = grid?.state?.assignedLayout

        if (grid == null || layout == null) {
            hide()
            return
        }

        val level = Minecraft.getInstance().level ?: return

        val marks = mutableMapOf<BlockPos, Pair<VoxelShape, Mark>>()
        val ghosts = mutableMapOf<BlockPos, BlockState>()
        val badStands = mutableSetOf<UUID>()
        val ghostStands = mutableListOf<ArmorStand>()

        var soilComplete = true

        layout.slots.forEach { slot ->
            val wanted = slot.placedBlock ?: return@forEach
            val pos = grid.getPosForSlotCoords(slot.x, slot.y) ?: return@forEach

            if (!compare(level, pos, wanted, marks, ghosts)) soilComplete = false
        }

        phase = if (soilComplete) Phase.Crops else Phase.Soil

        if (soilComplete) {
            layout.elementInstances.forEach { instance ->
                // already growing there, so there is nothing to plan and nothing in the way of it
                if (grid.elements.any { it.instance.slot.isCoordsEqual(instance.slot) }) {
                    return@forEach
                }

                val soil = grid.getPosForSlotCoords(instance.slot.x, instance.slot.y)
                    ?: return@forEach

                val stage = firstStageOf(instance.cropDef) ?: return@forEach
                val render = stage.toRenderData(level, soil, instance.cropDef.footprint)

                render.blockMap.forEach { (pos, state) ->
                    compare(level, pos, state, marks, ghosts)
                }

                ghostStands.addAll(render.stands)

                // a stand already standing in the crop's space is in the way of it
                val footprint = instance.cropDef.footprint
                val space = AABB(
                    soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
                    (soil.x + footprint.width).toDouble(),
                    (soil.y + CROP_HEIGHT).toDouble(),
                    (soil.z + footprint.height).toDouble()
                )

                level.getEntitiesOfClass(ArmorStand::class.java, space)
                    .forEach { badStands.add(it.uuid) }
            }
        }

        this.marks = marks
        this.ghosts = ghosts
        this.badStandsUUID = badStands
        this.ghostStands = ghostStands
    }

    /**
     * Says what is wrong at [pos] given that [wanted] belongs there, filing it under the mark that
     * tells the player what to do about it. Returns whether the spot is already as it should be.
     */
    private fun compare(
        level: Level,
        pos: BlockPos,
        wanted: BlockState,
        marks: MutableMap<BlockPos, Pair<VoxelShape, Mark>>,
        ghosts: MutableMap<BlockPos, BlockState>
    ): Boolean {
        val standing = level.getBlockState(pos)

        if (standing == wanted) return true

        // nothing belongs here, so anything standing here is in the way
        if (wanted.isAir) {
            if (standing.isAir) return true

            marks[pos] = standing.getShape(level, pos) to Mark.Remove
            return false
        }

        if (standing.isAir) {
            ghosts[pos] = wanted
            return false
        }

        // the same block in the wrong state, or ground that only wants working on, is not a block
        // to dig out and replace
        val adjustable = standing.block == wanted.block ||
                (standing.block in TILLABLE && wanted.block in TILLABLE)

        marks[pos] = standing.getShape(level, pos) to if (adjustable) Mark.Adjust else Mark.Wrong
        return false
    }

    /**
     * The stage a crop is at the moment it goes in the ground.
     *
     * A crop nobody has described a first stage for is skipped and named once, since a plan that
     * silently leaves a plant out is worse than one that says which plant it could not draw.
     */
    private fun firstStageOf(definition: CropDefinition): CropStage? {
        val stage = definition.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .filter { 1 in it.stageRange }
            .minByOrNull { it.stageRange.first }

        if (stage == null && reportedMissingStage.add(definition.name)) {
            ChatUtils.sendWithPrefix("No first stage described for ${definition.name}, skipping it.")
        }

        return stage
    }

    /** How far above the soil a crop can reach, for finding what is standing in its way. */
    private const val CROP_HEIGHT: Int = 5
}
