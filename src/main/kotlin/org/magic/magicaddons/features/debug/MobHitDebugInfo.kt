package org.magic.magicaddons.features.debug

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnAttackEntityEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import java.net.URI

/**
 * Prints what the client knows about whatever was hit. Type, custom name and skin hash always,
 * since a dump without them is useless; the rest is behind settings, being long enough to fill chat.
 */
object MobHitDebugInfo : Feature() {
    init {
        EventBus.register(this)
    }

    override val id: String = "MobHitDebug"
    override val displayName: String = "Mob Hit Debug"
    override val tooltipMessage: String = "On next mob hit will cancel the actual event and print debug information"
    override val category: String = "debug"

    private val showEquipment = BooleanSetting(
        key = "ShowEquipment",
        displayName = "Equipment",
        tooltip = "Adds every worn and held item, with the skin hash of each.",
        value = false
    )

    private val showNearbyEntities = BooleanSetting(
        key = "ShowNearbyEntities",
        displayName = "Nearby Entities",
        tooltip = "Adds the named armor stands and displays standing next to the target, which is " +
                "where most mobs keep the only information they have.",
        value = false
    )

    private val showPosition = BooleanSetting(
        key = "ShowPosition",
        displayName = "Position",
        tooltip = "Adds the block position of the target, for matching against a fixed area.",
        value = false
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        tooltip = tooltipMessage,
        value = false,
        children = listOf(
            showEquipment,
            showNearbyEntities,
            showPosition
        )
    )

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        if (!baseSetting.value) return
        event.canceled = true

        val target = event.target

        ChatUtils.sendWithPrefix(
            Component.literal(describeKind(target)).withStyle(ChatFormatting.GOLD)
        )

        describe(target).forEach { ChatUtils.send(it) }

        if (showNearbyEntities.value) {
            printNearbyInfoEntities(target)
        }
    }

    /** The short word for what was hit, which is the first thing worth knowing about it. */
    private fun describeKind(entity: Entity): String = when (entity) {
        is Player -> "Player"
        is ArmorStand -> "Armor Stand"
        is Display.ItemDisplay -> "Item Display"
        is Display -> "Display"
        is LivingEntity -> "Mob"
        else -> "Entity"
    }

    /** Everything worth knowing about one entity, as the lines to print under its heading. */
    private fun describe(entity: Entity): List<Component> = buildList {
        add(field("type", entity.type.toString()))

        entity.customName?.string?.let { add(field("name", it)) }

        if (entity is Player) {
            PlayerUtils.getSkinHash(entity)?.let { add(hashField("skin", it)) }
            PlayerUtils.getSkinUrl(entity)?.let { url ->
                add(
                    Component.literal("  skin url").withStyle(
                        Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withClickEvent(ClickEvent.OpenUrl(URI(url)))
                    )
                )
            }
        }

        if (entity is Display.ItemDisplay) {
            addAll(describeStack("item", entity.itemStack))
        }

        if (showPosition.value) {
            val pos = entity.blockPosition()
            add(field("at", "${pos.x} ${pos.y} ${pos.z}"))
        }

        if (showEquipment.value && entity is LivingEntity) {
            EQUIPMENT_SLOTS.forEach { slot ->
                addAll(describeStack(slot.name.lowercase(), entity.getItemBySlot(slot)))
            }
        }
    }

    /** An item slot, skipped entirely when it is empty so empty slots do not fill the chat. */
    private fun describeStack(label: String, stack: ItemStack): List<Component> {
        if (stack.isEmpty) return emptyList()

        return buildList {
            add(field(label, stack.item.toString()))
            PlayerUtils.getSkinHash(stack)?.let { add(hashField("$label hash", it)) }
        }
    }

    private fun field(label: String, value: String): Component =
        Component.literal("  $label: ").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE))

    /**
     * A hash, which is the value most matchers are written against, so it is worth being able to
     * take it out of the game without reading it off the screen character by character.
     */
    private fun hashField(label: String, hash: String): Component =
        Component.literal("  $label: ").withStyle(ChatFormatting.DARK_GRAY)
            .append(
                Component.literal(hash).withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(ClickEvent.CopyToClipboard(hash))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to copy")))
                )
            )

    /**
     * A lot of mobs are an item display with a name tag next to it and nothing else, so what stands
     * around the target is often the only thing that identifies it.
     */
    private fun printNearbyInfoEntities(entity: Entity, radius: Double = 0.5, height: Double = 2.0) {
        val level = Minecraft.getInstance().level ?: return

        val entities = level.getEntities(
            null,
            entity.boundingBox.inflate(radius, height, radius)
        ).filter {
            it !== entity && ((it is ArmorStand && it.hasCustomName()) || it is Display)
        }

        if (entities.isEmpty()) return

        ChatUtils.send(
            Component.literal("  nearby (${entities.size}):").withStyle(ChatFormatting.GOLD)
        )

        entities.forEach { nearby ->
            ChatUtils.send(
                Component.literal("  - ${describeKind(nearby)}").withStyle(ChatFormatting.GRAY)
            )
            describe(nearby).forEach { ChatUtils.send(Component.literal("  ").append(it)) }
        }
    }

    private val EQUIPMENT_SLOTS = listOf(
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    )
}
