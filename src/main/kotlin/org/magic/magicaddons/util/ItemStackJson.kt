package org.magic.magicaddons.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId

object ItemStackJson {

    private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun of(stack: ItemStack): JsonObject? {
        val registries = Minecraft.getInstance().connection?.registryAccess() ?: return null
        val json = JsonObject()
        json.addProperty("skyblockId", stack.getSkyBlockId()?.id)
        json.addProperty("name", stack.hoverName.string)
        json.add("stack", ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), stack).getOrThrow())
        return json
    }

    fun text(json: JsonElement): String = GSON.toJson(json)
}
