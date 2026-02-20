package org.magic.magicaddons.features.farming

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import org.magic.magicaddons.config.MagicAddonsConfig
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.api.SlotRenderable
import org.magic.magicaddons.util.ItemUtils.getHypixelId
import org.magic.magicaddons.util.LocationUtils

object EquipmentHighlight : Feature(), SlotRenderable {

    override val id = "highlightFarmingEquipment"
    override val category = "farming"

    private val config = MagicAddonsConfig.categories[category]?.get(id)

    // --- Equipment IDs ---
    private val pestEquipmentIds = setOf(
        "PEST_VEST",
        "PESTHUNTERS_BELT",
        "PESTHUNTERS_NECKLACE",
        "PESTHUNTERS_GLOVES"
    )

    private val blossomEquipmentIds = setOf(
        "BLOSSOM_CLOAK",
        "BLOSSOM_BELT",
        "BLOSSOM_NECKLACE",
        "BLOSSOM_BRACELET"
    )

    private val zorroId = "ZORROS_CAPE"

    // --- Runtime state ---
    private var isContest = false
    private var pestWidgetEnabled = false
    private var highlightBlossom = true

    override fun onSlotRender(
        context: DrawContext,
        slot: Slot,
        screen: HandledScreen<*>
    ) {
        // Feature toggle
        if (config?.enabled != true) return

        val stack = slot.stack ?: return
        val id = stack.getHypixelId() ?: return

        if (slot.inventory !is PlayerInventory) return

        val title = screen.title?.string ?: return
        if (title != "Your Equipment and Stats") return

         

        val color = (config.extra["equipmentColor"] as? Int) ?: 0xFF0000
        val zorroSwapEnabled = (config.extra["zorroSwaping"] as? Boolean) ?: false

        if (highlightBlossom) {
            if (zorroSwapEnabled && isContest) {
                if (id != "BLOSSOM_CLOAK" && (id in blossomEquipmentIds || id == zorroId)
                ) {
                    context.fill(slot.x,slot.y,slot.x+16,slot.y+16, config.extra["equipmentColor"] as? Int ?: 0xFFFF00)
                }
            } else {
                if (id in blossomEquipmentIds) {
                    context.fill(slot.x,slot.y,slot.x+16,slot.y+16, config.extra["equipmentColor"] as? Int ?: 0xFFFF00)
                }
            }
        } else {
            if (id in pestEquipmentIds) {
                context.fill(slot.x,slot.y,slot.x+16,slot.y+16, config.extra["equipmentColor"] as? Int ?: 0xFFFF00)
            }
        }
    }





}


