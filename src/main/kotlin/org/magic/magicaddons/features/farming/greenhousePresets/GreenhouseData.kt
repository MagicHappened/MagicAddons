package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller
import kotlin.math.abs

object GreenhouseData {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    private const val BUILD_OFFSET = 43
    private const val GRID_SIZE = 10

    val knownGreenhouseIds = mutableSetOf<Int>()
    val grids = mutableListOf<GreenhouseGrid>()

    var removedElementByAttack: ElementRuntimeState? = null

    private val elementsBySoil: Map<Block, List<CropDefinitionProvider>> =
        GreenhouseElementFactory.getAllFactories()
            .map { factory -> factory() }
            .flatMap { provider ->
                provider.definition.requiredSoil.map { soil ->
                    soil to provider
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )

    private var plantDiagnosticListeningElement: ElementRuntimeState? = null
    private var placedCrop: Pair<CropDefinition, BlockPos>? = null



    private fun initKnownIds() {
        if (!knownGreenhouseIds.isEmpty()) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach {
            if (it.data?.isGreenhouse == true) {
                knownGreenhouseIds.add(it.id)
                val grid = GreenhouseGrid()
                grid.plot = it
                grids.add(grid)
            }
        }
    }

    private fun scanGridData() {
        if (knownGreenhouseIds.isEmpty()) return

        val plot = PlotAPI.getCurrentPlot() ?: return

        val grid = grids.find { it.plot?.id == plot.id } ?: return //isnt greenhouse

        if (grid.state.initialized && !grid.state.needsUpdate) return
        grid.createSlotData()
        setPlantData(grid)
        grid.state.initialized = true
        grid.state.needsUpdate = false
        grid.state.lastUpdateTimestamp = System.currentTimeMillis()


        ChatUtils.sendWithPrefix("Successfully scanned data for ${plot.id}")
    }



    fun isInitialized(grid: GreenhouseGrid): Boolean {
        return grid.state.initialized
    }

    fun getCurrentGrid(): GreenhouseGrid? {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return grids.find { it.plot?.id == plotId }
    }

    fun clearAllData() {
        knownGreenhouseIds.clear()
        grids.clear()
    }

    private fun setPlantData(grid: GreenhouseGrid) {
        val visitedSlots = Array(GRID_SIZE) { BooleanArray(GRID_SIZE) }

        val level = Minecraft.getInstance().level ?: return
        val buildableArea = grid.plot?.getBuildableArea() ?: return

        val stands = level.getEntities(null, buildableArea)
            .filterIsInstance<ArmorStand>()
        val remainingStands = stands.toMutableList()

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (visitedSlots[y][x]) continue

                val slot = grid.getSlot(x, y) ?: continue

                val runtime = findElementAtSlot(grid, slot, remainingStands) ?: continue


                val def = runtime.cropDef
                if (runtime.cropDef.name == "Fire"){
                    runtime.renderOverride = {graphics, x, y, width, height ->
                        val sprite = ScreenUtil.getSpriteForState(Blocks.FIRE.defaultBlockState(),Direction.NORTH)
                        graphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            sprite,
                            x,
                            y,
                            width,
                            height
                        )
                    }
                }

                remainingStands.removeAll((runtime.standEntities ?: emptyList()).toSet())

                if (x + def.footprint.width > GRID_SIZE ||
                    y + def.footprint.height > GRID_SIZE
                ) continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }


                grid.elementInstances.add(
                    GreenhouseElementInstance(
                        elementId = def.skyblockId?.toString() ?: def.name,
                        slot = slot,
                        growthStage = runtime.growthStage
                    )
                )

