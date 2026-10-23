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

    class Reading(val level: Double, val readAt: Long)

    class BreakdownEntry(val category: String, val name: String, val value: Double)

    class Breakdown(val entries: List<BreakdownEntry>, val readAt: Long, val petName: String, val heldTool: String = "") {
        val total: Double get() = entries.sumOf { it.value }
    }

    class Temporary(val value: Double, val expiresAt: Long) {
        val isActive: Boolean get() = System.currentTimeMillis() < expiresAt
    }


    private const val FILE_NAME: String = "fortune.json"
    private const val READINGS_KEY: String = "readings"
    private const val BREAKDOWNS_KEY: String = "breakdowns"
    private const val TEMPORARY_KEY: String = "temporary"
    private const val SERVED_VISITORS_KEY: String = "served_visitors"
    private val SERVED_VISITORS_CODEC: Codec<List<String>> = Codec.STRING.listOf()

    private val READING_CODEC: Codec<Reading> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("level").forGetter { it.level },
            Codec.LONG.fieldOf("read_at").forGetter { it.readAt }
        ).apply(instance, ::Reading)
    }

    private val ENTRY_CODEC: Codec<BreakdownEntry> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.STRING.fieldOf("category").forGetter { it.category },
            Codec.STRING.fieldOf("name").forGetter { it.name },
            Codec.DOUBLE.fieldOf("value").forGetter { it.value }
        ).apply(instance, ::BreakdownEntry)
    }

    private val BREAKDOWN_CODEC: Codec<Breakdown> = RecordCodecBuilder.create { instance ->
        instance.group(
            ENTRY_CODEC.listOf().fieldOf("entries").forGetter { it.entries },
            Codec.LONG.fieldOf("read_at").forGetter { it.readAt },
            Codec.STRING.fieldOf("pet").forGetter { it.petName },
            Codec.STRING.optionalFieldOf("held_tool", "").forGetter { it.heldTool }
        ).apply(instance, ::Breakdown)
    }

    private val TEMPORARY_CODEC: Codec<Temporary> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("value").forGetter { it.value },
            Codec.LONG.fieldOf("expires_at").forGetter { it.expiresAt }
        ).apply(instance, ::Temporary)
    }

    private val READINGS_CODEC: Codec<Map<String, Reading>> = Codec.unboundedMap(Codec.STRING, READING_CODEC)
    private val BREAKDOWNS_CODEC: Codec<Map<String, Breakdown>> = Codec.unboundedMap(Codec.STRING, BREAKDOWN_CODEC)
    private val TEMPORARIES_CODEC: Codec<Map<String, Temporary>> = Codec.unboundedMap(Codec.STRING, TEMPORARY_CODEC)

    private class FileContents(
        val readings: Map<String, Reading>,
        val breakdowns: Map<String, Breakdown>,
        val temporaries: Map<String, Temporary>,
        val servedVisitors: List<String>
    )

    private var loadedFor: UUID? = null
    private val readings: MutableMap<String, Reading> = mutableMapOf()
    private val breakdowns: MutableMap<String, Breakdown> = mutableMapOf()
    private val temporaries: MutableMap<String, Temporary> = mutableMapOf()
    private val servedVisitors: MutableSet<String> = mutableSetOf()

    private fun profileId(): UUID? = runCatching { ProfileAPI.profileId }.getOrNull() ?: GreenhouseProfiles.activeProfileId

    private fun fileOf(profileId: UUID): Path = GreenhouseProfiles.profileDirOf(profileId).resolve(FILE_NAME)

    private fun keyOf(field: FortuneField, crop: CropDrop?): String =
        if (field.scope == FieldScope.PerCrop) "${field.name}:${requireNotNull(crop).name}" else field.name

    private fun keyOf(stat: FortuneStat): String = when (stat) {
        FortuneStat.General -> "general"
        is FortuneStat.Crop -> stat.crop.name
    }

    private fun ensureLoaded(): UUID? {
        val profileId = profileId() ?: return null
        if (profileId == loadedFor) return profileId

        readings.clear()
        breakdowns.clear()
        temporaries.clear()
        servedVisitors.clear()
        loadedFor = profileId
        when (val result = CodecStorage.load(fileOf(profileId), ::decodeFile)) {
            is ModFiles.LoadResult.Loaded -> {
                readings.putAll(result.value.readings)
                breakdowns.putAll(result.value.breakdowns)
                temporaries.putAll(result.value.temporaries)
                servedVisitors.addAll(result.value.servedVisitors)
            }
            is ModFiles.LoadResult.Unreadable -> Common.LOGGER.error("Fortune readings for $profileId could not be read", result.cause)
            ModFiles.LoadResult.NoFile -> Unit
        }
        return profileId
    }

    private fun decodeFile(root: JsonObject): FileContents = FileContents(
        CodecStorage.decodeEntry(root, READINGS_CODEC, READINGS_KEY).orEmpty(),
        CodecStorage.decodeEntry(root, BREAKDOWNS_CODEC, BREAKDOWNS_KEY).orEmpty(),
        CodecStorage.decodeEntry(root, TEMPORARIES_CODEC, TEMPORARY_KEY).orEmpty(),
        CodecStorage.decodeEntry(root, SERVED_VISITORS_CODEC, SERVED_VISITORS_KEY).orEmpty()
    )

    private fun save(profileId: UUID) {
        if (GreenhouseProfiles.holdsAlphaData) return
        CodecStorage.save(
            fileOf(profileId),
            listOf(
                CodecStorage.RootEntry(READINGS_KEY, READINGS_CODEC, readings.toMap()),
                CodecStorage.RootEntry(BREAKDOWNS_KEY, BREAKDOWNS_CODEC, breakdowns.toMap()),
                CodecStorage.RootEntry(TEMPORARY_KEY, TEMPORARIES_CODEC, temporaries.toMap()),
                CodecStorage.RootEntry(SERVED_VISITORS_KEY, SERVED_VISITORS_CODEC, servedVisitors.sorted())
            ),
            ::decodeFile
        )
    }

    fun get(field: FortuneField, crop: CropDrop? = null): Reading? {
        ensureLoaded() ?: return null
        return readings[keyOf(field, crop)]
    }

    fun set(field: FortuneField, crop: CropDrop?, level: Double): Boolean {
        val profileId = ensureLoaded() ?: return false
        val key = keyOf(field, crop)
        val changed = readings[key]?.level != level
        readings[key] = Reading(level, System.currentTimeMillis())
        save(profileId)
        return changed
    }

    private const val STAT_TOTALS_KEY: String = "stat_totals"

    fun statTotals(): Breakdown? {
        ensureLoaded() ?: return null
        return breakdowns[STAT_TOTALS_KEY]
    }

    fun setStatTotals(totals: Map<String, Double>, heldTool: String): Boolean {
        val profileId = ensureLoaded() ?: return false
        val entries = totals.map { (name, value) -> BreakdownEntry(STAT_TOTALS_KEY, name, value) }
        val previous = breakdowns[STAT_TOTALS_KEY]
        val changed = previous == null || previous.heldTool != heldTool || signatureOf(previous.entries) != signatureOf(entries)
        breakdowns[STAT_TOTALS_KEY] = Breakdown(entries, System.currentTimeMillis(), "", heldTool)
        save(profileId)
        return changed
    }

    private fun signatureOf(entries: List<BreakdownEntry>): Set<Triple<String, String, Double>> =
        entries.mapTo(HashSet()) { Triple(it.category, it.name, it.value) }

    fun breakdown(stat: FortuneStat): Breakdown? {
        ensureLoaded() ?: return null
        return breakdowns[keyOf(stat)]
    }

    fun setBreakdown(stat: FortuneStat, breakdown: Breakdown): Boolean {
        val profileId = ensureLoaded() ?: return false
        val changed = breakdowns[keyOf(stat)]?.let { signatureOf(it.entries) != signatureOf(breakdown.entries) } ?: true
        breakdowns[keyOf(stat)] = breakdown
        save(profileId)
        return changed
    }

    fun temporary(name: String): Temporary? {
        ensureLoaded() ?: return null
        return temporaries[name]?.takeIf { it.isActive }
    }

    fun setTemporary(name: String, value: Double, expiresAt: Long) {
        val profileId = ensureLoaded() ?: return
        temporaries[name] = Temporary(value, expiresAt)
        save(profileId)
    }

    fun clearTemporary(name: String) {
        val profileId = ensureLoaded() ?: return
        if (temporaries.remove(name) != null) save(profileId)
    }

    fun hasServed(visitorId: String): Boolean {
        ensureLoaded() ?: return false
        return visitorId in servedVisitors
    }

    fun noteServed(visitorIds: Collection<String>) {
        val profileId = ensureLoaded() ?: return
        if (servedVisitors.addAll(visitorIds)) save(profileId)
    }

    fun clearAll() {
        val profileId = ensureLoaded() ?: return
        readings.clear()
        servedVisitors.clear()
        breakdowns.clear()
        temporaries.clear()
        save(profileId)
    }

    fun isKnownInactive(name: String): Boolean {
        ensureLoaded() ?: return false
        return temporaries[name]?.isActive == false
    }

    fun activeTemporaries(): Map<String, Temporary> {
        ensureLoaded() ?: return emptyMap()
        return temporaries.filterValues { it.isActive }
    }
}
