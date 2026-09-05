package org.magic.magicaddons.util

import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.features.FeatureManager
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

        /**
         * Outline colour for this entity, as ARGB. Takes the entity so one source can colour
         * treasure and mobs differently.
         */
        fun highlightColor(entity: Entity): Int
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

        // the neighbour query behind every entity is what the highlight features read tags from;
        // with none of them on, only who came and went is worth knowing, and that needs no query
        val detailed = FeatureManager.features.any { it is HighlightFeature && it.baseSetting.value }

        level.entitiesForRendering().forEach { entity ->
            val informationEntities: List<Entity>?

            if (detailed) {
                val nearby = level.getEntities(entity, entity.boundingBox.inflate(0.5, 2.0, 0.5))

                if ((entity is ArmorStand || entity is Display) && isNearMeaningfulEntity(entity, nearby)) {
                    return@forEach
                }

                // collected for every entity, not just mobs: a lot of entities are an item display with a
                // name tag next to it and nothing else, and that name tag is all we know about them
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

    /** What the tags around an entity say, so a tag that was rewritten counts as a change. */
    private fun EntityInfo.tagSignature(): List<String> =
        informationEntities.orEmpty().map { "${it.id}:${it.customName?.string}" }

    /**
     * Whether this stand or display is decoration belonging to a mob standing beside it, rather than
     * a thing in its own right.
     *
     * A stand has to be invisible to count: a label is invisible and only its name or its head is
     * drawn, while a mineshaft corpse is a visible stand wearing armour and stays its own entity
     * however many mobs walk past it. The box is generous upwards, since a name tag floats over the
     * mob's head, and tight sideways, so something merely standing next to a mob is left alone.
     */
    private fun isNearMeaningfulEntity(entity: Entity, nearby: List<Entity>): Boolean {
        if (entity is ArmorStand && !entity.isInvisible) return false

        // the same neighbours the caller already asked the world for, rather than a second query
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
        return entity.uuid.version() == 4
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