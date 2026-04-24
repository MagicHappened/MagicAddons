package org.magic.magicaddons.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.platform.properties
import java.util.*

object PlayerUtils {

    private val skinCache = mutableMapOf<String, SkinData>()

    data class SkinData(
        val json: JsonObject,
        val url: String,
        val hash: String
    )

    private fun getSkinDataFromValue(value: String?): SkinData? {
        if (value == null) return null

        skinCache[value]?.let { return it }

        return try {
            val decodedJson = String(Base64.getDecoder().decode(value))
            val json = JsonParser.parseString(decodedJson).asJsonObject

            val url = json.getAsJsonObject("textures")
                .getAsJsonObject("SKIN")
                .get("url")
                .asString

            val hash = url.substringAfterLast("/")

            val data = SkinData(json, url, hash)
            skinCache[value] = data

            data
        } catch (_: Exception) {
            null
        }
    }

    private fun getTextureValue(player: Player): String? {
        val textures = player.gameProfile.properties["textures"]
        if (textures.isEmpty()) return null
        return textures.firstOrNull()?.value
    }

    fun getSkinJson(player: Player): JsonObject? {
        val value = getTextureValue(player)
        return getSkinDataFromValue(value)?.json
    }

    fun getSkinUrl(player: Player): String? {
        val value = getTextureValue(player)
        return getSkinDataFromValue(value)?.url
    }

    fun getSkinHash(player: Player): String? {
        val value = getTextureValue(player)
        return getSkinDataFromValue(value)?.hash
    }

    fun getSkinHash(stack: ItemStack): String? {
        val profile = stack.get(DataComponents.PROFILE) ?: return null
        val textures = profile.properties.get("textures")?.firstOrNull() ?: return null
        val skinData = getSkinDataFromValue(textures.value) ?: return null

        return skinData.hash
    }

}