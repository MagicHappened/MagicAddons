package org.magic.magicaddons.features.mining

import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.ui.InventoryClosedEvent
import org.magic.magicaddons.events.world.AddParticleEvent
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object HidePowderCoatingParticles : Feature() {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    var armorDirty = true
    var equippedDivan = false

    @EventHandler
    fun onAddParticle(event: AddParticleEvent){
        if (!baseSetting.value) return
        if (!event.packet.particle.type.equals(ParticleTypes.DUST)) return


        val dustPos = Vec3(event.packet.x, event.packet.y, event.packet.z)
        val distance: Double = dustPos.distanceTo(Minecraft.getInstance().player?.position() ?: return)
        if (distance > 4.0) return
        updateEquippedDivan()
        if (!equippedDivan) return
        event.canceled = true
    }

    @Subscription
    fun onContainerScreen(event: ContainerInitializedEvent){
        armorDirty = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryClosedEvent){
        armorDirty = true
    }


    fun updateEquippedDivan(){
        if (!armorDirty) return
        val player = Minecraft.getInstance().player ?: return
        equippedDivan = player.getItemBySlot(EquipmentSlot.HEAD).getSkyBlockId() == SkyBlockItemId.item("DIVAN_HELMET") &&
                player.getItemBySlot(EquipmentSlot.CHEST).getSkyBlockId() == SkyBlockItemId.item("DIVAN_CHESTPLATE") &&
        player.getItemBySlot(EquipmentSlot.LEGS).getSkyBlockId() == SkyBlockItemId.item("DIVAN_LEGGINGS") &&
                player.getItemBySlot(EquipmentSlot.FEET).getSkyBlockId() == SkyBlockItemId.item("DIVAN_BOOTS")

    }


    override val id: String = "HidePowderCoatingParticles"
    override val displayName: String = "Powder Coating Hider"
    override val tooltipMessage: String = "Hides powder coating particles when divan armor is equipped"
    override val category: String = "mining"

    override val baseSetting: BooleanSetting = BooleanSetting(
            displayName = displayName,
            tooltip = tooltipMessage,
            value = false
        )


}