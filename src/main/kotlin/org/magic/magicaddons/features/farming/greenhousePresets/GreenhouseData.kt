package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.FarmingDebug
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.toFunctionString
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.ServerUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.abs

object GreenhouseData {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    private const val BUILD_OFFSET = 43
    private const val GRID_SIZE = 10

    private val waterCanIds = setOf(
        "HYDRO_CAN_1000",
        "HYDRO_CAN_TURBO_2000",
        "HYDRO_CAN_ULTRA_3000",
        "AQUAMASTER_X",
        "AQUAMASTER_HYDROMAX"
    )

    var checkGreenhouses = false
    var greenhousesInitialized = false
    var greenhouseGrids = mutableListOf<GreenhouseGrid>()
    var presetGrids = mutableListOf<GreenhouseLayout>()
    var miscInfo = MiscGreenhouseInfo()
    var lastGridLayouts = mutableListOf<GreenhouseLayout>()

    var currentPreset: GreenhouseLayout? = null
    var currentGridIndex: Int = 0

    var lastCheckTime: Instant? = null
    var lastServerTick: Long? = null

    var removedElementByAttack: ElementRuntimeState? = null
    private var placedCrop: Pair<CropDefinition, BlockPos>? = null


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

    private var plantDiagnosticHitBaseBlock: BlockPos? = null
    private var plantDiagnosticListeningElement: ElementRuntimeState? = null
    // ^^ temp


    private fun initKnownIds() {
        if (checkGreenhouses) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach { plot ->
            if (plot.data?.isGreenhouse != true) return@forEach
            val existingGrid = greenhouseGrids.find { "plot_${plot.id}" == it.layout.id }
            existingGrid ?: run {
                val gridLayout = GreenhouseLayout(
                    id = "plot_${plot.id}",
                    name = "unnamed"
                )
                val gridState = GreenhouseGrid.GridState(
                    null,
                    true,
                    null,
                    false
                )

                val grid = GreenhouseGrid(gridState, gridLayout)
                grid.plot = plot
                greenhouseGrids.add(grid)
                return@forEach
            }
            existingGrid.plot = plot
        }
        greenhousesInitialized = true
        checkGreenhouses = true
    }

    private fun scanGridData() {
        if (!greenhousesInitialized) return
        val plot = PlotAPI.getCurrentPlot() ?: return

        val grid = getCurrentGrid() ?: return
        if (grid.state.hasRuntimeReferences && !grid.state.needsUpdate) return

        grid.plot = plot

        //gather in world data
        //todo not only need to change this to find diffs
        if ((grid.state.pendingGrowthTicks ?: -1) > 0) {

        }

        grid.createSlotData()
        grid.setPlantData()

        // after grid update
        grid.state.hasRuntimeReferences = true
        grid.state.needsUpdate = false
        grid.state.lastUpdateTimestamp = Instant.now()
        grid.state.pendingGrowthTicks = 0

        ChatUtils.sendWithPrefix("Successfully scanned data for ${plot.id}")
    }


    fun getCurrentGrid(): GreenhouseGrid? {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return greenhouseGrids.find { it.layout.id == "plot_$plotId" }
    }

    fun Float.isCardinalYaw(): Boolean {
        val normalized = ((this % 360f) + 360f) % 360f

        return abs(normalized - 0f) < 0.1f ||
                abs(normalized - 90f) < 0.1f ||
                abs(normalized - 180f) < 0.1f ||
                abs(normalized - 270f) < 0.1f
    }

    fun String.parseDurationToMs(): Long {
        var totalMs = 0L

        val regex = Regex("(\\d+)([dhms])")

        regex.findAll(this).forEach { match ->
            val value = match.groupValues[1].toLong()
            val unit = match.groupValues[2]

            totalMs += when (unit) {
                "d" -> value * 24 * 60 * 60 * 1000
                "h" -> value * 60 * 60 * 1000
                "m" -> value * 60 * 1000
                "s" -> value * 1000
                else -> 0L
            }
        }

        return totalMs
    }

