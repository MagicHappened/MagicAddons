package org.magic.magicaddons.features.debug

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
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
import org.magic.magicaddons.events.interact.AttackEntityEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils.typePath
import org.magic.magicaddons.util.PlayerUtils
import java.net.URI

object MobHitDebugInfo : Feature() {
    init {
        EventBus.register(this)
    }

    override val id: String = "MobHitDebug"
    override val displayName: String = "Mob Hit Debug"
    override val description: String = "Hit a mob to print what it is made of, for asking a dev to add it to Hypixel Mobs"
    override val category: String = "debug"

    private val letHitThroughSetting = BooleanSetting(
        key = "LetHitThrough",
        displayName = "Let Hit Through",
        description = "The hit still lands on the mob instead of being cancelled",
        value = false
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(letHitThroughSetting)
    )

    private const val NEARBY_RADIUS: Double = 0.5
    private const val NEARBY_HEIGHT: Double = 2.0

    private val GSON = GsonBuilder().setPrettyPrinting().create()

    @EventHandler
    fun onAttackEntity(event: AttackEntityEvent) {
        if (!baseSetting.value) return
        event.canceled = !letHitThroughSetting.value

        report(event.target)
    }

    private data class EntityEquipmentLine(
        val slot: String,
        val id: String,
        val dyeColor: Int?,
        val skullHash: String?
    )

    private data class EntityLine(
        val type: String,
        val name: String?,
        val invisible: Boolean,
        val marker: Boolean?,
        val skinHash: String?,
        val equipmentLines: List<EntityEquipmentLine>
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
            ClickEvent.CopyToClipboard(json(entity, nearby))))

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
        if (subject.equipmentLines.isNotEmpty()) append(" · ").append("${subject.equipmentLines.size} worn")
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
                text.append(Component.literal("\nskin  ${shortenHash(it)}").withStyle(ChatFormatting.GRAY))
            }
        }

        line.equipmentLines.forEach { item ->
            text.append(Component.literal("\n${if (short) "    " else "  "}${item.slot}  ${item.id}")
                .withStyle(ChatFormatting.WHITE))

            item.dyeColor?.let { rgb ->
                text.append(Component.literal("  ■").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))))
                text.append(Component.literal(" #%06X".format(rgb and 0xFFFFFF)).withStyle(ChatFormatting.GRAY))
            }

            item.skullHash?.let {
                text.append(Component.literal("  ${shortenHash(it)}").withStyle(ChatFormatting.GRAY))
            }
        }
    }

    private fun describe(entity: Entity): EntityLine = EntityLine(
        type = entity.typePath(),
        name = entity.customName?.string,
        invisible = entity.isInvisible,
        marker = (entity as? ArmorStand)?.isMarker,
        skinHash = (entity as? Player)?.let { PlayerUtils.getSkinHash(it) },
        equipmentLines = equipmentLinesFor(entity)
    )

    private fun equipmentLinesFor(entity: Entity): List<EntityEquipmentLine> = when (entity) {
        is LivingEntity -> ARMOR_SLOTS.mapNotNull { slot ->
            equipmentLine(slot.getName(), entity.getItemBySlot(slot))
        }

        is Display.ItemDisplay -> listOfNotNull(equipmentLine("item", entity.itemStack))

        else -> emptyList()
    }

    private fun equipmentLine(slotName: String, stack: ItemStack): EntityEquipmentLine? {
        if (stack.isEmpty) return null

        return EntityEquipmentLine(
            slot = slotName,
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

    private fun json(entity: Entity, neighbours: List<Entity>): String {
        val root = entityJson(entity)

        val nearbyArray = JsonArray()
        neighbours.forEach { nearbyArray.add(entityJson(it)) }
        root.add("nearby", nearbyArray)

        return GSON.toJson(root)
    }

    private fun entityJson(entity: Entity): JsonObject {
        val obj = JsonObject()

        obj.addProperty("type", entity.typePath())
        obj.addProperty("name", entity.customName?.string)
        obj.addProperty("uuid", entity.uuid.toString())
        obj.addProperty("networkId", entity.id)
        obj.addProperty("pos", "%.2f %.2f %.2f".format(entity.x, entity.y, entity.z))
        obj.addProperty("rotation", "%.1f %.1f".format(entity.yRot, entity.xRot))
        obj.addProperty("invisible", entity.isInvisible)
        obj.addProperty("glowing", entity.isCurrentlyGlowing)
        obj.addProperty("pose", entity.pose.name)
        obj.addProperty("size", "%.2f x %.2f".format(entity.bbWidth, entity.bbHeight))
        (entity as? ArmorStand)?.let { obj.addProperty("marker", it.isMarker) }
        (entity as? Player)?.let { player ->
            PlayerUtils.getSkinHash(player)?.let { obj.addProperty("skinHash", it) }
        }
        (entity as? LivingEntity)?.let { living ->
            obj.addProperty("scale", living.scale)
            obj.add("attributes", attributesAsJson(living))
        }

        obj.add("components", componentsJson(entity))
        obj.add("syncedData", syncedDataJson(entity))
        obj.add("equipment", equipmentJson(entity))

        return obj
    }

    /**
     * all the unique components such as shulker color parrot varient etc...
     */
    private fun componentsJson(entity: Entity): JsonObject {
        val obj = JsonObject()

        BuiltInRegistries.DATA_COMPONENT_TYPE.forEach { type ->
            val value = runCatching { entity.get(type) }.getOrNull() ?: return@forEach

            componentName(type)?.let { obj.addProperty(it, value.toString()) }
        }

        return obj
    }

    private fun attributesAsJson(entity: LivingEntity): JsonObject {
        val obj = JsonObject()

        entity.attributes.syncableAttributes.forEach { instance ->
            val name = BuiltInRegistries.ATTRIBUTE.getKey(instance.attribute.value()) ?: return@forEach

            obj.addProperty(name.toString(), instance.value)
        }

        return obj
    }

    private fun syncedDataJson(entity: Entity): JsonObject {
        val obj = JsonObject()

        entity.entityData.nonDefaultValues?.forEach { entry ->
            obj.addProperty(entry.id().toString(), entry.value().toString())
        }

        return obj
    }

    private fun equipmentJson(entity: Entity): JsonArray {
        val array = JsonArray()

        when (entity) {
            is LivingEntity -> ARMOR_SLOTS.forEach { slot ->
                equipmentJson(slot.getName(), entity.getItemBySlot(slot))?.let { array.add(it) }
            }

            is Display.ItemDisplay -> equipmentJson("item", entity.itemStack)?.let { array.add(it) }
        }

        return array
    }

    private fun equipmentJson(slot: String, stack: ItemStack): JsonObject? {
        if (stack.isEmpty) return null

        val obj = JsonObject()

        obj.addProperty("slot", slot)
        obj.addProperty("id", stack.item.toString())
        obj.addProperty("count", stack.count)
        obj.addProperty("name", stack.hoverName.string)

        val components = JsonObject()
        stack.components.forEach { component ->
            componentName(component.type())?.let { components.addProperty(it, component.value().toString()) }
        }
        obj.add("components", components)

        return obj
    }

    private fun componentName(type: DataComponentType<*>): String? =
        BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)?.toString()

    private val ARMOR_SLOTS = listOf(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND
    )

    private const val SHORT_HASH_LENGTH: Int = 20

    private fun shortenHash(hash: String): String =
        if (hash.length <= SHORT_HASH_LENGTH) hash else "${hash.take(8)}…${hash.takeLast(6)}"

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"
}
