package org.magic.magicaddons.util

import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.features.FeatureManager
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.EntityAddedEvent
import org.magic.magicaddons.events.world.EntityRemovedEvent
import org.magic.magicaddons.events.world.EntityUpdatedEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import kotlin.math.sqrt

object EntityUtils {
    init {
        EventBus.register(this)
    }

    private const val NEARBY_RADIUS: Double = 0.5
    private const val NEARBY_HEIGHT: Double = 2.0

    private const val PLAYER_UUID_VERSION: Int = 4

    class HighlightMark(val name: String, val icon: ItemStack? = null)

    interface HighlightSource {
        val highlightPriority: Int

        /** the outline color for this entity, as ARGB */
        fun highlightColor(entity: Entity): Int

        /** whether to be funny or not */
        val throughWalls: Boolean get() = true

        /** which marking should be applied to this entity, defaulting to null */
        fun highlightMark(entity: Entity): HighlightMark? = null
    }

    @JvmStatic
    fun inSight(camera: Vec3, entity: Entity): Boolean {
        val level = entity.level()
        val now = level.gameTime

        if (sightCheckedAt != now) {
            sightCheckedAt = now
            inSight.clear()
        }

        return inSight.getOrPut(entity) { inSightRayCheck(level, camera, entity) }
    }

    private var sightCheckedAt: Long = -1
    private val inSight: MutableMap<Entity, Boolean> = mutableMapOf()


    private fun inSightRayCheck(level: Level, camera: Vec3, entity: Entity): Boolean {
        val box = entity.boundingBox
        val middleX = (box.minX + box.maxX) / 2
        val middleZ = (box.minZ + box.maxZ) / 2
        val points = listOf(
            Vec3(middleX, (box.minY + box.maxY) / 2, middleZ),
            Vec3(middleX, box.maxY - SIGHT_INSET, middleZ),
            Vec3(middleX, box.minY + SIGHT_INSET, middleZ)
        )

        return points.any { point ->
            val hit = level.clip(ClipContext(camera, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity))

            hit.type == HitResult.Type.MISS || hit.blockPos == BlockPos.containing(point)
        }
    }

    private const val SIGHT_INSET: Double = 0.1


    private val highlightMap: MutableMap<Entity, MutableSet<HighlightSource>> = mutableMapOf()

    @JvmStatic
    val resolvedMap: MutableMap<Entity, HighlightSource> = mutableMapOf()

    fun add(entity: Entity, source: HighlightSource) {
        val set = highlightMap.computeIfAbsent(entity) { mutableSetOf() }
        set.add(source)

        resolvedMap[entity] = set.maxByOrNull { source: HighlightSource -> source.highlightPriority }!!
    }

    fun remove(entity: Entity, source: HighlightSource) {
        val set = highlightMap[entity] ?: return

        set.remove(source)

        if (set.isEmpty()) {
            highlightMap.remove(entity)
            resolvedMap.remove(entity)
        } else {
            resolvedMap[entity] = set.maxByOrNull { source: HighlightSource -> source.highlightPriority }!!
        }
    }

    var entityInfoList: List<EntityInfo>? = null

    private var entityMapCurr: Map<String, EntityInfo> = emptyMap()

    private val addedEntities = mutableListOf<EntityInfo>()
    private val removedEntities = mutableListOf<EntityInfo>()
    private val updatedEntities = mutableListOf<EntityInfo>()

    fun removeAllForSource(source: HighlightSource) {
        val iterator = highlightMap.iterator()

        while (iterator.hasNext()) {
            val (entity, set) = iterator.next()

            if (set.remove(source)) {
                if (set.isEmpty()) {
                    iterator.remove()
                    resolvedMap.remove(entity)
                } else {
                    resolvedMap[entity] = set.maxByOrNull { source: HighlightSource -> source.highlightPriority }!!
                }
            }
        }
    }



    @EventHandler
    private fun onWorldTick(event: WorldTickEvent){
        update()
    }

    private fun update() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        val newList = mutableListOf<EntityInfo>()
        val newMap = mutableMapOf<String, EntityInfo>()

        val detailed = FeatureManager.features.any { it is HighlightFeature && it.baseSetting.value }

        level.entitiesForRendering().forEach { entity ->
            val informationEntities: List<Entity>?

            if (detailed) {
                val nearby = level.getEntities(entity, entity.boundingBox.inflate(NEARBY_RADIUS, NEARBY_HEIGHT, NEARBY_RADIUS))

                if ((entity is ArmorStand || entity is Display) && isNearMeaningfulEntity(entity, nearby)) {
                    return@forEach
                }


                informationEntities = nearby
                    .filter {
                        it !== entity && (
                                (it is ArmorStand && it.hasCustomName()) ||
                                it is Display
                                )
                    }
            } else {
                informationEntities = null
            }

            val distance = sqrt(entity.distanceToSqr(player))

            val info = EntityInfo(entity, informationEntities, distance)
            newList += info
            newMap[entity.uuid.toString()] = info
        }

        addedEntities.clear()
        removedEntities.clear()
        updatedEntities.clear()