    fun Instant.toReadableDuration(from: Instant = Instant.now()): String {
        var seconds = abs(Duration.between(this, from).seconds)

        val days = seconds / 86400
        seconds %= 86400

        val hours = seconds / 3600
        seconds %= 3600

        val minutes = seconds / 60
        seconds %= 60

        val parts = mutableListOf<String>()

        if (days > 0) parts += "${days}d"
        if (hours > 0) parts += "${hours}h"
        if (minutes > 0) parts += "${minutes}m"
        if (seconds > 0 || parts.isEmpty()) parts += "${seconds}s"

        return parts.joinToString(" ")
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

    fun computeNextAvailableId(): Int {
        val usedIds = presetGrids
            .mapNotNull {
                it.id.removePrefix("preset_").toIntOrNull()
            }
            .toSet()

        var nextId = 1

        while (nextId in usedIds) {
            nextId++
        }

        return nextId
    }

    fun checkForUpdate() {
        if (!greenhousesInitialized) return
        if (warnUnknownValues(!miscInfo.shouldIgnoreWarning)) return

        val nextTick = miscInfo.nextTickTime ?: return

        val growthTickMs = computeGrowthStageTimeMs(
            getCurrentUniques().size,
            miscInfo.cropGrowthValue!!,
            miscInfo.cropSpeedUpgradeValue!!
        )

        val now = Instant.now()

        val onlineTickTracking =
            LocationAPI.island == SkyBlockIsland.GARDEN &&
                    !LocationAPI.isGuest &&
                    lastCheckTime != null

        val overdueMs = if (onlineTickTracking) {
            val currentTick = ServerUtils.totalServerTicks
            val previousTick = lastServerTick

            lastServerTick = currentTick

            if (previousTick == null) return

            val passedServerTicks = currentTick - previousTick
            if (passedServerTicks <= 0) return

            val lastCheck = lastCheckTime ?: return

            val serverMs = passedServerTicks * 50L
            val realMs = now.toEpochMilli() - lastCheck.toEpochMilli()
            val adjustmentDelta = realMs - serverMs
            miscInfo.nextTickTime =
                miscInfo.nextTickTime!!.minusMillis(adjustmentDelta)
            lastCheckTime = now

            now.toEpochMilli() -
                    miscInfo.nextTickTime!!.toEpochMilli()


        } else {
            lastCheckTime = now
            now.toEpochMilli() - nextTick.toEpochMilli()
        }

        if (overdueMs <= 0) return
        val passedGrowthTicks = (overdueMs / growthTickMs)

        if (passedGrowthTicks <= 0 && !nextTick.isBefore(now)) return
        Common.LOGGER.info("Overdue ms $overdueMs")
        Common.LOGGER.info("Overdue growth ticks $passedGrowthTicks")
        val nextTickAdvance = (passedGrowthTicks + 1) * growthTickMs
        Common.LOGGER.info("Next tick advance ms $nextTickAdvance")
        Common.LOGGER.info("Previous tick ${miscInfo.nextTickTime}")
        Common.LOGGER.info("Next tick ${miscInfo.nextTickTime!!.plusMillis((nextTickAdvance))}")
        Common.LOGGER.info("Now $now")
        miscInfo.nextTickTime =
            miscInfo.nextTickTime!!.plusMillis(
                nextTickAdvance
            )

        greenhouseGrids.forEach { grid ->
            if (onlineTickTracking && !grid.hasRuntime()) return@forEach
            val pendingTicks = grid.state.pendingGrowthTicks ?: return@forEach

            grid.state.pendingGrowthTicks = pendingTicks + passedGrowthTicks.toInt()
            grid.state.needsUpdate = true
        }
    }

    @Subscription
    fun onTick(event: TickEvent) {
        val now = Instant.now()
        val last = lastCheckTime
        if (
            last == null ||
            last.plusSeconds(60).isBefore(now) ||
            miscInfo.nextTickTime?.isBefore(now) ?: false
        ) {
            checkForUpdate()
        }
    }


    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.new != SkyBlockIsland.GARDEN) {
            DataHandler.saveGardenData()
            greenhouseGrids.forEach {
                it.state.hasRuntimeReferences = false
            }
        }

