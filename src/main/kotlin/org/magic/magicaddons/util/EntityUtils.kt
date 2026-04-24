package org.magic.magicaddons.util

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.events.world.OnWorldTickEvent
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import kotlin.math.sqrt

object EntityUtils {
    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    interface HighlightSource {
        val highlightPriority: Int
        val highlightColor: Int
    }


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

    fun hasSource(entity: Entity, source: HighlightSource): Boolean {
        return highlightMap[entity]?.contains(source) == true
    }

    var entityInfoList: List<EntityInfo>? = null

    private var entityMapPrev: Map<String, EntityInfo> = emptyMap()
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
    private fun onWorldTick(event: OnWorldTickEvent){
        update()
    }

    private fun update() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        val newList = mutableListOf<EntityInfo>()
        val newMap = mutableMapOf<String, EntityInfo>()

        level.entitiesForRendering().forEach { entity ->

            val nearby = level.getEntities(entity,entity.boundingBox.inflate(0.5, 2.0, 0.5))

            if ((entity is ArmorStand || entity is Display) && isNearMeaningfulEntity(level,entity, nearby)) {
                return@forEach
            }

            val informationEntities = if (entity is LivingEntity) {
                nearby
                    .filter {
                        it !== entity && (
                                (it is ArmorStand && it.hasCustomName()) ||
                                it is Display
                                )
                    }
            } else null

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

            val oldTags = oldInfo.informationEntities?.toSet()
            val newTags = newInfo.informationEntities?.toSet()

            if (oldTags != newTags) {
                updatedEntities += newInfo
            }
        }


        if (addedEntities.isNotEmpty()) {
            EventBus.post(OnEntityAdded(addedEntities))
        }
        if (removedEntities.isNotEmpty()) {
            EventBus.post(OnEntityRemoved(removedEntities))
        }

        if (updatedEntities.isNotEmpty()) {
            EventBus.post(OnEntityUpdated(updatedEntities))
        }
        // update state
        entityInfoList = newList
        entityMapPrev = entityMapCurr
        entityMapCurr = newMap
    }

    private fun isNearMeaningfulEntity(world: ClientLevel, entity: Entity, nearby: List<Entity>): Boolean {
        val box = entity.boundingBox.inflate(2.0)

        return world.getEntities(
            entity,
            box
        ).any { entity ->
            when (entity) {
                is ArmorStand -> false
                is Display -> false
                is LivingEntity -> true
                else -> false
            }
        }
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
    fun hasArmorId(stack: ItemStack, id: String, suffix: String): Boolean {
        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return false
        val tag = customData.copyTag()

        val armorId = tag.getString("id")
        return armorId.orElse(null) == "${id}_$suffix"
    }

}