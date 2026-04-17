package org.magic.magicaddons.features.farming

import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
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

    private fun initData(){
        if (allInitialized()) return
        if (!knownIdsInitialized) return
        val plot = PlotAPI.getCurrentPlot() ?: return
        val plotId = plot.id
        if (!knownGreenhouseIds.contains(plotId)) return
        if (initializedGreenhouseIds.contains(plotId)) return


        val unsafeBox = getAABBUnsafe(plot)
        val box = unsafeBox?.toBoxFromAABB() ?: return

        initializeGreenhouse(plotId, box)


    }
    private fun initializeGreenhouse(plotId: Int, box: Box) {

        val world = MinecraftClient.getInstance().world ?: return

        val startX = box.minX + 43
        val startZ = box.minZ + 43

        val grid = GreenhouseGrid()

        for (x in 0 until 10) {
            for (y in 0 until 10) {

                val worldX = (startX + x).toInt()
                val worldZ = (startZ + y).toInt()

                val pos = net.minecraft.util.math.BlockPos(
                    worldX,
                    73,
                    worldZ
                )
                val blockState = world.getBlockState(pos)
                val unlocked = blockState.block != Blocks.PODZOL

                val addedSlot = GreenhouseSlot(x,y, unlocked, blockState)
                grid.setSlot(addedSlot)
            }
        }

        greenhouseList.add(grid)
        ChatUtils.sendWithPrefix("Initialized Greenhouse: $plotId")
        initializedGreenhouseIds.add(plotId)
    }


    fun allInitialized() = knownGreenhouseIds.all { initializedGreenhouseIds.contains(it) }





    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!baseSetting.value) return
        initData()
    }

    @Subscription
    fun islandChanged(event: IslandChangeEvent) {
        if (!baseSetting.value) return
        if (event.new != SkyBlockIsland.GARDEN) return
        if (!knownIdsInitialized) {
            if (PlotAPI.plots.isEmpty()) {
                //todo add warning to the user to open desk
            }
            PlotAPI.plots.forEach {
                if (it.data?.isGreenhouse != true) return@forEach
                knownGreenhouseIds.add(it.id)
            }
            knownIdsInitialized = true
        }
        initData()


    }

    fun slotToWorldPos(slot: GreenhouseSlot, box: Box, yLevel: Double = 73.0): Vec3d {

        val startX = box.minX + 43
        val startZ = box.minZ + 43

        val x = startX + slot.x
        val z = startZ + slot.y

        return Vec3d(
            x + 0.5,
            yLevel,
            z + 0.5
        )
    }





    //hopefully with 26.1 no longer needed
    fun getAABBUnsafe(plot: Any): Any? {
        return try {
            val field = plot.javaClass.getDeclaredField("aabb")
            field.isAccessible = true
            field.get(plot)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun Any.toBoxFromAABB(): Box? {
        return try {
            val fields = this.javaClass.declaredFields
                .filter { it.type == Double::class.javaPrimitiveType }
                .onEach { it.isAccessible = true }

            if (fields.size < 6) return null

            // this is a band aid fix
            val minX = fields[1].getDouble(this)
            val minY = fields[2].getDouble(this)
            val minZ = fields[3].getDouble(this)
            val maxX = fields[4].getDouble(this)
            val maxY = fields[5].getDouble(this)
            val maxZ = fields[6].getDouble(this)

            Box(minX, minY, minZ, maxX, maxY, maxZ)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}