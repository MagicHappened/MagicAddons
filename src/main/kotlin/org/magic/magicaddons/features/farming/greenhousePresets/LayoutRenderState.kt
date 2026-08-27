package org.magic.magicaddons.features.farming.greenhousePresets

import java.util.UUID
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
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.render.WorldRender.ghostBlock
import org.magic.magicaddons.render.WorldRender.markBlock
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent

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
        SkyBlockAPI.eventBus.register(this)
        EventBus.register(this)
    }

    /** A block standing where something else belongs, drawn solid on its edges. */
    private const val BLOCKED_COLOR: Int = 0xFFFF3333.toInt()

    /** How much of that red fills the block itself, enough to read through. */
    private const val BLOCKED_FILL_ALPHA: Int = 0x60

    /** The same warning worn by an entity, which is tinted rather than outlined. */
    const val RED_TINT: Int = 0x90FF0000.toInt()

    /** A block that should be placed, drawn as it would look but see through. */
    const val GHOST_TINT: Int = 0x70FFFFFF

    /** Which half of the job the player is on. */
    enum class Phase {
        Soil,
        Crops
    }

    @Volatile
    var phase: Phase = Phase.Soil
        private set

    /** Blocks that have to go, by the shape they occupy, which every block has however thin it is. */
    @Volatile
    private var blocked: Map<BlockPos, VoxelShape> = emptyMap()

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

    /** The layout being built, or null while nothing is being shown. */
    @Volatile
    private var target: GreenhouseLayout? = null

    /** Crops with no first stage described yet, so each is only ever mentioned once. */
    private val reportedMissingStage = mutableSetOf<String>()

    @Subscription
    private fun onRenderWorld(event: RenderWorldEvent.AfterTranslucent) {
        val blocked = this.blocked
        val ghosts = this.ghosts

        blocked.forEach { (pos, shape) -> event.markBlock(pos, shape, BLOCKED_COLOR, BLOCKED_FILL_ALPHA) }
        ghosts.forEach { (pos, state) -> event.ghostBlock(pos, state, GHOST_TINT) }
    }

    /** Starts showing [layout], from the soil up. */
    fun show(layout: GreenhouseLayout) {
        target = layout
        phase = Phase.Soil
        reportedMissingStage.clear()

        refresh()
    }

    fun hide() {
        target = null
        blocked = emptyMap()
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
        val layout = target ?: return
        val grid = GreenhouseData.getCurrentGrid() ?: return
        val level = Minecraft.getInstance().level ?: return

        val blocked = mutableMapOf<BlockPos, VoxelShape>()
        val ghosts = mutableMapOf<BlockPos, BlockState>()
        val badStands = mutableSetOf<UUID>()
        val ghostStands = mutableListOf<ArmorStand>()

        var soilComplete = true

        layout.slots.forEach { slot ->
            val wanted = slot.placedBlock ?: return@forEach
            if (wanted.isAir) return@forEach

            val pos = grid.getPosForSlotCoords(slot.x, slot.y) ?: return@forEach
            val standing = level.getBlockState(pos)

            when {
                // already right, so there is nothing to say about it
                standing.block == wanted.block -> Unit

                // free ground, so show what goes here
                standing.isAir -> {
                    ghosts[pos] = wanted
                    soilComplete = false
                }

                // something else is in the way and has to go before anything can be planted
                else -> {
                    blocked[pos] = standing.getShape(level, pos)
                    soilComplete = false
                }
            }
        }

        phase = if (soilComplete) Phase.Crops else Phase.Soil

        if (soilComplete) {
            layout.elementInstances.forEach { instance ->
                val soil = grid.getPosForSlotCoords(instance.slot.x, instance.slot.y)
                    ?: return@forEach

                val stage = firstStageOf(instance.cropDef) ?: return@forEach
                val render = stage.toRenderData(level, soil, instance.cropDef.footprint)

                render.blockMap.forEach { (pos, state) ->
                    val standing = level.getBlockState(pos)

                    when {
                        standing.block == state.block -> Unit
                        standing.isAir -> ghosts[pos] = state
                        else -> blocked[pos] = standing.getShape(level, pos)
                    }
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

        this.blocked = blocked
        this.ghosts = ghosts
        this.badStandsUUID = badStands
        this.ghostStands = ghostStands
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
