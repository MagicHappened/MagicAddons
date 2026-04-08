package org.magic.magicaddons.features.debug

import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.PlayerHeadItem
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnAttackEntityEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import java.net.URI

object MobHitDebugInfo : Feature() {
    init {
        EventBus.register(this)
    }

    override val id: String = "MobHitDebug"
    override val displayName: String = "Mob Hit Skin Debug"
    override val tooltipMessage: String = "On next mob hit will cancel the actual event and print debug information"
    override val category: String = "debug"

    override val baseSetting: BooleanSetting = BooleanSetting(
        key = "enabled",
        displayName = displayName,
        tooltip = tooltipMessage,
        value = false
        //todo add the select option to return
    )

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        if (!baseSetting.value) return
        event.canceled = true

        when (val target = event.target) {
            is PlayerEntity -> attackPlayerDebug(target)
            is ArmorStandEntity -> attackArmorStandDebug(target)
            is LivingEntity -> attackMobDebug(target)
            else -> attackUnknownDebug(target)
        }
    }


    fun attackPlayerDebug(player: PlayerEntity) {
        val url = PlayerUtils.getSkinUrl(player)
        val hash = PlayerUtils.getSkinHash(player)

        if (url == null || hash == null) {
            ChatUtils.sendWithPrefix("No skin data")
            return
        }

        val clickableText = Text.literal("Click for skin url").setStyle(
            Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI(url)))
        )
        ChatUtils.sendWithPrefix("=== Player Debug ===")
        ChatUtils.sendWithPrefix(clickableText)
        ChatUtils.sendWithPrefix("Skin hash: $hash")
        val armorStandTags = MinecraftClient.getInstance().world?.getOtherEntities(null, player.boundingBox.expand(0.5, 2.0, 0.5))
            ?.filterIsInstance<ArmorStandEntity>()
            ?.mapNotNull { it.customName?.string }
        armorStandTags?.forEach {
            ChatUtils.sendWithPrefix("Nearby armor stand: $it")
        }


    }

    fun attackArmorStandDebug(stand: ArmorStandEntity) {
        ChatUtils.sendWithPrefix("=== Armor Stand Debug ===")

        val name = stand.customName?.string ?: "No custom name"
        ChatUtils.sendWithPrefix("Name: $name")

        sendEntityEquipmentDebug(stand)

    }

    fun attackMobDebug(mob: LivingEntity) {
        ChatUtils.sendWithPrefix("=== Mob Debug ===")

        val name = mob.customName?.string ?: mob.name.string
        ChatUtils.sendWithPrefix("Name: $name")

        val type = mob.type.toString()
        ChatUtils.sendWithPrefix("Type: $type")
        sendEntityEquipmentDebug(mob)

    }

    fun attackUnknownDebug(entity: Entity) {
        ChatUtils.sendWithPrefix("=== Unknown Entity Debug ===")
        ChatUtils.sendWithPrefix("Class: ${entity::class.qualifiedName}")
    }

    fun sendEntityEquipmentDebug(entity: LivingEntity) {
        ChatUtils.sendWithPrefix("--- Equipment ---")

        val equipment = mutableMapOf<EquipmentSlot, ItemStack>(
            EquipmentSlot.MAINHAND to entity.getEquippedStack(EquipmentSlot.MAINHAND),
            EquipmentSlot.OFFHAND to entity.getEquippedStack(EquipmentSlot.OFFHAND),
            EquipmentSlot.FEET to entity.getEquippedStack(EquipmentSlot.FEET),
            EquipmentSlot.CHEST to entity.getEquippedStack(EquipmentSlot.CHEST),
            EquipmentSlot.LEGS to entity.getEquippedStack(EquipmentSlot.LEGS),
            EquipmentSlot.HEAD to entity.getEquippedStack(EquipmentSlot.HEAD)
        )

        equipment.forEach { (slot, stack) ->
            if (stack.isEmpty) return@forEach

            val itemName = stack.item.toString()
            ChatUtils.sendWithPrefix("$slot: $itemName")

            if (isPlayerHead(stack)) {
                val skullInfo = getSkullInfo(stack)
                if (skullInfo != null) {
                    ChatUtils.sendWithPrefix("  -> Skull: $skullInfo")
                } else {
                    ChatUtils.sendWithPrefix("  -> Skull: (no texture data)")
                }
            }
        }

    }

    fun isPlayerHead(stack: ItemStack): Boolean {
        return stack.item is PlayerHeadItem
    }

    fun getSkullInfo(stack: ItemStack): String? {
        val hash = PlayerUtils.getSkinHash(stack) ?: return null
        return "hash=$hash"
    }

}