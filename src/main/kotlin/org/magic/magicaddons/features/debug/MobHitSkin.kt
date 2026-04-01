package org.magic.magicaddons.features.debug

import com.google.gson.JsonParser
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnAttackEntityEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import java.net.URI
import java.util.*

object  MobHitSkin : Feature() {
    init {
        EventBus.register(this)
    }

    enum class MobHitInfoOption {
        SKIN_URL,
        SKIN_HASH,
        BOTH
    }

    override val id: String = "MobHitSkinDebug"
    override val displayName: String = "Mob Hit Skin Debug"
    override val tooltipMessage: String = "On next mob hit will cancel the actual event and print the skin hash (if applicable)"
    override val category: String = "debug"

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        if (!baseSetting.value) return
        event.canceled = true

        val target = event.target

        if (target !is PlayerEntity) {
            ChatUtils.sendWithPrefix("Not a player: ${target::class.simpleName}")
            return
        }

        val profile = target.gameProfile
        val textures = profile.properties["textures"]

        if (textures.isEmpty()) {
            ChatUtils.sendWithPrefix("No skin textures found")
            return
        }

        val value = textures.first().value ?: run {
            ChatUtils.sendWithPrefix("Texture value is null")
            return
        }

        try {
            val decodedJson = String(Base64.getDecoder().decode(value))

            val json = JsonParser.parseString(decodedJson).asJsonObject
            val url = json
                .getAsJsonObject("textures")
                .getAsJsonObject("SKIN")
                .get("url")
                .asString

            val hash = url.substringAfterLast("/")

            val clickableText = Text.literal("Click for skin url").setStyle(
                Style.EMPTY
                    .withClickEvent(
                    ClickEvent.OpenUrl(URI(url))
                    )
            )
            ChatUtils.sendWithPrefix(clickableText)
            ChatUtils.sendWithPrefix("Skin hash: $hash")

        } catch (_: Exception) {
            ChatUtils.sendWithPrefix("Failed to parse skin data")
        }
    }

}