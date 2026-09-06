package org.magic.magicaddons.features.kuudra

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.AnyPlayerSwingEvent
import org.magic.magicaddons.events.world.WorldTickEvent
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

    val wornReaperArmorList: MutableSet<Player> = mutableSetOf()
    val wornReaperTuxedoArmorList: MutableSet<Player> = mutableSetOf()
    var lastPullTimeMs: Long? = null

    const val REND_COOLDOWN: Int = 500

    /** Which part of a player's display name holds the name itself, after the level tag. */
    private const val NAME_SIBLING_INDEX: Int = 1

    /** The two corners of Kuudra's lair. */
    private val LAIR_CORNER_A: Vec3 = Vec3(-60.0, 40.0, -142.0)
    private val LAIR_CORNER_B: Vec3 = Vec3(-135.0, 1.0, -65.0)
    private val LAIR_BOX: AABB = AABB(LAIR_CORNER_A, LAIR_CORNER_B)

    override val id: String = "CustomRendSound"
    override val displayName: String = "Custom Rend Sound"
    override val description: String = "Plays a custom selected sound when a rend pull is detected"
    override val category: String = "kuudra"
    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(
            TextSetting(
                key = "RendPullSoundPath",
                displayName = "Sound Path",
                description = "The sound path for the rend sound",
                value = "minecraft:entity.goat.screaming.death"
            )
        )
    )

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        if (!baseSetting.value) return
        val inKuudra = LocationAPI.island == SkyBlockIsland.KUUDRA
        if (!inKuudra) return
        if (!inKuudraLair()) return
        EntityUtils.entityInfoList?.forEach { entity ->
            if (entity.entity !is Player) {
                return@forEach
            }
            if (!entity.informationEntities.isNullOrEmpty()) { // no armor stands = real player
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
    fun onAnySwing(event: AnyPlayerSwingEvent) {
        if (!baseSetting.value) return
        val inKuudra = LocationAPI.island == SkyBlockIsland.KUUDRA
        if (!inKuudra) return
        val now = System.currentTimeMillis()
        if (lastPullTimeMs != null && now - lastPullTimeMs!! < REND_COOLDOWN) return

        val player = event.player

        if (player !in wornReaperTuxedoArmorList){
            return
        }

        var itemCorrect = false
        val stack = event.mainHandStack

        stack.components.forEach { component ->
            if (component.type.toString() != "minecraft:custom_data") return@forEach

            val value = component.value.toString()

            if (
                "BONE_BOOMERANG" in value ||
                "TERMINATOR" in value ||
                "ATOMSPLIT_KATANA" in value
            ) {
                itemCorrect = true
            }
        }

        if (!itemCorrect) return

        wornReaperArmorList.remove(event.player)
        wornReaperTuxedoArmorList.remove(event.player)
        ChatUtils.sendWithPrefix("${event.player.displayName.siblings[NAME_SIBLING_INDEX].string} Pulled!")
        val soundId = Identifier.parse(
            baseSetting.getChild<TextSetting>("RendPullSoundPath")?.value
                ?: "minecraft:entity.goat.screaming.death"
        )

        val goatSound = SimpleSoundInstance(
            soundId,
            SoundSource.PLAYERS,
            1f,
            1f,
            RandomSource.create(0L),
            false,
            0,
            SoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            false
        )
        Minecraft.getInstance().soundManager.play(goatSound)
        lastPullTimeMs = now
    }

    fun inKuudraLair(): Boolean{
        val player = Minecraft.getInstance().player ?: return false
        return LAIR_BOX.contains(Vec3(player.x, player.y, player.z))
    }
}