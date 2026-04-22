package org.magic.magicaddons.util

import com.google.common.eventbus.Subscribe
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.extensions.armorStacks
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.kuudra.CustomRendSound
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyOnSkyBlock
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
import kotlin.math.sqrt

object EntityUtils {
    init {
        SkyBlockAPI.eventBus.register(this)
    }

    val highlightEntityList: MutableSet<Entity> = mutableSetOf()

    private var entityList: List<Entity>? = null


    var entityInfoList: List<EntityInfo>? = null

    private var entityMapPrev: Map<String, EntityInfo> = emptyMap()
    private var entityMapCurr: Map<String, EntityInfo> = emptyMap()

    private val addedEntities = mutableListOf<EntityInfo>()
    private val removedEntities = mutableListOf<EntityInfo>()
    private val updatedEntities = mutableListOf<EntityInfo>()



    @JvmStatic
    fun onWorldTick(level: ClientLevel) {

        if (!HighlightMobs.baseSetting.value && !CustomRendSound.baseSetting.value) return // change this to include more settings that depend on world tick

        EventBus.post(OnWorldTickEvent())
        update(level)

    }




    private fun update(level: ClientLevel) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val world = client.level ?: return

        val newList = mutableListOf<EntityInfo>()
        val newMap = mutableMapOf<String, EntityInfo>()

        level.entitiesForRendering().forEach { entity ->
            if (entity is ArmorStand && isNearPlayerEntity(world, entity)) return@forEach

            val armorStandTags = if (entity is Player) {
                world.getEntities(null, entity.boundingBox.inflate(0.5, 2.0, 0.5))
                    .filterIsInstance<ArmorStand>()
                    .mapNotNull { it.customName?.string }
            } else null

            val distance = sqrt(entity.distanceToSqr(player))

            val info = EntityInfo(entity, armorStandTags, distance)
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

    private fun isNearPlayerEntity(world: ClientLevel, armorStand: ArmorStand): Boolean {
        return world.players().any { it.distanceToSqr(armorStand) < 2.0 }
    }

    @OnlyOnSkyBlock
    @Subscribe
    fun onRenderWorldEvent(event: RenderWorldEvent.AfterEntities) {

        val level = Minecraft.getInstance().level ?: return
        val camPos = event.cameraPosition
        val pose = event.poseStack

        val partialTick = Minecraft.getInstance().deltaTracker.gameTimeDeltaTicks

        for (entity in EntityUtils.highlightedEntityList) {

            // safety: entity may despawn
            if (entity.level() != level) continue
            if (!entity.isAlive) continue

            val box = entity.boundingBox.move(
                -camPos.x,
                -camPos.y,
                -camPos.z
            )

            pose.pushPose()

            GizmoDrawing.box(
                box,
                DrawStyle.stroked(0xFF00FF00.toInt(), 4f)
            ).ignoreOcclusion()

            pose.popPose()
        }
    }





    fun isEntityWearingArmorId(id: String, entity: Player, searchHelmet: Boolean): Boolean{
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