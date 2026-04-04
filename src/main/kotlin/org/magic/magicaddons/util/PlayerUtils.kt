package org.magic.magicaddons.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import org.magic.magicaddons.data.SkyblockIsland
import java.util.Base64

object PlayerUtils {
    var location: SkyblockIsland? = null

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

    private fun getTextureValue(player: PlayerEntity): String? {
        val textures = player.gameProfile.properties["textures"]
        if (textures.isEmpty()) return null
        return textures.firstOrNull()?.value
    }

    fun getSkinJson(player: PlayerEntity): JsonObject? {
        val value = getTextureValue(player)
        return getSkinDataFromValue(value)?.json
    }

    fun getSkinUrl(player: PlayerEntity): String? {
        val value = getTextureValue(player)
        return getSkinDataFromValue(value)?.url
    }

    fun getSkinHash(player: PlayerEntity): String? {
        val value = getTextureValue(player)
        return getSkinDataFromValue(value)?.hash
    }

    fun getSkinHash(stack: ItemStack): String? {
        val profileComponent = stack.get(DataComponentTypes.PROFILE) ?: return null
        val value = profileComponent.gameProfile?.properties?.get("textures")
            ?.firstOrNull()?.value

        return getSkinDataFromValue(value)?.hash
    }

}