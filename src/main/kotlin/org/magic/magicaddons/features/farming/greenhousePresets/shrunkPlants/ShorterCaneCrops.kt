package org.magic.magicaddons.features.farming.greenhousePresets.shrunkPlants

import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.ScannedPlant
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Sugarcane
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.MagicJellybean
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SOIL_Y
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.world.LevelUnloadingEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.SBLocation
import org.magic.magicaddons.util.compat.RenderCompat

object ShorterCaneCrops {

    private const val BOTTOM_BLOCK_Y: Int = GREENHOUSE_SOIL_Y + 1
    private const val HIGHEST_JELLYBEAN_BLOCK_Y: Int = GREENHOUSE_SOIL_Y + 11
    private const val BOTTOM_STAND_MAX_Y: Double = GREENHOUSE_SOIL_Y + 0.5

    private val DRAW_NOTHING: BlockState = Blocks.AIR.defaultBlockState()

    private val HIDEABLE_BLOCKS: Set<Block> = setOf(Blocks.SUGAR_CANE, Blocks.MELON_STEM, Blocks.WHEAT)

    private class StageLabel(val position: Vec3, val text: Component)

    private class HiddenPlantParts(
        val replacementStateByBlock: Map<BlockPos, BlockState>,
        val hiddenStandIds: List<Int>,
        val stageLabel: StageLabel?
    )

    @Volatile
    private var replacementStateByBlock: Long2ObjectMap<BlockState> = Long2ObjectMaps.emptyMap()

    private var hiddenStandIds: IntSet = IntSets.EMPTY_SET

    private var stageLabels: List<StageLabel> = emptyList()

