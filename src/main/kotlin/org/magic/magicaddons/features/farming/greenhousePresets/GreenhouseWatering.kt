package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.time.Duration
import java.time.Instant
import java.util.Optional

/**
 * Reads how much water the plants of a greenhouse hold.
 *
 * Skyblock only says so when the level changes: watering a plant hangs a bar above it for a few
 * seconds and then takes it away again. So using a watering can opens a window, and for as long as
 * it is open the plot is watched for bars to read.
 */
object GreenhouseWatering {

    init {
        EventBus.register(this)
    }

    private val waterCanIds = setOf(
        "HYDRO_CAN_1000",
        "HYDRO_CAN_TURBO_2000",
        "HYDRO_CAN_ULTRA_3000",
        "AQUAMASTER_X",
        "AQUAMASTER_HYDROMAX"
    )

    /** The character skyblock builds its bars out of, one per notch of the level. */
    private const val BAR_CHAR: Char = '|'

    /**
     * The notches of a water bar. Blue is water the plant holds and red is water it owes, both
     * measured against the white notches that make up the rest of the bar.
     */
    private val BAR_FILLED_COLOR: Int = TextColor.BLUE.value
    private val BAR_DEBT_COLOR: Int = TextColor.RED.value
    private val BAR_EMPTY_COLOR: Int = TextColor.WHITE.value

    /**
     * How long after a watering the stands are worth looking for. The water bar is not a permanent
     * part of a plant like the fleshtrap hunger bar is, it appears when the level changes and takes
     * itself away again a few seconds later.
     */
    private val WATERING_WINDOW: Duration = Duration.ofSeconds(10)

    /** When the stands spawned by the last watering stop being expected. */
    private var wateringUntil: Instant? = null


    /** Every tier of the watering can, matched past the prefix and casing of a skyblock id. */
    private fun isWaterCan(id: SkyBlockId): Boolean =
        id.id.substringAfter("item:").uppercase() in waterCanIds

    /**
     * Opens the window if [heldId] is a watering can, and says whether it did, so the caller can
     * stop looking at what the item might otherwise have been.
     */
    fun startWateringWindow(heldId: SkyBlockId): Boolean {
        if (!isWaterCan(heldId)) return false

        wateringUntil = Instant.now().plus(WATERING_WINDOW)
        return true
    }

    /**
     * The spray of a watering can does not reach the plants on the same tick the can is used, so a
     * use only says the water bars are about to appear. [readWaterStands] does the reading.
     */


    /**
     * Takes the water level off any bar standing over a plant of the current grid. Called for every
     * batch of entities that appears while a watering is expected, since the bars spawn a moment
     * after the spray and take themselves away again.
     */
    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        readWaterStands()
    }

    /**
     * Takes the water level off any bar standing over a plant of the current grid, for as long as a
     * watering is expected.
     *
     * Polled rather than driven by an entity event: the game reuses a bar it already has above a
     * plant, and a stand whose name changed is neither added nor counted as updated, since an update
     * only means the entities standing beside a plant changed.
     */
    private fun readWaterStands() {
        val until = wateringUntil ?: return

        if (Instant.now().isAfter(until)) {
            wateringUntil = null
            return
        }

        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val area = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        level.getEntitiesOfClass(ArmorStand::class.java, area).forEach { stand ->
            val waterLevel = stand.customName?.let { parseBar(it) } ?: return@forEach
            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val element = grid.elementCovering(slot) ?: return@forEach

            if (!element.instance.cropDef.needsWater) return@forEach

            element.instance.waterLevel = waterLevel
        }
    }

    /**
     * Reads a water bar as the level it stands for, between -100 and 100.
     *
     * The bar is one character per notch and always full length, what changes is the colouring. A
     * watered plant fills from the front in blue with white behind it, so eleven blue of sixteen is
     * 68. A plant in debt is white from the front with red behind it, and the red is the debt, so a
     * quarter red is -25 and a half red is -50.
     *
     * Counting the coloured notches rather than taking the leading run matters at both ends: a
     * plant sitting on exactly zero shows a bar of nothing but white, which a leading run would
     * have read as completely full.
     *
     * A bar in any other colour is not a water bar and is refused. Several plants hang a bar of the
     * same character over themselves, the fleshtrap hunger bar among them, and reading one of those
     * as a water level would be worse than reading nothing.
     */
    private fun parseBar(name: Component): Int? {
        var filled = 0
        var debt = 0
        var total = 0
        var foreign = false

        name.visit({ style, text ->
            val notches = text.count { it == BAR_CHAR }

            if (notches > 0) {
                when (style.color?.value) {
                    BAR_FILLED_COLOR -> filled += notches
                    BAR_DEBT_COLOR -> debt += notches
                    BAR_EMPTY_COLOR -> Unit
                    else -> foreign = true
                }

                total += notches
            }

            Optional.empty<Unit>()
        }, Style.EMPTY)

        if (foreign || total == 0) return null

        // a bar cannot hold water and owe it at once, and owing is the half worth believing
        if (debt > 0) return -(debt * 100 / total)

        return filled * 100 / total
    }
}