                grid.elements.add(runtime)
            }
        }
    }

    private fun findElementAtSlot(
        grid: GreenhouseGrid,
        slot: GreenhouseSlot,
        remainingStands: MutableList<ArmorStand>
    ): ElementRuntimeState? {

        val soil = slot.placedBlock?.block ?: return null
        val candidates = elementsBySoil[soil] ?: return null
        val origin = grid.getPosForSlot(slot) ?: return null

        var bestDef: CropDefinition? = null
        var bestGrowth: GrowthStageInfo? = null
        var bestScore = -1
        var bestUsedStands: List<Entity>? = null
        var bestBlocks: Map<BlockPos, BlockState>? = null

        for (candidate in candidates) {
            val def = candidate.definition

            val stages = def.stageDefs.flatMap {
                when (it) {
                    is CropStagePattern -> it.expand()
                    is CropStage -> listOf(it)
                }
            }

            for (stage in stages) {
                val result = stage.matchesStage(origin, remainingStands)

                if (!result.matched) continue
                if (result.score <= bestScore) continue

                bestScore = result.score
                bestDef = def
                bestUsedStands = result.usedStands
                bestBlocks = result.matchedBlocks

                val range = stage.stageRange
                bestGrowth = if (range.first == range.last) {
                    GrowthStageInfo.Known(range.first)
                } else {
                    GrowthStageInfo.Estimated(range)
                }
            }
        }

        if (bestDef != null) {
            val runtime = ElementRuntimeState(
                cropDef = bestDef,
                origin = slot,
                growthStage = bestGrowth,
                waterLevel = null,
                standEntities = bestUsedStands,
                blocksMap = bestBlocks
            )

            return runtime
        }

        return null
    }


    fun matchesWithRotation(
        actual: Vec3,
        expected: Vec3,
        allowRotation: Boolean
    ): Boolean {
        if (!allowRotation) {
            return isClose(actual, expected)
        }

        val rotations = listOf(
            expected,
            Vec3(-expected.z, expected.y, expected.x),
            Vec3(-expected.x, expected.y, -expected.z),
            Vec3(expected.z, expected.y, -expected.x)
        )

        return rotations.any { rotated ->
            isClose(actual, rotated)
        }
    }

    private fun isClose(a: Vec3, b: Vec3, epsilon: Double = 0.01): Boolean {
        return abs(a.x - b.x) < epsilon &&
                abs(a.y - b.y) < epsilon &&
                abs(a.z - b.z) < epsilon
    }


    fun Plot.getBuildableArea(): AABB {
        val box = this.aabb
        val minX = box.minX + BUILD_OFFSET
        val minZ = box.minZ + BUILD_OFFSET

        return AABB(
            minX, box.minY,
            minZ,
            minX + GRID_SIZE, box.maxY,
            minZ + GRID_SIZE
        )
    }




    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!baseSetting.value) return
        initKnownIds()
        scanGridData()
    }

    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onInventory(event: ContainerInitializedEvent) {
        if (event.title != "Crop Diagnostics") {
            plantDiagnosticListeningElement = null
            return
        }
        if (plantDiagnosticListeningElement == null) return
        val realItems = event.containerItems.filter { !it.isSkyblockFiller() }
        val identifyStack = realItems[0]
        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null
        if (useNameFallback) {
            if (!identifyStack.getLore().any { it.string.contains("Base Crop") }) {
                ChatUtils.sendWithPrefix("Found Base Crop: $identifyStack")
                return
            }
        }


    }


    @EventHandler
    fun onBlockBreak(event: OnBlockDestroyedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return



        val pos = event.pos
        val blockCenter = Vec3.atCenterOf(pos)

        if (grid.plot?.aabb?.contains(blockCenter) != true) return

        val slot = grid.getSlotAt(pos)
        if (slot != null) {
            slot.placedBlock = Blocks.AIR.defaultBlockState()
            grid.setSlot(slot)
            return
        }

        val iterator = grid.elements.iterator()

        while (iterator.hasNext()) {
            val element = iterator.next()

            val blocksMap = element.blocksMap ?: continue

            if (!blocksMap.containsKey(pos)) continue

            val originSlot = element.origin

            iterator.remove()

            grid.elementInstances.removeIf { instance ->
                instance.slot == originSlot
            }

            break
        }
    }

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return

        if (event.target !is ArmorStand) return
        // for now just remove the element later add cancellation with layouts

        var removedElement: ElementRuntimeState? = null
        grid.elements.removeIf { element ->

            val removed = (element.standEntities ?: return@removeIf false).any {
                it == event.target
            }
            if (removed) {
                removedElement = element
            }
            removed
        }
        if (removedElement != null) {
            grid.elementInstances.removeIf {
                it.slot == removedElement
            }
        }
        removedElementByAttack = removedElement


    }

    @EventHandler
    fun onBlockPlaced(event: OnBlockPlacedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return

        if (placedCrop == null) return
        cropPlanted()

        placedCrop = null
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return

        val gridArea = grid.plot?.getBuildableArea() ?: return

        if (event.addedEntityList.any {
                !gridArea.contains(it.entity.position())
            }) return

        if (placedCrop == null) return
        cropPlanted()

        placedCrop = null
    }

    @EventHandler
    fun onBlockUpdated(event: OnBlockChangedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return

        val gridArea = grid.plot?.getBuildableArea() ?: return
        if (!gridArea.contains(event.packet.pos.center)) return
        val slot = grid.getSlotAt(event.packet.pos, false) ?: return
        ChatUtils.sendWithPrefix("continue filteration pls")
        if (event.packet.pos.y == 74) {
            if (event.packet.blockState.block == Blocks.FIRE) {
                val alreadyHasFire = grid.elements.any {
                    it.origin == slot && it.cropDef.name == "Fire"
                }
                if (!alreadyHasFire){
                    val fireRuntime = FireElement().getFireAtSlot(
                        slot,
                        mapOf(event.packet.pos to event.packet.blockState))
                    grid.elements.add(fireRuntime)
                }
                return
            } else {

                //todo fires a lot more than necessary so need block difference detection and
                // another method for entity difference detection, and that triggers another reinit
            }
        }

        if (event.packet.pos.y != 73) return

        slot.placedBlock = event.packet.blockState
        grid.setSlot(slot)
    }

    private val waterCanIds = setOf(
        "HYDRO_CAN_1000",
        "HYDRO_CAN_TURBO_2000",
        "HYDRO_CAN_ULTRA_3000",
        "AQUAMASTER_X",
        "AQUAMASTER_HYDROMAX"

    )

    @EventHandler
    fun onInteractEntity(event: OnInteractEntityEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return

        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return
        val standTarget = event.target as? ArmorStand ?: return

        if (mainHandId.id == "item:plant_diagnostics_tool") {
            tryGetDiagnosticData(null, standTarget, grid)
            return
        }
    }


    @EventHandler
    fun onItemUse(event: OnUseEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        if (("item:" + mainHandId.id) in waterCanIds) {
            tryGetWaterCanData()
            return
        }
    }

    @EventHandler
    fun onBlockUse(event: OnBlockUseEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        val foundCrop = CropRegistry.all.firstOrNull { it.matchesId(mainHandId) }

        if (mainHandId.id == "item:plant_diagnostics_tool") {
            tryGetDiagnosticData(event.hit.blockPos, null, grid)
            return
        }
        if (("item:" + mainHandId.id) in waterCanIds) {
            tryGetWaterCanData()
            return
        }

        if (foundCrop != null) {
            val pos = event.hit.blockPos.relative(event.hit.direction)

            placedCrop = Pair(foundCrop, pos)

            return
        }


    }

    fun cropPlanted() {
        if (placedCrop == null) return
        val grid = getCurrentGrid() ?: return
        val availableStands = grid.getUnassignedArmorStands()?.toMutableList() ?: return
        val slot = grid.getSlotAt(placedCrop!!.second, false) ?: return
        val runtime = findElementAtSlot(grid, slot, availableStands)
        runtime ?: return
        if (runtime.cropDef.needsWater) {
            runtime.waterLevel = 0
        }
        grid.elements.add(runtime)
        grid.elementInstances.add(
            GreenhouseElementInstance(
                runtime.cropDef.skyblockId?.id ?: runtime.cropDef.name,
                slot,
                0, //todo add water
                runtime.growthStage
            )
        )

    }

    fun tryGetWaterCanData() {

    }

    fun tryGetDiagnosticData(hitBlock: BlockPos? = null, hitEntity: ArmorStand? = null, grid: GreenhouseGrid) {
        var hitElement: ElementRuntimeState? = null

        if (hitBlock != null) {
            hitElement = grid.elements.find {
                it.blocksMap?.keys?.contains(hitBlock) ?: return@find false
            }
        }
        if (hitEntity != null && hitElement != null) {
            hitElement = grid.elements.find {
                it.standEntities?.contains(hitEntity) ?: return@find false
            }
        }
        if (hitElement == null) {
            plantDiagnosticListeningElement = null
            return
        }

        plantDiagnosticListeningElement = hitElement
    }


}