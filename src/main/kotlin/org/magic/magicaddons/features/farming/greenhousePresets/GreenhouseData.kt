package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
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

    var greenhousesInitialized = false
    val greenhouseGrids = mutableListOf<GreenhouseGrid>()
    val presetGrids = mutableListOf<GreenhouseGrid>()

    var removedElementByAttack: ElementRuntimeState? = null

    val elementsBySoil: Map<Block, List<CropDefinition>> =
        CropRegistry.all
            .flatMap { definition ->
                definition.requiredSoil.map { soil ->
                    soil to definition
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )


    private var plantDiagnosticListeningElement: ElementRuntimeState? = null
    private var placedCrop: Pair<CropDefinition, BlockPos>? = null


    private fun initKnownIds() {
        if (greenhousesInitialized) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach {
            if (it.data?.isGreenhouse == true) {
                val gridLayout = GreenhouseLayout(
                    id = "plot_${it.id}",
                    name = null
                )
                val gridState = GreenhouseGrid.GridState()

                val grid = GreenhouseGrid(gridState, gridLayout)
                grid.plot = it
                greenhouseGrids.add(grid)
            }
        }
        greenhousesInitialized = true
    }

    private fun scanGridData() {
        if (!greenhousesInitialized) return

        val plot = PlotAPI.getCurrentPlot() ?: return

        val grid = greenhouseGrids.find { it.layout.id == "plot_${plot.id}" } ?: return //isnt greenhouse
        if (grid.state.initialized && !grid.state.needsUpdate) return

        grid.plot = plot
        grid.createSlotData()
        grid.setPlantData()

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
        return greenhouseGrids.find { it.layout.id == "plot_$plotId" }
    }

    fun clearAllData() {
        greenhousesInitialized = false
        greenhouseGrids.clear()
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

        val slot = grid.getSlotAt(pos, false) ?: return
        if (event.pos.y == 73) {
            slot.placedBlock = Blocks.AIR.defaultBlockState()
            return
        }

        grid.removeMatchingBlock(pos)
    }

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        val grid = getCurrentGrid() ?: return
        if (!isInitialized(grid)) return

        if (event.target !is ArmorStand) return
        // for now just remove the element later add cancellation with layouts


        val removed = grid.removeMatchingEntity(event.target)
        removedElementByAttack = removed
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

        if (gridArea.contains(event.packet.pos.center)) {
            if (event.packet.pos.y == 74) {

                if (event.packet.blockState.block == Blocks.FIRE) {
                    val alreadyHasFire = grid.elements.any {
                        it.instance.slot == slot && it.cropDef.name == "Fire"
                    }
                    if (!alreadyHasFire) {
                        val fireRuntime = FireElement.getFireAtSlot(
                            slot,
                            mapOf(event.packet.pos to event.packet.blockState)
                        )
                        grid.elements.add(fireRuntime)
                    }
                    return
                }
            } else {
                ChatUtils.sendWithPrefix("continue filteration pls")
                //todo fires a lot more than necessary so need block difference detection and
                // another method for entity difference detection, and that triggers another reinit
            }
        }

        if (event.packet.pos.y != 73) return
        slot.placedBlock = event.packet.blockState
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
        val runtime = grid.findElementAtSlot(slot, availableStands)
        runtime ?: return
        if (runtime.cropDef.needsWater) {
            runtime.instance.waterLevel = 0
        }
        grid.addElement(runtime)
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