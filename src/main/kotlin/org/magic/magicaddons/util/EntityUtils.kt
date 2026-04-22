package org.magic.magicaddons.util

import com.google.common.eventbus.Subscribe
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
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
import org.magic.magicaddons.features.combat.HighlightMobs
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyOnSkyBlock
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
import kotlin.math.sqrt

object EntityUtils {
    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    val highlightEntityList: MutableSet<Entity> = mutableSetOf()

    var entityInfoList: List<EntityInfo>? = null

    private var entityMapPrev: Map<String, EntityInfo> = emptyMap()
    private var entityMapCurr: Map<String, EntityInfo> = emptyMap()

    private val addedEntities = mutableListOf<EntityInfo>()
    private val removedEntities = mutableListOf<EntityInfo>()
    private val updatedEntities = mutableListOf<EntityInfo>()

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
            if (entity is ArmorStand && isNearPlayerEntity(level, entity)) return@forEach

            val armorStandTags = if (entity is Player) {
                level.getEntities(null, entity.boundingBox.inflate(0.5, 2.0, 0.5))
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

        // update state
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
        val dispatcher = Minecraft.getInstance().entityRenderDispatcher


        event.poseStack.pushPose()

        for (entity in (HighlightMobs.highlightedEntityList ?: return)) { //todo change to add more features to highlight somehow

            if (entity.level() != level) continue
            if (!entity.isAlive) continue

            val renderer = dispatcher.getRenderer(entity)
            if (renderer !is LivingEntityRenderer<*,*,*>) continue
            try {
                renderer as? LivingEntityRenderer<Entity, LivingEntityRenderState, *> ?: continue
            }
            catch (t: Throwable) {
                ChatUtils.sendWithPrefix("caught unsafe cast ${t.message}")
                continue
            }


            val state = renderer.createRenderState(entity, event.partialTicks)

            val vertexConsumer = event.buffer.getBuffer(
                RenderTypes.outline(renderer.getTextureLocation(state))
            )

            val cam = event.cameraPosition

            event.poseStack.pushPose()

            // camera-relative transform
            event.poseStack.translate(
                entity.x - cam.x,
                entity.y - cam.y,
                entity.z - cam.z
            )

            // IMPORTANT: apply entity rotation
            event.poseStack.mulPose(
                org.joml.Quaternionf()
                    .rotateY(-Math.toRadians(entity.yRot.toDouble()).toFloat())
            )



            renderer.model.renderToBuffer(
                event.poseStack,
                vertexConsumer,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF.toInt()
            )

            event.poseStack.popPose()
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
        val tag = customData.copyTag() // CompoundTag

        val armorId = tag.getString("id") // or whatever key you're using
        return armorId.orElse(null) == "${id}_$suffix"
    }

}