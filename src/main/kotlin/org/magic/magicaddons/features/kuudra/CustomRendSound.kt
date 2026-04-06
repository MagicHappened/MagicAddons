package org.magic.magicaddons.features.kuudra

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.DrawStyle
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.AddParticleEvent
import org.magic.magicaddons.features.Feature
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object CustomRendSound : Feature() {
    init {
        EventBus.register(this)
    }



    var lastPullTimeMs: Long? = null

    var redstoneBlockParticleAmountCount = 0
    var lastRedstoneBlockParticleTimestamp: Long? = null

    const val REND_COOLDOWN: Int = 500
    const val VARIENCE_ALLOWED_MS: Int = 100

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

    @EventHandler
    fun onAddParticleEvent(event: AddParticleEvent) {
        if (!baseSetting.value) return

        val blockStateParticleEffect = event.parameters as? BlockStateParticleEffect ?: return
        val block = blockStateParticleEffect.blockState.block

        if (Registries.BLOCK.getId(block).path != "redstone_block") {
            return
        }

        val now = System.currentTimeMillis()

        if (lastPullTimeMs != null && now - lastPullTimeMs!! < REND_COOLDOWN) return

        if (lastRedstoneBlockParticleTimestamp == null) {
            lastRedstoneBlockParticleTimestamp = now
        }

        if (now - (lastRedstoneBlockParticleTimestamp ?: 0L) < VARIENCE_ALLOWED_MS) {
            redstoneBlockParticleAmountCount++
            lastRedstoneBlockParticleTimestamp = now
        } else {
            redstoneBlockParticleAmountCount = 0
            lastRedstoneBlockParticleTimestamp = now
        }

        if (redstoneBlockParticleAmountCount < 20) return

        val inKuudra = LocationAPI.island == SkyBlockIsland.KUUDRA // todo change to kuudra once live

        if (!inKuudra)
            return

        val player = MinecraftClient.getInstance().player ?: return
        val vec1 = Vec3d(-60.0, 40.0, -142.0)
        val vec2 = Vec3d(-135.0, 1.0, -65.0)
        val box = Box(vec1, vec2)
        if (!box.contains(Vec3d(player.x, player.y, player.z))) return


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
        redstoneBlockParticleAmountCount = 0
        lastRedstoneBlockParticleTimestamp = null

        /*
        if (blockState != null) {
            val blockId = Registries.BLOCK.getId(blockState.block)

            ChatUtils.sendWithPrefix(
                "Particle: $id | Block: $blockId | State: $blockState"
            )
        } else {
            ChatUtils.sendWithPrefix("Particle: $id")
        }
            [MagicAddons] Particle: minecraft:block | Block: minecraft:redstone_block | State: Block{minecraft:redstone_block} (150)

         */

    }


}