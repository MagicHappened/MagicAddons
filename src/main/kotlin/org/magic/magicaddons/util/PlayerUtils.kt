package org.magic.magicaddons.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.entity.player.PlayerEntity
import org.magic.magicaddons.data.SkyblockIsland
import java.util.Base64

object PlayerUtils {
    var location: SkyblockIsland? = null

    /*

    {
  "timestamp": 1608313373173,
  "profileId": "41d3abc2d749400c9090d5434d03831b",
  "profileName": "Megakloon",
  "signatureRequired": true,
  "textures": {
    "SKIN": {
      "url": "http://textures.minecraft.net/texture/65f31c1c215a57e937fd75ab35782f85ec242a1b1f950a26a42bb5e0aa5cbeda"
    },
    "CAPE": {
      "url": "http://textures.minecraft.net/texture/8f23a1c92f05d7b2b7f6f5e0c9c7e0df5a6b7c8d9f0e1a2b3c4d5e6f7g8h9i0"
    }
  }
}


    */
    fun getSkinJson(player: PlayerEntity): JsonObject? {
        val textures = player.gameProfile.properties["textures"]
        if (textures.isEmpty()) return null

        val value = textures.firstOrNull()?.value ?: return null

        return try {
            val decodedJson = String(Base64.getDecoder().decode(value))
            JsonParser.parseString(decodedJson).asJsonObject
        } catch (_: Exception) {
            null
        }
    }

    fun getSkinUrl(player: PlayerEntity): String? {
        val json = getSkinJson(player) ?: return null
        return try {
            json.getAsJsonObject("textures")
                .getAsJsonObject("SKIN")
                .get("url")
                .asString
        } catch (_: Exception) {
            null
        }
    }

    fun getSkinHash(player: PlayerEntity): String? {
        val url = getSkinUrl(player) ?: return null
        return url.substringAfterLast("/")
    }


}