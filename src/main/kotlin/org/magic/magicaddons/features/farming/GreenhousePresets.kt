package org.magic.magicaddons.features.farming

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnAttackEntityEvent
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent
import org.magic.magicaddons.events.interact.OnStartDestroyBlockEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

object GreenhousePresets : Feature() {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }


    override val id: String = "GreenhousePresets"
    override val displayName: String = "Greenhouse Presets"
    override val tooltipMessage: String = "Enables Greenhouse Presets, highlighting, prevents accidental breaks and more."
    override val category: String = "farming"

    override val baseSetting: BooleanSetting =
        BooleanSetting(
            key = "enabled",
            displayName = displayName,
            tooltip = tooltipMessage,
            value = true
        ) //todo change to false default, just for testing purposes

    val knownGreenhouseIds = mutableSetOf<Int>()
    var knownIdsInitialized = false
    val initializedGreenhouseIds = mutableListOf<Int>()
    val greenhouseList = mutableListOf<GreenhouseGrid>()


    var checkedDeskReminder = true

    private fun initData() {
        if (allInitialized()) return
        if (!knownIdsInitialized) return

        val plot = PlotAPI.getCurrentPlot() ?: return
        val plotId = plot.id
        if (!knownGreenhouseIds.contains(plotId)) return
        if (initializedGreenhouseIds.contains(plotId)) return

        val box = plot.aabb

        initializeGreenhouse(plotId, box)
    }

    private fun initializeGreenhouse(plotId: Int, box: AABB) {

        val world = Minecraft.getInstance().level ?: return

        val startX = box.minX + 43
        val startZ = box.minZ + 43

        val grid = GreenhouseGrid()
        grid.plot = PlotAPI.getCurrentPlot()

        for (x in 0 until 10) {
            for (y in 0 until 10) {

                val worldX = (startX + x).toInt()
                val worldZ = (startZ + y).toInt()

                val pos = BlockPos(
                    worldX,
                    73,
                    worldZ
                )
                val blockState = world.getBlockState(pos)
                val unlocked = blockState.block != Blocks.PODZOL

                val addedSlot = GreenhouseSlot(x, y, unlocked, blockState)
                grid.setSlot(addedSlot)
            }
        }

        greenhouseList.add(grid)
        ChatUtils.sendWithPrefix("Initialized Greenhouse: $plotId")
        initializedGreenhouseIds.add(plotId)
    }

    fun initKnownIds() {
        if (knownIdsInitialized) return

        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach {
            if (it.data?.isGreenhouse != true) return@forEach
            knownGreenhouseIds.add(it.id)
        }
        knownIdsInitialized = true

    }

    fun allInitialized(): Boolean {
        if (!knownIdsInitialized) {
            return false
        }
        return knownGreenhouseIds.all { initializedGreenhouseIds.contains(it) }
    }


    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!baseSetting.value) return
        initKnownIds()
        if (!knownIdsInitialized) {
            if (checkedDeskReminder) {
                checkedDeskReminder = false //todo see if this works (remove skyblockapi cache)
                ChatUtils.sendWithPrefix("Cant figure out known greenhouses. Please open your plots menu in /desk")
            }
            return
        }
        initData()
    }

    @Subscription
    fun islandChanged(event: IslandChangeEvent) {
        if (!baseSetting.value) return
        if (event.new != SkyBlockIsland.GARDEN) return
        initKnownIds()
        if (!knownIdsInitialized) return
        initData()


    }





    @EventHandler
    fun onAttack(event: OnAttackEntityEvent){
        if (!baseSetting.value) return
        //todo add attack prevention here.
    }

    @EventHandler
    fun onStartBlockBreak(event: OnStartDestroyBlockEvent){
        if (!baseSetting.value) return
        //todo add block break filteration logic here
    }

    fun slotToWorldPos(slot: GreenhouseSlot, box: AABB, yLevel: Double = 73.0): Vec3 {

        val startX = box.minX + 43
        val startZ = box.minZ + 43

        val x = startX + slot.x
        val z = startZ + slot.y

        return Vec3(
            x + 0.5,
            yLevel,
            z + 0.5
        )
    }



    @EventHandler
    private fun onBlockBreak(event: OnBlockDestroyedEvent){
        if (LocationAPI.island != SkyBlockIsland.GARDEN) return
    }


}