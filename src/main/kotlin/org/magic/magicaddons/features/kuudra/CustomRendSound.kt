package org.magic.magicaddons.features.kuudra

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnAnyPlayerSwingEvent
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.EntityUtils.isEntityWearingArmorId
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object CustomRendSound : Feature() {
    init {
        EventBus.register(this)
    }

    var wornReaperArmorList: MutableSet<AbstractClientPlayerEntity> = mutableSetOf()
    var wornReaperTuxedoArmorList: MutableSet<AbstractClientPlayerEntity> = mutableSetOf()
    var lastPullTimeMs: Long? = null

    const val REND_COOLDOWN: Int = 500

    override val id: String = "CustomRendSound"
    override val displayName: String = "Custom Rend Sound"
    override val tooltipMessage: String = "Plays a custom selected sound when a rend pull is detected"
    override val category: String = "kuudra"
    override val baseSetting: BooleanSetting = BooleanSetting(
        key = "enabled",
        displayName = displayName,
        tooltip = tooltipMessage,
        value = false,
        children = listOf(
            TextSetting(
                key = "RendPullSoundPath",
                displayName = "Sound Path",
                tooltip = "The sound path for the rend sound",
                value = "minecraft:entity.goat.screaming.death"
            )
        )
    )

    // ELEGANT_TUXEDO_BOOTS | ELEGANT_TUXEDO_LEGGINGS | ELEGANT_TUXEDO_CHESTPLATE

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        if (!baseSetting.value) return
        val inKuudra = LocationAPI.island == SkyBlockIsland.KUUDRA
        if (!inKuudra) return
        if (!inKuudraLair()) return
        EntityUtils.entityInfoList?.forEach { entity ->
            if (entity.entity !is AbstractClientPlayerEntity) {
                return@forEach
            }
            if (!entity.armorStandTags.isNullOrEmpty()) {
                return@forEach
            }
            if (entity.entity in wornReaperTuxedoArmorList) {
                return@forEach
            }

            if (isEntityWearingArmorId("REAPER", entity.entity, false)){
                wornReaperArmorList.add(entity.entity)
            }
            if (entity.entity in wornReaperArmorList) {
                if (isEntityWearingArmorId("ELEGANT_TUXEDO", entity.entity, false)){
                    wornReaperTuxedoArmorList.add(entity.entity)
                }
            }
        }

    }



    @EventHandler
    fun onAnySwing(event: OnAnyPlayerSwingEvent) {
        if (!baseSetting.value) return
        val inKuudra = LocationAPI.island == SkyBlockIsland.KUUDRA
        if (!inKuudra) return
        val now = System.currentTimeMillis()
        if (lastPullTimeMs != null && now - lastPullTimeMs!! < REND_COOLDOWN) return



        if (event.player !in wornReaperTuxedoArmorList){
            return
        }

        var itemCorrect = false
        event.mainHandStack.components.forEach {
            if (it.type.toString() != "minecraft:custom_data") return@forEach
            if (it.value.toString().contains("BONE_BOOMERANG")) itemCorrect = true
            if (it.value.toString().contains("TERMINATOR")) itemCorrect = true
            if (it.value.toString().contains("ATOMSPLIT_KATANA")) itemCorrect = true
        }
        if (!itemCorrect) return

        wornReaperArmorList.remove(event.player.entity)
        wornReaperTuxedoArmorList.remove(event.player.entity)
        ChatUtils.sendWithPrefix("${event.player.displayName?.siblings?.get(1)?.string} Pulled!")
        // SoundEvents.ENTITY_GOAT_SCREAMING_DEATH.id
        // minecraft:entity.goat.screaming.death
        // baseSetting.getChild<TextSetting>("RendPullSoundPath")?.value ?: "mob.goat.death.screamer"
        val goatSound = PositionedSoundInstance(
            Identifier.of(baseSetting.getChild<TextSetting>("RendPullSoundPath")?.value ?: "minecraft:mob.goat.death.screamer"),
            SoundCategory.PLAYERS,
            1F,
            1F,
            Random.create(0.toLong()),
            false,
            0,
            SoundInstance.AttenuationType.NONE,
            0.toDouble(), 0.toDouble(), 0.toDouble(),
            false
        )
        MinecraftClient.getInstance().soundManager.play(goatSound)
        lastPullTimeMs = now

    }

    fun inKuudraLair(): Boolean{
        val player = MinecraftClient.getInstance().player ?: return false
        val vec1 = Vec3d(-60.0, 40.0, -142.0)
        val vec2 = Vec3d(-135.0, 1.0, -65.0)
        val box = Box(vec1, vec2)
        return box.contains(Vec3d(player.x, player.y, player.z))
    }





}