package org.magic.magicaddons.features.farming.fortune

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.nio.file.Path
import java.util.UUID
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.data.handlers.CodecStorage
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI

object FortuneReadings {

    class Reading(val level: Double, val readAt: Long, val origin: String)

    const val ORIGIN_MENU: String = "menu"
    const val ORIGIN_API: String = "api"
    const val ORIGIN_MANUAL: String = "manual"

    private const val FILE_NAME: String = "fortune.json"
    private const val READINGS_KEY: String = "readings"

    private val READING_CODEC: Codec<Reading> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("level").forGetter { it.level },
            Codec.LONG.fieldOf("read_at").forGetter { it.readAt },
            Codec.STRING.fieldOf("origin").forGetter { it.origin }
        ).apply(instance, ::Reading)
    }

    private val READINGS_CODEC: Codec<Map<String, Reading>> = Codec.unboundedMap(Codec.STRING, READING_CODEC)

    private var loadedFor: UUID? = null
    private val readings: MutableMap<String, Reading> = mutableMapOf()

    private fun profileId(): UUID? = runCatching { ProfileAPI.profileId }.getOrNull() ?: GreenhouseProfiles.activeProfileId

    private fun fileOf(profileId: UUID): Path = GreenhouseProfiles.profileDirOf(profileId).resolve(FILE_NAME)

    private fun keyOf(field: FortuneField, crop: CropDrop?): String =
        if (field.scope == FieldScope.PerCrop) "${field.name}:${requireNotNull(crop).name}" else field.name

    private fun ensureLoaded(): UUID? {
        val profileId = profileId() ?: return null
        if (profileId == loadedFor) return profileId

        readings.clear()
        loadedFor = profileId
        when (val result = CodecStorage.load(fileOf(profileId), ::decodeFile)) {
            is ModFiles.LoadResult.Loaded -> readings.putAll(result.value)
            is ModFiles.LoadResult.Unreadable -> Common.LOGGER.error("Fortune readings for $profileId could not be read", result.cause)
            ModFiles.LoadResult.NoFile -> Unit
        }
        return profileId
    }

    private fun decodeFile(root: JsonObject): Map<String, Reading> =
        CodecStorage.decodeEntry(root, READINGS_CODEC, READINGS_KEY).orEmpty()

    private fun save(profileId: UUID) {
        if (GreenhouseProfiles.holdsAlphaData) return
        CodecStorage.save(fileOf(profileId), listOf(CodecStorage.RootEntry(READINGS_KEY, READINGS_CODEC, readings.toMap())), ::decodeFile)
    }

    fun get(field: FortuneField, crop: CropDrop? = null): Reading? {
        ensureLoaded() ?: return null
        return readings[keyOf(field, crop)]
    }

    fun set(field: FortuneField, crop: CropDrop?, level: Double, origin: String) {
        val profileId = ensureLoaded() ?: return
        readings[keyOf(field, crop)] = Reading(level, System.currentTimeMillis(), origin)
        save(profileId)
    }

    fun clear(field: FortuneField, crop: CropDrop? = null) {
        val profileId = ensureLoaded() ?: return
        if (readings.remove(keyOf(field, crop)) != null) save(profileId)
    }
}
