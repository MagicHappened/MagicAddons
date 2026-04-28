package org.magic.magicaddons.features.debug

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import org.magic.magicaddons.data.config.BooleanSetting
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
    override val displayName: String = "Mob Hit Debug"
    override val tooltipMessage: String = "On next mob hit will cancel the actual event and print debug information"
    override val category: String = "debug"

    override val baseSetting: BooleanSetting = BooleanSetting(
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
            is Player -> attackPlayerDebug(target)
            is ArmorStand -> attackArmorStandDebug(target)
            is LivingEntity -> attackMobDebug(target)
            else -> attackUnknownDebug(target)
        }
    }


    fun attackPlayerDebug(player: Player) {
        val url = PlayerUtils.getSkinUrl(player)
        val hash = PlayerUtils.getSkinHash(player)

        if (url == null || hash == null) {
            ChatUtils.sendWithPrefix("No skin data")
            return
        }

        val clickableText = Component.literal("Click for skin url").setStyle(
            Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI(url)))
        )

        ChatUtils.sendWithPrefix("=== Player Debug ===")
        ChatUtils.sendWithPrefix(clickableText)
        ChatUtils.sendWithPrefix("Skin hash: $hash")

        printNearbyInfoEntities(player)
    }

    fun attackArmorStandDebug(stand: ArmorStand) {
        ChatUtils.sendWithPrefix("=== Armor Stand Debug ===")

        val name = stand.customName?.string ?: "No custom name"
        ChatUtils.sendWithPrefix("Name: $name")

        sendEntityDebug(stand)

    }

    fun attackMobDebug(mob: LivingEntity) {
        ChatUtils.sendWithPrefix("=== Mob Debug ===")

        val name = mob.customName?.string ?: mob.name.string
        ChatUtils.sendWithPrefix("Name: $name")

        val type = mob.type.toString()
        ChatUtils.sendWithPrefix("Type: $type")
        sendEntityDebug(mob)

        printNearbyInfoEntities(mob)
    }

    fun attackUnknownDebug(entity: Entity) {
        ChatUtils.sendWithPrefix("=== Unknown Entity Debug ===")
        ChatUtils.sendWithPrefix("Class: ${entity::class.qualifiedName}")
    }

    private fun printNearbyInfoEntities(entity: Entity, radius: Double = 0.5, height: Double = 2.0) {
        val level = Minecraft.getInstance().level ?: return

        val entities = level.getEntities(
            null,
            entity.boundingBox.inflate(radius, height, radius)
        ).filter {
            it !== entity && (
                    (it is ArmorStand && it.hasCustomName()) ||
                            it is Display
                    )
        }

        if (entities.isEmpty()) return

        ChatUtils.sendWithPrefix("=== Nearby Info Entities: ===")

        entities.forEach { 
            val name = it.customName?.string ?: "No custom name"
            ChatUtils.sendWithPrefix("Name: $name")
            ChatUtils.sendWithPrefix("Type: ${it.type}")
            sendEntityDebug(it)
        }
    }


    fun sendEntityDebug(entity: Entity) {

        when (entity) {

            is LivingEntity -> {
                ChatUtils.sendWithPrefix("=== Equipment ===")

                val slots = listOf(
                    EquipmentSlot.MAINHAND,
                    EquipmentSlot.OFFHAND,
                    EquipmentSlot.FEET,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.HEAD
                )

                for (slot in slots) {
                    val stack = entity.getItemBySlot(slot)
                    if (stack.isEmpty) continue

                    ChatUtils.sendWithPrefix("  -> ITEM ($slot): ${stack.item}")
                    ChatUtils.sendWithPrefix(PlayerUtils.getSkinHash(stack) ?: "No skin hash")
                }
            }

            is Display.ItemDisplay -> {
                val stack = entity.itemStack
                if (stack.isEmpty) return

                ChatUtils.sendWithPrefix("=== ItemDisplay ===")
                ChatUtils.sendWithPrefix("  -> ITEM: ${stack.item}")
                ChatUtils.sendWithPrefix(PlayerUtils.getSkinHash(stack) ?: "No skin hash")
            }

        }
    }









}