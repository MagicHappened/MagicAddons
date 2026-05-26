package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.BlockUtils.getId
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller
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

        val grid = greenhouseGrids.find { it.layout.id == "plot_${plot.id}" } ?: return //isnt greenhouse
        if (grid.state.hasRuntimeReferences && !grid.state.needsUpdate) return

        grid.plot = plot
        grid.createSlotData()
        grid.setPlantData()

        grid.state.hasRuntimeReferences = true
        grid.state.needsUpdate = false
        grid.state.lastUpdateTimestamp = Instant.now()


        ChatUtils.sendWithPrefix("Successfully scanned data for ${plot.id}")
    }




    fun getCurrentGrid(): GreenhouseGrid? {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return greenhouseGrids.find { it.layout.id == "plot_$plotId" }
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

        val uniques = countUniques()
        val tickMs = computeGrowthStageTimeSeconds(
            uniques,
            miscInfo.cropGrowthValue!!,
            miscInfo.cropSpeedUpgradeValue!!) * 1000L

        if (miscInfo.nextTickTime == null) return

        val now = Instant.now()

        greenhouseGrids.forEach { grid ->

            val lastUpdate = grid.state.lastUpdateTimestamp ?: return@forEach

            val elapsedMs = now.toEpochMilli() - lastUpdate.toEpochMilli()

            val passedTicks = (elapsedMs / tickMs).toInt()

            if (passedTicks <= 0) return@forEach

            grid.state.pendingTicks = passedTicks
            grid.state.needsUpdate = true
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
        checkForUpdate()
        scanGridData()
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onInventory(event: ContainerInitializedEvent) {
        val realItems = event.containerItems.filter { !it.isSkyblockFiller() }
        if (event.title == "Crop Diagnostics") {
            sendDiagnosesData(realItems)
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

        val foundCrop = CropRegistry.all.firstOrNull { it.matchesId(mainHandId) }

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
        var unknownValue = false
        if (miscInfo.cropGrowthValue == null) {
            ChatUtils.sendWithCommand(
                "Unknown Crop Growth value. Click here to open desk",
                "/desk"
            )
            unknownValue = true
        }
        if (miscInfo.cropSpeedUpgradeValue == null || miscInfo.cropYieldUpgradeValue == null) {
            ChatUtils.sendWithCommand(
                "Unknown Crop Speed or Yield upgrade. Click here to open desk",
                "/greenhouseupgrades"
            )
            unknownValue = true
        }
        if (miscInfo.nextTickTime == null){
            ChatUtils.sendWithPrefix("Unknown tick time, please right click a non fully grown plant")
            unknownValue = true
        }

        return unknownValue
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
        grid.addElement(runtime, System.currentTimeMillis())
    }

    fun tryGetWaterCanData() {

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

    fun sendDiagnosesData(realItems: List<ItemStack>) {
        if (!baseSetting.value) return
        val identifyStack = realItems.firstOrNull() ?: return

        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null

        var def: CropDefinition? = null

        if (useNameFallback) {
            if (identifyStack.getLore().any { it.string.contains("Base Crop") }) {
                def = CropRegistry.all.find {
                    it.name == (identifyStack.customName?.string ?: identifyStack.itemName.string)
                }
            }
        } else {
            def = CropRegistry.all.find {
                it.skyblockId == stackId
            }
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
                    raw.equals("DEAD", ignoreCase = true) -> null
                    else -> raw.toIntOrNull()
                }
            }

        val nextStage = runCatching {
            saplingLore[2].siblings.getOrNull(2)?.string
                ?.ifBlank { saplingLore[2].siblings.getOrNull(1)?.string ?: "" }
                ?: saplingLore[2].siblings.getOrNull(1)?.string
        }.getOrNull()
        ChatUtils.sendWithPrefix("Next stage: $nextStage")
        if (nextStage?.contains(Regex("\\d")) ?: false){
            miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
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

        val isSelf = UUID.fromString("eef58b9d-39e1-4062-8a1a-2f921f14a46d") == Minecraft.getInstance().player?.uuid

        if (matchingStage == null) {
            ChatUtils.sendWithPrefix("No matching stage for ${def.name} please send the copied output")
            plantDiagnosticHitBaseBlock?.let {
                copyCropStageData(it, stageRaw, def, !isSelf)
            }
            return
        }
    }

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

    fun countUniques(): Int {
        val foundUniques = mutableSetOf<String>()

        greenhouseGrids.forEach { grid ->
            grid.layout.elementInstances.forEach { instance ->

                val cropDef = CropRegistry.all.find { def ->
                    (def.skyblockId?.id ?: def.name) == instance.elementId
                } ?: return@forEach

                if (!cropDef.isBaseCrop) return@forEach

                val uniqueKey = when (cropDef.name) {
                    "Sunflower", "Moonflower" -> "FLOWER"
                    "Red Mushroom", "Brown Mushroom" -> "MUSHROOM"
                    else -> cropDef.skyblockId?.id ?: cropDef.name
                }

                foundUniques.add(uniqueKey)
            }
        }

        return foundUniques.size
    }


    fun computeGrowthStageTimeSeconds(
        uniqueCrops: Int,
        cropGrowthStat: Int,
        greenhouseUpgrade: Int
    ): Double {

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

        return 14400.0 / denominator
    }

    fun copyCropStageData(
        basePos: BlockPos,
        stageNum: Int? = null,
        foundDefinition: CropDefinition? = null,
        discordFormat: Boolean = false
    ) {
        val world = Minecraft.getInstance().level ?: return
        val sb = StringBuilder(2048)

        val blockLines = mutableListOf<String>()

        var y = basePos.y + 1
        var stageIndex = 0

        if (discordFormat) {
            sb.appendLine("```")
        }



        while (true) {
            val checkPos = BlockPos(basePos.x, y, basePos.z)
            val checkState = world.getBlockState(checkPos)

            if (checkState.isAir) break

            val offsetY = y - basePos.y
            val blockId = checkState.getId()

            val hasAge = checkState.getIntProperty("age") != null

            val matcherLine = if (hasAge) {
                """
                it.isBlock("$blockId") &&
                        it.getIntProperty("age") == ${checkState.getIntProperty("age")}
            """.trimIndent()
            } else {
                """
                it.isBlock("$blockId")
            """.trimIndent()
            }

            blockLines.add(
                """
        CropBlockState(
            offset = BlockPos(0,$offsetY,0),
            matcher = {
$matcherLine
            }
        )
            """.trimIndent()
            )

            stageIndex++
            y++
        }


        val box = AABB(
            basePos.x.toDouble(),
            basePos.y.toDouble() - 2,
            basePos.z.toDouble(),
            basePos.x + 1.0,
            basePos.y.toDouble() + 4,
            basePos.z + 1.0
        )

        val stands = world.getEntities(null, box)

        val standLines = mutableListOf<String>()

        for (entity in stands) {
            if (entity !is ArmorStand) continue
            val center = Vec3(
                basePos.x + 0.5,
                basePos.y.toDouble(),
                basePos.z + 0.5
            )

            val offset = entity.position().subtract(center)

            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            val hash = PlayerUtils.getSkinHash(head)

            standLines.add(
                """
        CropArmorStand(
            offset = Vec3(${offset.x}, ${offset.y}, ${offset.z}),
            matcher = {
                ${if (!hash.isNullOrBlank()) "it == \"$hash\"" else "true"}
            }
        )
    """.trimIndent()
            )
        }


        sb.appendLine("CropStage(")

        // blocks
        sb.appendLine("    blocks = listOf(")
        if (blockLines.isNotEmpty()) {
            sb.appendLine(blockLines.joinToString(",\n"))
        }
        sb.appendLine("    ),")

        // armor stands
        if (standLines.isNotEmpty()) {
            sb.appendLine("    armorStands = listOf(")
            sb.appendLine(
                standLines.joinToString(",\n") { "        $it" }
            )
            sb.appendLine("    ),")
        } else {
            sb.appendLine("    armorStands = null,")
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

}