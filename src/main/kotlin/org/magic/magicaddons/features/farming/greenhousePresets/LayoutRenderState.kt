package org.magic.magicaddons.features.farming.greenhousePresets

import java.util.UUID
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.Level
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.Footprint
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.FarmlandBlock
import net.minecraft.world.level.block.state.properties.IntegerProperty
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

    /**
     * The glow around a ghosted stand's head. Strong blue rather than the pale wash the model
     * takes, since an outline is drawn as itself rather than multiplied over a texture.
     */
    const val GHOST_OUTLINE_COLOR: Int = 0xFF3399FF.toInt()

    /**
     * Ground a hoe turns into other ground. A dirt where farmland belongs is not the wrong block,
     * it is the right one left untilled, and saying so in red would send the player digging.
     */
    /** State a plan does not care about, because nothing the player does decides it. */
    private val IGNORED_PROPERTIES: List<IntegerProperty> = listOf(FarmlandBlock.MOISTURE)

    /**
     * Blocks whose state is the world's business rather than the player's.
     *
     * Fire keeps an age it burns through and a face for every neighbour it is leaning on, and both
     * change constantly on their own. Comparing them made a fire that was exactly where the plan
     * wanted it read as the wrong state most of the time and the right one whenever its age came
     * back around to zero, which is the flicker. Nobody places fire in a particular state, so for
     * these the block being there at all is the whole question.
     */
    private val STATE_IS_NOT_OURS: Set<Block> = setOf(Blocks.FIRE)

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
                // the plant the player is told to grow is the one they have to place. What the
                // layout grows towards appears on its own once the ingredients are right, so
                // planning it would be asking for the thing being asked for
                if (instance.slot.slotMark == LayoutSlot.Marking.Target) return@forEach

                // already growing there, so there is nothing to plan and nothing in the way of it
                if (grid.elements.any { it.instance.slot.isCoordsEqual(instance.slot) }) {
                    return@forEach
                }

                val soil = grid.getPosForSlotCoords(instance.slot.x, instance.slot.y)
                    ?: return@forEach

                // a plant standing at a stage nobody has described yet matches nothing and drops
                // out of the scan, and was then planned for as though its slot were bare, so a
                // ghost appeared inside the plant and flickered away on the next scan that did
                // match it. Anything growing on the soil is a plant, described or not
                if (isOccupied(level, soil, instance.cropDef.footprint)) return@forEach

                val stage = ghostStageOf(instance.cropDef) ?: return@forEach
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
     * Whether anything at all is growing on the soil a plan wants to fill.
     *
     * Deliberately blunt: a block above the soil, or a stand standing in the space, is a plant
     * whether or not the definitions can name it. Being wrong here draws nothing where something
     * could have been drawn, which is quieter than drawing a plan through a plant already there.
     */
    private fun isOccupied(level: Level, soil: BlockPos, footprint: Footprint): Boolean {
        for (offsetX in 0 until footprint.width) {
            for (offsetY in 0 until footprint.height) {
                if (!level.getBlockState(soil.offset(offsetX, 1, offsetY)).isAir) return true
            }
        }

        val space = AABB(
            soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
            (soil.x + footprint.width).toDouble(),
            (soil.y + CROP_HEIGHT).toDouble(),
            (soil.z + footprint.height).toDouble()
        )

        return level.getEntitiesOfClass(ArmorStand::class.java, space).any { !it.isMarker }
    }

    /**
     * Whether two states are the same as far as a plan is concerned.
     *
     * A state carries things the player has no say over and no reason to be told about. Farmland
     * grows damp when it is near water, so a watered plot differs from a dry one in every slot, and
     * calling that out would paint a finished greenhouse orange over something nobody can fix.
     */
    private fun BlockState.sameEnoughAs(other: BlockState): Boolean {
        if (this == other) return true
        if (block != other.block) return false

        if (block in STATE_IS_NOT_OURS) return true

        // every ignored property is copied across before comparing, so what is left is only the
        // state a player actually chose
        val normalised = IGNORED_PROPERTIES.fold(this) { state, property ->
            if (state.hasProperty(property) && other.hasProperty(property)) {
                state.setValue(property, other.getValue(property))
            } else {
                state
            }
        }

        return normalised == other
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

        if (standing.sameEnoughAs(wanted)) return true

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
     * What a crop looks like the moment it is put down, which is not the same question for the two
     * kinds of crop.
     *
     * A base crop is planted as a seed and grows, so it starts at its first stage. A mutation is
     * placed already grown and never grows further, so it looks like its last stage from the moment
     * it goes in, and drawing it as a seedling would show the player something they will never see.
     *
     * A crop nobody has described that stage for is skipped and named once, since a plan that
     * silently leaves a plant out is worse than one that says which plant it could not draw.
     */
    private fun ghostStageOf(definition: CropDefinition): CropStage? {
        val stages = definition.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }

        val stage = if (definition.isMutation) {
            stages.filter { definition.maxStage in it.stageRange }
                .maxByOrNull { it.stageRange.last }
        } else {
            stages.filter { 1 in it.stageRange }
                .minByOrNull { it.stageRange.first }
        }

        if (stage == null && reportedMissingStage.add(definition.name)) {
            val which = if (definition.isMutation) "last" else "first"

            ChatUtils.sendWithPrefix("No $which stage described for ${definition.name}, skipping it.")
        }

        return stage
    }

    /** How far above the soil a crop can reach, for finding what is standing in its way. */
    private const val CROP_HEIGHT: Int = 5
}