    fun init() {
        EventBus.register(this)

        ModelLoadingPlugin.register { context ->
            context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE) { model, bakeContext ->
                if (bakeContext.state().block in HIDEABLE_BLOCKS) ShrunkPlantBlockModel(model) else model
            }
        }
    }

    @EventHandler
    fun onPlotChanged(event: PlotChangedEvent) = onRenderThread { updateHiddenPlantParts() }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) = onRenderThread { updateHiddenPlantParts() }

    @EventHandler
    fun onLevelUnloading(event: LevelUnloadingEvent) = onRenderThread { applyHiddenPlantParts(emptyList()) }

    private fun onRenderThread(action: () -> Unit) = Minecraft.getInstance().execute(action)

    fun hiddenBlockAt(pos: BlockPos): BlockState? {
        val replacementStates = replacementStateByBlock
        if (replacementStates.isEmpty()) return null
        if (pos.y <= BOTTOM_BLOCK_Y || pos.y > HIGHEST_JELLYBEAN_BLOCK_Y) return null

        return replacementStates.get(pos.asLong())
    }

    fun isDrawingAir(replacementState: BlockState): Boolean = replacementState === DRAW_NOTHING

    fun isStandHidden(entityId: Int): Boolean = hiddenStandIds.contains(entityId)

    private fun shouldShrinkCaneCrops(): Boolean = GreenhousePresets.shorterCaneCropsOn() && SBLocation.OwnGreenhouse.inside()

    fun updateHiddenPlantParts() {
        applyHiddenPlantParts(if (shouldShrinkCaneCrops()) hiddenPartsInCurrentGreenhouse() else emptyList())
    }

    private fun hiddenPartsInCurrentGreenhouse(): List<HiddenPlantParts> {
        val grid = GreenhouseData.getCurrentGrid()?.takeIf { it.isScanned() } ?: return emptyList()

        return grid.scannedPlants.mapNotNull { scannedPlant ->
            val bottomBlock = grid.getPosForSlot(scannedPlant.plant.slot)?.above() ?: return@mapNotNull null

            when (scannedPlant.plant.cropDef) {
                MagicJellybean.definition -> hiddenPartsOfJellybean(scannedPlant, bottomBlock)
                Sugarcane.definition -> hiddenPartsOfSugarCane(scannedPlant, bottomBlock)
                else -> null
            }
        }
    }

    private fun hiddenPartsOfJellybean(scannedPlant: ScannedPlant, bottomBlock: BlockPos): HiddenPlantParts {
        val stageLabel = labelForPlant(scannedPlant.plant, bottomBlock, visibleBlockCount = 1)
        val knownBlocks = scannedPlant.blocks.orEmpty()

        if (knownBlocks[bottomBlock]?.`is`(Blocks.SUGAR_CANE) != true) return HiddenPlantParts(emptyMap(), emptyList(), stageLabel)

        val blocksAbove = knownBlocks.keys.filter { it.y > bottomBlock.y }

        val standsAbove = scannedPlant.stands.orEmpty().filterIsInstance<ArmorStand>().filterNot { stand ->
            stand.y < BOTTOM_STAND_MAX_Y && PlayerUtils.getSkullHash(stand) == MagicJellybean.CANE_HASH
        }

        return HiddenPlantParts(blocksAbove.associateWith { DRAW_NOTHING }, standsAbove.map { it.id }, stageLabel)
    }

    private fun hiddenPartsOfSugarCane(scannedPlant: ScannedPlant, bottomBlock: BlockPos): HiddenPlantParts {
        val stageLabel = labelForPlant(scannedPlant.plant, bottomBlock, visibleBlockCount = 2)
        val nothingHidden = HiddenPlantParts(emptyMap(), emptyList(), stageLabel)
        val knownBlocks = scannedPlant.blocks.orEmpty()

        val canesAbove = knownBlocks.filter { (pos, state) -> pos.y > bottomBlock.y && state.`is`(Blocks.SUGAR_CANE) }.keys.sortedBy { it.y }
        if (canesAbove.isEmpty()) return nothingHidden

        val (topBlock, topState) = knownBlocks.entries.firstOrNull { it.value.`is`(Blocks.WHEAT) } ?: return nothingHidden

        val replacementStates = (canesAbove.drop(1) + topBlock).associateWith { DRAW_NOTHING } + (canesAbove.first() to topState)

        return HiddenPlantParts(replacementStates, emptyList(), stageLabel)
    }

    private fun labelForPlant(plant: Plant, bottomBlock: BlockPos, visibleBlockCount: Int): StageLabel? {
        val lowestStage = plant.lowestStage ?: return null
        val highestStage = plant.highestStage ?: return null
        val maxStage = plant.cropDef.maxStage

        val isGrowing = highestStage < maxStage
        if (!isGrowing || plant.isPlacedMutation) return null

        val stageText = if (lowestStage == highestStage) "$lowestStage" else "$lowestStage-$highestStage"
        val position = Vec3(bottomBlock.x + 0.5, (bottomBlock.y + visibleBlockCount).toDouble(), bottomBlock.z + 0.5)

        return StageLabel(position, Component.literal("$stageText / $maxStage"))
    }

    private fun applyHiddenPlantParts(hiddenParts: List<HiddenPlantParts>) {
        stageLabels = hiddenParts.mapNotNull { it.stageLabel }
        hiddenStandIds = IntOpenHashSet(hiddenParts.flatMap { it.hiddenStandIds })

        val newReplacements = Long2ObjectOpenHashMap<BlockState>()
        hiddenParts.forEach { plantParts ->
            plantParts.replacementStateByBlock.forEach { (pos, state) -> newReplacements.put(pos.asLong(), state) }
        }

        val previousReplacements = replacementStateByBlock
        if (newReplacements == previousReplacements) return

        replacementStateByBlock = newReplacements

        val changedSections = LongOpenHashSet()
        newReplacements.long2ObjectEntrySet().forEach { entry ->
            if (previousReplacements.get(entry.longKey) !== entry.value) changedSections.add(SectionPos.blockToSection(entry.longKey))
        }
        previousReplacements.keys.forEach { pos ->
            if (!newReplacements.containsKey(pos)) changedSections.add(SectionPos.blockToSection(pos))
        }

        val level = Minecraft.getInstance().level ?: return
        changedSections.forEach { section ->
            level.setSectionDirtyWithNeighbors(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section))
        }
    }

    fun submitStageLabels(poseStack: PoseStack, collector: SubmitNodeCollector, camera: CameraRenderState) {
        stageLabels.forEach { stageLabel ->
            poseStack.pushPose()
            poseStack.translate(
                stageLabel.position.x - camera.pos.x,
                stageLabel.position.y - camera.pos.y,
                stageLabel.position.z - camera.pos.z
            )
            RenderCompat.submitNameTag(collector, poseStack, stageLabel.text, camera.pos.distanceToSqr(stageLabel.position), camera)
            poseStack.popPose()
        }
    }
}
