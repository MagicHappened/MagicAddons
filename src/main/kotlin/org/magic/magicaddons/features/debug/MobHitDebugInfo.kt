package org.magic.magicaddons.features.debug

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
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
 * Prints what the client knows about whatever was hit, as one chat line whose hover carries the
 * detail and whose click copies the whole thing as json.
 */
object MobHitDebugInfo : Feature() {
    init {
        EventBus.register(this)
    }

    override val id: String = "MobHitDebug"
    override val displayName: String = "Mob Hit Debug"
    override val description: String = "On next mob hit will cancel the actual event and print debug information"
    override val category: String = "debug"

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false
        //todo add the select option to return
    )

    /** How far around the hit entity to look for the stands and displays that belong to it. */
    private const val NEARBY_RADIUS: Double = 0.5
    private const val NEARBY_HEIGHT: Double = 2.0

    private val GSON = GsonBuilder().setPrettyPrinting().create()

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        if (!baseSetting.value) return
        event.canceled = true

        report(event.target)
    }

    /** One item of an entity's equipment, as the debug cares about it. */
    private data class ItemLine(
        val slot: String,
        val id: String,
        val dyeColor: Int?,
        val skullHash: String?
    )

    /** One entity, the hit one or something standing in it. */
    private data class EntityLine(
        val type: String,
        val name: String?,
        val invisible: Boolean,
        val marker: Boolean?,
        val skinHash: String?,
        val items: List<ItemLine>
    )

    private fun report(entity: Entity) {
        val subject = describe(entity)
        val nearby = nearbyEntities(entity)
        val neighbours = nearby.map(::describe)

        val summary = Component.literal(summaryText(subject, neighbours.size))
            .setStyle(
                Style.EMPTY.withHoverEvent(HoverEvent.ShowText(detailText(subject, neighbours)))
            )

        summary.append(clickable("[copy]", ChatFormatting.GREEN, "Copies the full dump as json",
            ClickEvent.CopyToClipboard(json(entity, subject, neighbours))))

        if (entity is Player) {
            PlayerUtils.getSkinUrl(entity)?.let { url ->
                summary.append(clickable("[skin]", ChatFormatting.AQUA, url, ClickEvent.OpenUrl(URI(url))))
            }
            subject.skinHash?.let { hash ->
                summary.append(clickable("[Skin Hash]", ChatFormatting.YELLOW, hash,
                    ClickEvent.CopyToClipboard(hash)))
            }
        }

        ChatUtils.sendWithPrefix(summary)
    }

    /** Name, type, whether it can be seen, what it wears and how much is standing in it. */
    private fun summaryText(subject: EntityLine, neighbours: Int): String = buildString {
        append(subject.name ?: subject.type)
        append(" · ").append(subject.type)
        append(" · ").append(if (subject.invisible) "invisible" else "visible")
        if (subject.items.isNotEmpty()) append(" · ").append("${subject.items.size} worn")
        append(" · ").append("$neighbours nearby")
        append(" ")
    }

    private fun clickable(
        label: String,
        color: ChatFormatting,
        hover: String,
        click: ClickEvent
    ): Component = Component.literal(" $label").setStyle(
        Style.EMPTY
            .withColor(color)
            .withClickEvent(click)
            .withHoverEvent(HoverEvent.ShowText(Component.literal(hover)))
    )

    /** The hover: the hit entity in full, then a line for each thing standing in it. */
    private fun detailText(subject: EntityLine, neighbours: List<EntityLine>): Component {
        val text = Component.literal("")

        appendEntity(text, subject)

        text.append(Component.literal("\nNearby (${neighbours.size})").withStyle(ChatFormatting.GRAY))
        neighbours.forEach { neighbour ->
            text.append(Component.literal("\n  "))
            appendEntity(text, neighbour, short = true)
        }

        return text
    }

    private fun appendEntity(text: MutableComponent, line: EntityLine, short: Boolean = false) {
        if (short) {
            text.append(Component.literal(line.type).withStyle(ChatFormatting.WHITE))
            line.name?.let { text.append(Component.literal("  \"$it\"").withStyle(ChatFormatting.GRAY)) }
            if (line.invisible) text.append(Component.literal("  invisible").withStyle(ChatFormatting.DARK_GRAY))
        } else {
            text.append(Component.literal(line.name ?: "no name").withStyle(ChatFormatting.WHITE))
            text.append(Component.literal(" — ${line.type}").withStyle(ChatFormatting.GRAY))
            text.append(Component.literal("\ninvisible: ${yesNo(line.invisible)}").withStyle(ChatFormatting.GRAY))
            line.marker?.let {
                text.append(Component.literal("   marker: ${yesNo(it)}").withStyle(ChatFormatting.GRAY))
            }
            line.skinHash?.let {
                text.append(Component.literal("\nskin  ${shorten(it)}").withStyle(ChatFormatting.GRAY))
            }
        }

        line.items.forEach { item ->
            text.append(Component.literal("\n${if (short) "    " else "  "}${item.slot}  ${item.id}")
                .withStyle(ChatFormatting.WHITE))

            item.dyeColor?.let { rgb ->
                text.append(Component.literal("  ■").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))))
                text.append(Component.literal(" #%06X".format(rgb and 0xFFFFFF)).withStyle(ChatFormatting.GRAY))
            }

            item.skullHash?.let {
                text.append(Component.literal("  ${shorten(it)}").withStyle(ChatFormatting.GRAY))
            }
        }
    }

    private fun describe(entity: Entity): EntityLine = EntityLine(
        type = entity.type.toString().removePrefix("entity.minecraft."),
        name = entity.customName?.string,
        invisible = entity.isInvisible,
        marker = (entity as? ArmorStand)?.isMarker,
        skinHash = (entity as? Player)?.let { PlayerUtils.getSkinHash(it) },
        items = items(entity)
    )

    private fun items(entity: Entity): List<ItemLine> = when (entity) {
        is LivingEntity -> ARMOR_SLOTS.mapNotNull { slot ->
            itemLine(slot.getName(), entity.getItemBySlot(slot))
        }

        is Display.ItemDisplay -> listOfNotNull(itemLine("item", entity.itemStack))

        else -> emptyList()
    }

    private fun itemLine(slot: String, stack: ItemStack): ItemLine? {
        if (stack.isEmpty) return null

        return ItemLine(
            slot = slot,
            id = stack.item.toString(),
            dyeColor = stack.get(DataComponents.DYED_COLOR)?.rgb,
            skullHash = PlayerUtils.getSkinHash(stack)
        )
    }

    private fun nearbyEntities(entity: Entity): List<Entity> {
        val level = Minecraft.getInstance().level ?: return emptyList()

        return level.getEntities(
            entity,
            entity.boundingBox.inflate(NEARBY_RADIUS, NEARBY_HEIGHT, NEARBY_RADIUS)
        ).filter { it !== entity }
    }

    /** The whole dump, for the clipboard: full hashes, positions and every flag. */
    private fun json(entity: Entity, subject: EntityLine, neighbours: List<EntityLine>): String {
        val root = entityJson(subject)

        root.addProperty("uuid", entity.uuid.toString())
        root.addProperty("pos", "%.2f %.2f %.2f".format(entity.x, entity.y, entity.z))

        val nearbyArray = JsonArray()
        neighbours.forEach { nearbyArray.add(entityJson(it)) }
        root.add("nearby", nearbyArray)

        return GSON.toJson(root)
    }

    private fun entityJson(line: EntityLine): JsonObject {
        val obj = JsonObject()

        obj.addProperty("type", line.type)
        obj.addProperty("name", line.name)
        obj.addProperty("invisible", line.invisible)
        line.marker?.let { obj.addProperty("marker", it) }
        line.skinHash?.let { obj.addProperty("skinHash", it) }

        val items = JsonArray()
        line.items.forEach { item ->
            val itemObj = JsonObject()
            itemObj.addProperty("slot", item.slot)
            itemObj.addProperty("id", item.id)
            item.dyeColor?.let { itemObj.addProperty("dye", "#%06X".format(it and 0xFFFFFF)) }
            item.skullHash?.let { itemObj.addProperty("skullHash", it) }
            items.add(itemObj)
        }
        obj.add("equipment", items)

        return obj
    }

    private val ARMOR_SLOTS = listOf(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND
    )

    /** A hash as the hover shows one: enough of both ends to recognise it. */
    private fun shorten(hash: String): String =
        if (hash.length <= 20) hash else "${hash.take(8)}…${hash.takeLast(6)}"

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"
}
