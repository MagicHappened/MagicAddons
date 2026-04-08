package org.magic.magicaddons.util

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.DrawStyle
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.EntityList
import net.minecraft.world.debug.gizmo.GizmoDrawing
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.extensions.armorStacks
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.kuudra.CustomRendSound
import kotlin.math.sqrt

object EntityUtils {


    @JvmStatic
    var renderTickCounter: RenderTickCounter? = null

    //entity list from world tick
    private var entityList: EntityList? = null

    //maintained entity info
    var entityInfoList: List<EntityInfo>? = null

    private var entityMapPrev: Map<String, EntityInfo> = emptyMap()
    private var entityMapCurr: Map<String, EntityInfo> = emptyMap()

    private val addedEntities = mutableListOf<EntityInfo>()
    private val removedEntities = mutableListOf<EntityInfo>()
    private val updatedEntities = mutableListOf<EntityInfo>()



    @JvmStatic
    fun onWorldTick(entityList: EntityList) {

        if (!HighlightMobs.baseSetting.value && !CustomRendSound.baseSetting.value) return // change this to include more settings that depend on world tick

        this.entityList = entityList
        EventBus.post(OnWorldTickEvent())
        update()

    }

    private fun update() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val world = client.world ?: return

        val newList = mutableListOf<EntityInfo>()
        val newMap = mutableMapOf<String, EntityInfo>()

        entityList?.forEach { entity ->
            if (entity is ArmorStandEntity && isNearPlayerEntity(world, entity)) return@forEach

            val armorStandTags = if (entity is PlayerEntity) {
                world.getOtherEntities(null, entity.boundingBox.expand(0.5, 2.0, 0.5))
                    .filterIsInstance<ArmorStandEntity>()
                    .mapNotNull { it.customName?.string }
            } else null

            val distance = sqrt(entity.squaredDistanceTo(player))

            val info = EntityInfo(entity, armorStandTags, distance)
            newList += info
            newMap[entity.uuidAsString] = info
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

            val oldTags = oldInfo.armorStandTags?.toSet()
            val newTags = newInfo.armorStandTags?.toSet()

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

        // Update state
        entityInfoList = newList
        entityMapPrev = entityMapCurr
        entityMapCurr = newMap
    }

    private fun isNearPlayerEntity(world: ClientWorld, armorStand: ArmorStandEntity): Boolean {
        return world.players.any { it.squaredDistanceTo(armorStand) < 2.0 }
    }

    fun renderEntityBoundingBox(entity: Entity) {
        val box = entity.getDimensions(entity.pose).getBoxAt(entity.getLerpedPos(renderTickCounter?.getTickProgress(false) ?: 0F))

        GizmoDrawing.box(
            box,
            DrawStyle.stroked(0xFF00FF00.toInt(), 4f)
        ).ignoreOcclusion()
    }

    fun isEntityWearingArmorId(id: String, entity: AbstractClientPlayerEntity, searchHelmet: Boolean): Boolean{
        var correctFeet = false
        var correctLegs = false
        var correctChest = false
        var correctHelmet = false
        run feet@ {
            entity.armorStacks.get(EquipmentSlot.FEET).components.forEach { component ->
                if (component.type.toString() == "minecraft:custom_data") {
                    if (component.value.toString().contains("${id}_BOOTS")) {
                        correctFeet = true
                        return@feet
                    }
                }
            }
        }

        if (!correctFeet) return false
        run legs@{
            entity.armorStacks.get(EquipmentSlot.LEGS).components.forEach { component ->
                if (component.type.toString() == "minecraft:custom_data") {
                    if (component.value.toString().contains("${id}_LEGGINGS")) {
                        correctLegs = true
                        return@legs
                    }
                }
            }
        }
        if (!correctLegs) return false

        run chest@{
            entity.armorStacks.get(EquipmentSlot.CHEST).components.forEach { component ->
                if (component.type.toString() == "minecraft:custom_data") {
                    if (component.value.toString().contains("${id}_CHESTPLATE")) {
                        correctChest = true
                        return@chest
                    }
                }
            }
        }
        if (!searchHelmet) return correctChest
        run helmet@{
            entity.armorStacks.get(EquipmentSlot.CHEST).components.forEach { component ->
                if (component.type.toString() == "minecraft:custom_data") {
                    if (component.value.toString().contains("${id}_CHESTPLATE")) {
                        correctHelmet = true
                        return@helmet
                    }
                }
            }


        }
        return correctHelmet
    }

}