        checkForUpdate()

    }

    @Subscription
    fun onGameShutdown(event: ServerDisconnectEvent) {
        DataHandler.saveGardenData()
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
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onInventory(event: ContainerInitializedEvent) {
        val realItems = event.containerItems.filter { !it.isSkyblockFiller() }
        if (event.title == "Crop Diagnostics") {
            getDiagnosesData(realItems)
            plantDiagnosticListeningElement = null
            plantDiagnosticHitBaseBlock = null
            return
        }
        plantDiagnosticListeningElement = null
        plantDiagnosticHitBaseBlock = null
        if (event.title == "Desk") {
            updateCropGrowth(realItems)
            return
        }

        if (event.title == "Greenhouse Upgrades") {
            updateUpgrades(realItems)
        }

    }


    @EventHandler
    fun onBlockBreak(event: OnBlockDestroyedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return


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
        if (!grid.hasRuntime()) return

        if (event.target !is ArmorStand) return
        // for now just remove the element later add cancellation with layouts


        val removed = grid.removeMatchingEntity(event.target)
        removedElementByAttack = removed
    }

    @EventHandler
    fun onBlockPlaced(event: OnBlockPlacedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return

        if (placedCrop == null) return
        cropPlanted()

        placedCrop = null
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

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
        if (!grid.hasRuntime()) return

        val gridArea = grid.plot?.getBuildableArea() ?: return
        if (!gridArea.contains(event.packet.pos.center)) return
        val slot = grid.getSlotAt(event.packet.pos, false) ?: return

        if (gridArea.contains(event.packet.pos.center)) {
            if (event.packet.pos.y == 74) {

                if (event.packet.blockState.block == Blocks.FIRE) {
                    val alreadyHasFire = grid.elements.any {
                        it.instance.slot == slot && it.instance.cropDef.name == "Fire"
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
            }
        }

        if (event.packet.pos.y != 73) return
        slot.placedBlock = event.packet.blockState
    }


    @EventHandler
    fun onInteractEntity(event: OnInteractEntityEvent) {
        val entityBlockPos = BlockPos.containing(event.target.position())
        plantDiagnosticHitBaseBlock = BlockPos(entityBlockPos.x, 73, entityBlockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return
        val standTarget = event.target as? ArmorStand ?: return
        if (mainHandId.id == "item:plant_diagnostics_tool") {
            setDiagnosesListeningElement(null, standTarget, grid)
            return
        }
    }


    @EventHandler
    fun onItemUse(event: OnUseEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        if (("item:" + mainHandId.id) in waterCanIds) {
            tryGetWaterCanData()
            return
        }
    }

    @EventHandler
    fun onBlockUse(event: OnBlockUseEvent) {
        plantDiagnosticHitBaseBlock = BlockPos(event.hit.blockPos.x, 73, event.hit.blockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        val foundCrop = CropRegistry.get(mainHandId.id)

        if (mainHandId.id == "item:plant_diagnostics_tool") {
            setDiagnosesListeningElement(event.hit.blockPos, null, grid)

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


    fun warnUnknownValues(sendWarning: Boolean = true): Boolean {
        val warnings = mutableListOf<Component>()
        if (miscInfo.cropGrowthValue == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Crop Growth value. Click here to open desk",
                    "/desk"
                )
            )
        }
        if (miscInfo.cropSpeedUpgradeValue == null || miscInfo.cropYieldUpgradeValue == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Crop Speed or Yield upgrade. Click here to open desk",
                    "/greenhouseupgrades"
                )
            )
        }
        if (miscInfo.nextTickTime == null) {
            warnings.add(
                ChatUtils.buildWithPrefix("Unknown tick time, please right click a non fully grown plant")
            )
        }
        if (sendWarning) {
            ChatUtils.sendWarningsComponents(warnings)
        }
        return warnings.isNotEmpty()
    }

    fun cropPlanted() {
        if (placedCrop == null) return
        val grid = getCurrentGrid() ?: return

        val availableStands = grid.getUnassignedArmorStands()?.toMutableList() ?: return
        val slot = grid.getSlotAt(placedCrop!!.second, false) ?: return
        val runtime = grid.findElementAtSlot(slot, availableStands)
        runtime ?: return
        if (runtime.instance.cropDef.needsWater) {
            runtime.instance.waterLevel = 0
        }
        grid.addElement(runtime, System.currentTimeMillis())
    }

    // "||||||||||||||||||||" this is the custom name of the armor to string we just need the formatting??
    fun tryGetWaterCanData() {
        val buildableArea = getCurrentGrid()?.plot?.getBuildableArea() ?: return
        val stands = Minecraft.getInstance().level?.getEntitiesOfClass(ArmorStand::class.java, buildableArea) ?: return
        ChatUtils.sendWithPrefix("Stands size: ${stands.size}")
        val filteredStands = stands.filter {
            it.getItemBySlot(EquipmentSlot.HEAD) == ItemStack.EMPTY
        }
        ChatUtils.sendWithPrefix("Filtered stand size: ${filteredStands.size}")
    }

    fun setDiagnosesListeningElement(hitBlock: BlockPos? = null, hitEntity: ArmorStand? = null, grid: GreenhouseGrid) {
        var hitElement: ElementRuntimeState? = null
        if (hitBlock != null) {
            hitElement = grid.elements.find {
                it.blocksMap?.keys?.contains(hitBlock) ?: return@find false
            }
        }
        if (hitEntity != null && hitElement == null) {
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

    fun getDiagnosesData(realItems: List<ItemStack>) {
        if (!baseSetting.value) return
        val identifyStack = realItems.firstOrNull() ?: return

        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null

        var def: CropDefinition? = null

        if (useNameFallback) {
            if (identifyStack.getLore().any { it.string.contains("Base Crop") }) {
                def = CropRegistry.get(identifyStack.customName?.string ?: identifyStack.itemName.string)
            }
        } else {
            def = CropRegistry.get(stackId.id)
        }

        val beaconStack = realItems.firstOrNull { it.item == Items.BEACON }
        val beaconLore = beaconStack?.getLore() ?: return

        val saplingStack = realItems.firstOrNull { it.item == Items.JUNGLE_SAPLING }
        val saplingLore = saplingStack?.getLore() ?: return

        val bucketStack = realItems.firstOrNull { it.item == Items.WATER_BUCKET }
        val bucketLore = bucketStack?.getLore() ?: return

        val waterLevel = runCatching {
            bucketLore[0].siblings[1].string.toInt()
        }.getOrNull()

        val status = runCatching {
            beaconLore[0].siblings[1].string
        }.getOrNull()

        val age = runCatching {
            saplingLore[0].siblings[1].string
        }.getOrNull()

        val stageRaw = runCatching {
            saplingLore[1].siblings[2].string
        }.getOrNull()
            ?.let { raw ->
                when {
                    raw.equals("FULLY GROWN", ignoreCase = true) -> def?.maxStage
                    raw.equals("DEAD", ignoreCase = true) -> {
                        if (def?.skyblockId == SkyBlockItemId.item("DEAD_PLANT"))
                            return@let 1
                        return@let null
                    }

                    else -> raw.toIntOrNull()
                }
            }

        val nextStage = runCatching {
            saplingLore[2].siblings.getOrNull(2)?.string
                ?.ifBlank { saplingLore[2].siblings.getOrNull(1)?.string ?: "" }
                ?: saplingLore[2].siblings.getOrNull(1)?.string
        }.getOrNull()

        if (nextStage?.contains(Regex("\\d")) ?: false) {
            if (!LocationAPI.isGuest) {
                miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
                lastCheckTime = Instant.now()
            }
        }

        def ?: return
        val matchingStage = def.stageDefs.find { stageDef ->
            stageRaw in stageDef.stageRange
        }
        stageRaw ?: return
        plantDiagnosticListeningElement?.let {
            it.instance.age = age?.parseDurationToMs()
            it.instance.growthStage = GrowthStageInfo.Known(stageRaw)
            it.instance.waterLevel = waterLevel
        }
        val level = Minecraft.getInstance().level ?: return
        var abnormalRotationFound = false
        val element = plantDiagnosticHitBaseBlock?.let { block ->
            val minX = block.x.toDouble()
            val minY = block.y.toDouble()
            val minZ = block.z.toDouble()

            val maxX = (block.x + FarmingDebug.footprint.width).toDouble()
            val maxY = (block.y + 15).toDouble()  // height
            val maxZ = (block.z + FarmingDebug.footprint.height).toDouble()

            val box = AABB(
                minX, minY, minZ,
                maxX, maxY, maxZ
            )

            val armorStands = level.getEntitiesOfClass(ArmorStand::class.java, box)
            abnormalRotationFound = armorStands.any {
                ((it.headPose.x != 0.0f || it.headPose.y != 0.0f || it.headPose.z != 0.0f) ||
                        (!it.xRot.isCardinalYaw() || !it.yRot.isCardinalYaw()))
                        && PlayerUtils.getSkinHash(it.getItemBySlot(EquipmentSlot.HEAD)) != null
            }
            GreenhouseGrid.findElementAtBasePos(
                block,
                armorStands
            )
        }

        element?.let {
            ChatUtils.sendWithPrefix("Successfully matched element ${it.instance.elementId} : ${it.instance.growthStage} to block $plantDiagnosticHitBaseBlock")
        }

        val isSelf = UUID.fromString("eef58b9d-39e1-4062-8a1a-2f921f14a46d") == Minecraft.getInstance().player?.uuid
        val override = false
        if (matchingStage != null){
            if (matchingStage.needsRotationData(abnormalRotationFound)){
                ChatUtils.sendWithPrefix("No rotation data for ${def.name}")
                plantDiagnosticHitBaseBlock?.let {
                    copyCropStageData(it,stageRaw, def, !isSelf)
                }
            }
            else if (override){
                ChatUtils.sendWithPrefix("Overridden ${def.name}")
                plantDiagnosticHitBaseBlock?.let {
                    copyCropStageData(it,stageRaw, def, !isSelf)
                }
            }
        }
        else {
            ChatUtils.sendWithPrefix("No matching stage for ${def.name} please send the copied output")
            plantDiagnosticHitBaseBlock?.let {
                copyCropStageData(it,stageRaw, def, !isSelf)
            }
        }
    }
    fun CropStage.needsRotationData(abnormalRotation: Boolean): Boolean =
        abnormalRotation && this.armorStands?.let { stands ->
            stands.isNotEmpty() && !stands.any {
                it.headRotation != null && it.xRotation != null && it.yRotation != null
            }
        } ?: true

    fun updateCropGrowth(realItems: List<ItemStack>) {
        val sunflower = realItems.firstOrNull {
            it.item == Items.SUNFLOWER
        } ?: return

        val lore = sunflower.getLore()

        val cropGrowthLine = lore.firstOrNull { component ->
            component.siblings.any { it.string.contains("Crop Growth") }
        }

        val cropGrowth = cropGrowthLine
            ?.siblings
            ?.firstOrNull { it.string.any { c -> c.isDigit() } }
            ?.string
            ?.filter { it.isDigit() }
            ?.toIntOrNull()

        cropGrowth ?: return
        if (miscInfo.cropGrowthValue != cropGrowth) {
            ChatUtils.sendWithPrefix("Updated crop growth value to $cropGrowth")
            ChatUtils.sendWithPrefix("Make sure to open /desk again if you get more crop growth.")
            miscInfo.cropGrowthValue = cropGrowth
        }

    }

    fun updateUpgrades(realItems: List<ItemStack>) {
        val seedsStack = realItems.firstOrNull { it.item == Items.WHEAT_SEEDS }
        val plantPotStack = realItems.firstOrNull { it.item == Items.FLOWER_POT }
        val seedsLore = seedsStack?.getLore()
        val potLore = plantPotStack?.getLore()

        val speedTier = seedsLore?.firstOrNull { line ->
            line.siblings.any { it.string.contains("Current Tier") }
        }?.siblings?.firstOrNull { it.string.toIntOrNull() != null }
            ?.string
            ?.toIntOrNull()

        val yieldTier = potLore?.firstOrNull { line ->
            line.siblings.any { it.string.contains("Current Tier") }
        }?.siblings?.firstOrNull { it.string.toIntOrNull() != null }
            ?.string
            ?.toIntOrNull()

        speedTier ?: return
        yieldTier ?: return

        if (miscInfo.cropSpeedUpgradeValue != speedTier) {
            miscInfo.cropSpeedUpgradeValue = speedTier
            ChatUtils.sendWithPrefix("Updated Growth Speed Tier to $speedTier")
        }
        if (miscInfo.cropYieldUpgradeValue != yieldTier) {
            miscInfo.cropYieldUpgradeValue = yieldTier
            ChatUtils.sendWithPrefix("Updated Plant Yield Tier to $speedTier")
        }
    }

    fun getCurrentUniques(): Set<UniqueCropKey> {
        val foundUniques = mutableSetOf<UniqueCropKey>()

        greenhouseGrids.forEach { grid ->
            grid.layout.elementInstances.forEach { instance ->
                if (!instance.cropDef.isBaseCrop) return@forEach
                foundUniques.add(UniqueCropKey.from(instance.cropDef))
            }
        }

        return foundUniques
    }

    fun getMissingUniques(): Set<UniqueCropKey> {
        val found = getCurrentUniques()
        return CropRegistry.all
            .filter { it.isBaseCrop }
            .map { UniqueCropKey.from(it) }
            .toSet()
            .minus(found)
    }


    fun computeGrowthStageTimeMs(
        uniqueCrops: Int,
        cropGrowthStat: Int,
        greenhouseUpgrade: Int
    ): Long {

        val uniqueCropBonus = 0.025 * uniqueCrops
        val cropGrowthBonus = 0.0025 * cropGrowthStat

        val upgradeBonus = when (greenhouseUpgrade) {
            in 0..8 -> 0.05 * greenhouseUpgrade
            9 -> 0.50
            else -> throw IllegalArgumentException("Invalid greenhouse upgrade level: $greenhouseUpgrade")
        }

        val denominator =
            1.0 +
                    uniqueCropBonus +
                    cropGrowthBonus +
                    upgradeBonus

        val seconds = 14400.0 / denominator

        return (seconds * 1000.0).toLong()
    }



    fun copyCropStageData(
        basePos: BlockPos,
        stageNum: Int? = null,
        foundDefinition: CropDefinition? = null,
        discordFormat: Boolean = false
    ) {
        val world = Minecraft.getInstance().level ?: return
        val sb = StringBuilder(2048)

        val blockData = mutableListOf<CropBlockExport>()
        val standData = mutableListOf<ArmorStandExport>()

        val footprint = foundDefinition?.footprint
        val width = footprint?.width ?: 1
        val height = footprint?.height ?: 1

        if (discordFormat) {
            sb.appendLine("```")
        }

        for (dx in 0 until width) {
            for (dz in 0 until height) {

                var y = basePos.y + 1

                while (true) { //for multi height crops
                    val checkPos = BlockPos(
                        basePos.x + dx,
                        y,
                        basePos.z + dz
                    )

                    val checkState = world.getBlockState(checkPos)

                    if (checkState.isAir) break

                    val offsetY = y - basePos.y

                    blockData.add(
                        CropBlockExport(
                            offset = BlockPos(dx,offsetY,dz),
                            blockState = checkState
                        )
                    )
                    y++
                }
            }
        }
        // capture maximum stands (false positives on players but thats fine)
        val box = AABB(
            basePos.x.toDouble(),
            basePos.y.toDouble() - 2,
            basePos.z.toDouble(),
            basePos.x + width.toDouble(),
            basePos.y.toDouble() + 14,
            basePos.z + height.toDouble()
        )

        val stands = world.getEntities(null, box)

        val originVec = Vec3(
            basePos.x.toDouble() + width / 2.0, //get center of footprint
            basePos.y.toDouble(),               // has to be center for mirroring to work properly.
            basePos.z.toDouble() + width / 2.0
        )


        for (entity in stands) {
            if (entity !is ArmorStand) continue

            val offset = entity.position().subtract(originVec)

            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            val hash = PlayerUtils.getSkinHash(head)
            val headRotations = entity.headPose
            val customName = if (entity.hasCustomName()) {
                entity.name.string.replace("\"", "\\\"")
            } else null

            standData.add(
                ArmorStandExport(
                    offset = offset,
                    rotation = headRotations,
                    xRotation = entity.xRot,
                    yRotation = entity.yRot,
                    hash = hash,
                    customName = customName
                )
            )
        }



        sb.appendLine("CropStage(")

        var finalBlockString = ""
        if (blockData.isNotEmpty()){
            val grouped = blockData.groupBy {
                it.blockState
            }

            val singletons = grouped.values
                .filter { it.size == 1 }
                .map { it.first() }

            val patterns = grouped.values
                .filter { it.size > 1 }

            val parts = mutableListOf<String>()

            if (patterns.isNotEmpty()) {
                patterns.forEach {

                    val posList = it.joinToString(",\n") { b ->
                        "BlockPos(${b.offset.x}, ${b.offset.y}, ${b.offset.z})"
                    }

                    parts += """
            CropBlockState.blockStatePattern(
                listOf(
                    $posList
                ),
                blockState = ${toFunctionString(it.first().blockState)}
            )
        """.trimIndent()
                }
            }

            if (parts.isNotEmpty()){
                var appendedString = "    blocks = " + parts.removeFirst()
                parts.forEach {
                    appendedString += " + $it"
                }

                finalBlockString = appendedString
                parts.clear()
            }


            if (singletons.isNotEmpty()) {
                val singletonPart = singletons.joinToString(",\n") { block ->
                    """
    CropBlockState(
        offset = BlockPos(${block.offset.x}, ${block.offset.y}, ${block.offset.z}),
        blockState = ${toFunctionString(block.blockState)}
    )
    """.trimIndent()
                }

                parts += singletonPart
            }

            if (parts.isNotEmpty()){
                if (finalBlockString.isBlank()){ //no patterns only singletons
                    finalBlockString = "    blocks = listOf(\n" +
                            parts.joinToString(",\n") +
                            "\n)"

                } else { //patterns AND blocks
                    val combined = finalBlockString +
                            " + listOf(\n" +
                            parts.joinToString(",\n") +
                            "\n)"

                    finalBlockString = combined
                }
            }

            if (finalBlockString.isNotBlank()) {
                sb.appendLine("$finalBlockString,")
            }
        }
        else {
            sb.appendLine("    blocks = listOf(),")
        }

        if (standData.isNotEmpty()) {

            val grouped = standData.groupBy {
                it.hash
            }

            val singletons = grouped.values
                .filter { it.size == 1 }
                .map { it.first() }

            val patterns = grouped.values
                .filter { it.size > 1 }

            val patternSections = mutableListOf<String>()
            val singletonSections = mutableListOf<String>()

            if (patterns.isNotEmpty()) {
                patterns.forEach { group ->

                    val offsets = group.joinToString(",\n") {
                        "        Vec3(${it.offset.x}, ${it.offset.y}, ${it.offset.z})"
                    }

                    val rotations = group.joinToString(",\n") {
                        "        Rotations(${it.rotation.x}f, ${it.rotation.y}f, ${it.rotation.z}f)"
                    }

                    val xRotations = group.joinToString(",\n") {
                        "        ${it.xRotation}f"
                    }

                    val yRotations = group.joinToString(",\n") {
                        "        ${it.yRotation}f"
                    }

                    val anyAbnormalRotations = group.any { it.rotation.x != 0f || it.rotation.y != 0f || it.rotation.z != 0f }
                    val anyAbnormalXRotations = group.any { !it.xRotation.isCardinalYaw() }
                    val anyAbnormalYRotations = group.any { !it.yRotation.isCardinalYaw() }


                    val hash = group.first().hash
                    val name = group.first().customName

                    val rotationsSection = """
    rotations = listOf(
$rotations
    ),
    xRotations = listOf(
$xRotations
    ),
    yRotations = listOf(
$yRotations
    ),
""".trimIndent()

                    patternSections += """
CropArmorStand.matcherPattern(
    offsets = listOf(
$offsets
    ),
    ${if (anyAbnormalRotations || anyAbnormalXRotations || anyAbnormalYRotations) rotationsSection else ""}
    hashString = "$hash"${
                        name?.let {
                            ",\n    customName = \"$it\""
                        } ?: ""
                    }
)
        """.trimIndent()
                }

            }


            if (singletons.isNotEmpty()) {

                val singletonText = singletons.joinToString(",\n") { stand ->
                    buildString {

                        val fields = mutableListOf<String>()
                        fields.add("offset = Vec3(${stand.offset.x}, ${stand.offset.y}, ${stand.offset.z})")
                        if (stand.rotation.x != 0f || stand.rotation.y != 0f || stand.rotation.z != 0f) {
                            fields.add("headRotation = Rotations(${stand.rotation.x}f, ${stand.rotation.y}f, ${stand.rotation.z}f)")
                            fields.add("xRotation = ${stand.xRotation}f")
                            fields.add("yRotation = ${stand.yRotation}f")
                        }
                        if (stand.hash != null){
                            fields.add("hashString = \"${stand.hash}\"")
                        }
                        if (stand.customName != null){
                            fields.add("containsCustomName = \"${stand.customName}\"")
                        }

                        append(
                            """
CropArmorStand(
    ${fields.joinToString(",\n" )}
)
""".trimIndent()
                        )
                    }
                }

                singletonSections += singletonText

            }


            val final = when {
                patterns.isNotEmpty() && singletons.isNotEmpty() ->
                    patternSections.joinToString(" + ") +
                            " + listOf(" +
                            singletonSections.joinToString(",\n") +
                            "\n)"

                patterns.isNotEmpty() ->
                    patternSections.joinToString(" + ")

                singletons.isNotEmpty() ->
                    "listOf(\n" +
                            singletonSections.joinToString(",\n") +
                            "\n)"

                else -> " listOf()"
            }
            sb.appendLine("    armorStands = $final,")
        } else {
            sb.appendLine("    armorStands = listOf(),")
        }

        sb.appendLine("    ${stageNum ?: 1}..${stageNum ?: 1}")
        sb.appendLine(")")

        if (discordFormat) {
            sb.appendLine("```")
            sb.appendLine("Crop found: ${foundDefinition?.name} stageNum=$stageNum")
        }

        val result = sb.toString()
        Minecraft.getInstance().keyboardHandler.clipboard = result

        ChatUtils.sendWithPrefix("Copied crop stage to clipboard (${result.length} chars)")
    }

    //temp for exporting
    data class ArmorStandExport(
        val offset: Vec3,
        val rotation: Rotations,
        val xRotation: Float,
        val yRotation: Float,
        val hash: String?,
        val customName: String?
    )

    data class CropBlockExport(
        val offset: BlockPos,
        val blockState: BlockState
    )


    sealed class UniqueCropKey {

        data class Def(val id: String) : UniqueCropKey()
        data object Flower : UniqueCropKey()
        data object Mushroom : UniqueCropKey()

        companion object {
            fun from(def: CropDefinition): UniqueCropKey {
                return when (def.name) {
                    "Sunflower", "Moonflower" -> Flower
                    "Red Mushroom", "Brown Mushroom" -> Mushroom
                    else -> Def(def.skyblockId?.id ?: def.name)
                }
            }
        }
    }
}