        // detect added
        addedEntities += newMap.filterKeys { it !in entityMapCurr }.values

        // detect removed
        removedEntities += entityMapCurr.filterKeys { it !in newMap }.values

        // detect updated
        newMap.forEach { (uuid, newInfo) ->
            val oldInfo = entityMapCurr[uuid] ?: return@forEach

            // by name as well as by identity: skyblock reuses a name tag it already hung rather
            // than replacing it, so a tag whose text changed is the same entity in both sets
            if (oldInfo.tagSignature() != newInfo.tagSignature()) {
                updatedEntities += newInfo
            }
        }


        if (addedEntities.isNotEmpty()) {
            EventBus.post(EntityAddedEvent(addedEntities))
        }
        if (removedEntities.isNotEmpty()) {
            EventBus.post(EntityRemovedEvent(removedEntities))
        }

        if (updatedEntities.isNotEmpty()) {
            EventBus.post(EntityUpdatedEvent(updatedEntities))
        }
        // update state
        entityInfoList = newList
        entityMapCurr = newMap
    }

    private fun EntityInfo.tagSignature(): List<String> =
        informationEntities.orEmpty().map { "${it.id}:${it.customName?.string}" }


    private fun isNearMeaningfulEntity(entity: Entity, nearby: List<Entity>): Boolean {
        if (entity is ArmorStand && !entity.isInvisible) return false

        return nearby.any { other ->
            when (other) {
                is ArmorStand -> false
                is Display -> false
                is Player -> !isRealPlayer(other)
                is LivingEntity -> true
                else -> false
            }
        }
    }

    fun isRealPlayer(entity: Player): Boolean {
        return entity.uuid.version() == PLAYER_UUID_VERSION
    }

    /** The entity type's description id, "entity.minecraft.pig" for a pig. */
    fun Entity.typeId(): String = type.toString()

    /** The last part of the type id, "pig" for a pig. */
    fun Entity.typePath(): String = typeId().substringAfterLast('.')

    /** The skull texture an item display holds or an armor stand wears on its head, or null. */
    fun carriedSkullHash(entity: Entity): String? = when (entity) {
        is Display.ItemDisplay -> PlayerUtils.getSkinHash(entity.itemStack)
        is ArmorStand -> PlayerUtils.getHelmetHash(entity)
        else -> null
    }

    /** returns the entity that we actually want to highlight (eg item display for rat instead of zombie)
     */
    fun skullCarrier(info: EntityInfo, hash: String): Entity? {
        val entity = info.entity

        if (entity is LivingEntity && PlayerUtils.getHelmetHash(entity) == hash) {
            return entity
        }

        val carrier = info.informationEntities?.firstOrNull { carriedSkullHash(it) == hash }
            ?: return null

        return if (entity.isInvisible) carrier else entity
    }

    fun isEntityWearingArmorId(id: String, entity: Player, searchHelmet: Boolean): Boolean {

        val boots = entity.getItemBySlot(EquipmentSlot.FEET)
        if (!hasArmorId(boots, id, "BOOTS")) return false

        val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
        if (!hasArmorId(legs, id, "LEGGINGS")) return false

        val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
        if (!hasArmorId(chest, id, "CHESTPLATE")) return false

        if (!searchHelmet) return true

        val helmet = entity.getItemBySlot(EquipmentSlot.HEAD)
        return hasArmorId(helmet, id, "HELMET")
    }
    /** The slots a stand can carry a crop's parts in, head first since nearly all of them do. */
    private val CARRY_SLOTS: List<EquipmentSlot> =
        listOf(EquipmentSlot.HEAD, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)

    /**
     * The plain item the entity carries, as the slot it is in and "minecraft:gold_block", or null
     * when it carries only a skull or nothing.
     */
    /** Whether the entity carries anything at all. A plant's stands always do, a nameplate never does. */
    fun carriesAnything(entity: LivingEntity): Boolean =
        CARRY_SLOTS.any { !entity.getItemBySlot(it).isEmpty }

    fun heldItem(entity: LivingEntity): Pair<EquipmentSlot, String>? = CARRY_SLOTS
        .firstNotNullOfOrNull { slot ->
            val stack = entity.getItemBySlot(slot)
            if (stack.isEmpty || PlayerUtils.getSkinHash(stack) != null) null
            else slot to BuiltInRegistries.ITEM.getKey(stack.item).toString()
        }

    fun itemIdIn(entity: LivingEntity, slot: EquipmentSlot): String? {
        val stack = entity.getItemBySlot(slot)
        if (stack.isEmpty || PlayerUtils.getSkinHash(stack) != null) return null

        return BuiltInRegistries.ITEM.getKey(stack.item).toString()
    }

    fun itemStackOf(itemId: String): ItemStack? =
        runCatching { BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(null) }
            .getOrNull()
            ?.let { ItemStack(it) }

    fun hasArmorId(stack: ItemStack, id: String, suffix: String): Boolean {
        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return false
        val tag = customData.copyTag()

        val armorId = tag.getString("id")
        return armorId.orElse(null) == "${id}_$suffix"
    }

}