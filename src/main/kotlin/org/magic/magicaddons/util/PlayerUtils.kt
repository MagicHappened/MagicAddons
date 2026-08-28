package org.magic.magicaddons.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.authlib.properties.Property
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.platform.GameProfile
import tech.thatgravyboat.skyblockapi.platform.PropertyMap
import tech.thatgravyboat.skyblockapi.platform.properties
import tech.thatgravyboat.skyblockapi.platform.toResolvableProfile
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
        val textures = profile.properties.get("textures").firstOrNull() ?: return null
        val skinData = getSkinDataFromValue(textures.value) ?: return null

        return skinData.hash
    }

    /**
     * The skull an entity carries, in whichever slot it happens to carry it.
     *
     * Reading only the head slot was wrong. Hypixel hangs some crops' skulls off the main hand
     * instead, which renders the same head smaller for free, and a stand built that way looked
     * to us like a stand carrying nothing at all.
     *
     * Every slot is tried and the first skull found wins. A stand holding something that is not a
     * skull, or nothing at all, has no hash, which is a failure to identify it rather than a
     * reason to keep looking elsewhere.
     */
    fun getSkullHash(entity: LivingEntity): String? =
        EquipmentSlot.entries.firstNotNullOfOrNull { getSkinHash(entity.getItemBySlot(it)) }

    fun getItemFromHash(hash: String): ItemStack {
        val stack = ItemStack(Items.PLAYER_HEAD)

        val texturesJson = """
        {
          "textures": {
            "SKIN": {
              "url": "http://textures.minecraft.net/texture/$hash"
            }
          }
        }
    """.trimIndent()

        val encoded = Base64.getEncoder()
            .encodeToString(texturesJson.toByteArray(Charsets.UTF_8))

        val profile = GameProfile(
            uuid = UUID.randomUUID(),
            name = "",
            map = PropertyMap {
                put("textures", Property("textures", encoded))
            }
        )

        stack.set(
            DataComponents.PROFILE,
            profile.toResolvableProfile()
        )

        return stack
    }

}