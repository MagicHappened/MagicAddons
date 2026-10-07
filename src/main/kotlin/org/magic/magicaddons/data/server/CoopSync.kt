package org.magic.magicaddons.data.server

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import net.minecraft.client.Minecraft
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.SBLocation

object CoopSync {

    class CoopState(
        val isCoop: Boolean,
        val members: Int,
        val strictest: GreenhousePresets.GreenhouseCoop,
        val version: Int,
        val online: List<String>,
        val inGarden: List<String>
    )

    class Activity(val harvested: Int, val placed: Int, val watered: Int, val plots: List<String>)

    private class PulledPlot(val id: String, val scannedAt: Long, val lastChangedAt: Long, val grid: String)

    private val HEARTBEAT_INTERVAL: Duration = Duration.ofMinutes(15)
    private val SETTING_SEND_COOLDOWN: Duration = Duration.ofMinutes(5)
    private val ALWAYS_UPLOAD_INTERVAL: Duration = Duration.ofMinutes(12)
    private val LEAVE_GARDEN_UPLOAD_COOLDOWN: Duration = Duration.ofMinutes(10)
    private val PULL_WAIT: Duration = Duration.ofSeconds(5)

    private const val PULL_FAILED_MESSAGE: String =
        "Unable to pull coop data from the server, plots might be inaccurate until scanned. Look in the log for further details"

    private val gson = Gson()

    @Volatile
    var state: CoopState? = null
        private set

    @Volatile
    private var lastContactAtMs: Long = 0

    @Volatile
    private var lastUploadAtMs: Long = 0

    private var lastPulledVersion: Int? = null

    private var isJoinPullPending: Boolean = false

    @Volatile
    private var isPullInFlight: Boolean = false

    @Volatile
    private var scanHeldUntilMs: Long = 0

    private var lastSentCoopSetting: GreenhousePresets.GreenhouseCoop? = null

    private var settingSendDueAtMs: Long? = null

    private var wasInOwnGarden: Boolean = false

    private var harvested: Int = 0
    private var placed: Int = 0
    private var watered: Int = 0
    private val changedPlots: MutableSet<String> = mutableSetOf()

    private val isEligible: Boolean
        get() = ServerSession.isConnected && GreenhousePresets.discordIntegrationEnabled() && !GreenhouseProfiles.holdsAlphaData

    private val profileId: String?
        get() = GreenhouseProfiles.activeProfileId?.toString()

    fun onSkyBlockJoin() {
        isJoinPullPending = true
    }

    fun resetForProfile() {
        state = null
        lastPulledVersion = null
        wasInOwnGarden = false
        isJoinPullPending = true
        isPullInFlight = false
        scanHeldUntilMs = 0
        clearActivity()
    }

    private fun onGardenEntered() {
        if (!isEligible) return
        sendHeartbeat(inGarden = true)
        if (state?.isCoop == true && state?.version != lastPulledVersion) requestPull(holdScan = false)
    }

    private fun onGardenLeft() {
        if (!isEligible) return
        sendHeartbeat(inGarden = false)
        val strictest = state?.takeIf { it.isCoop }?.strictest ?: return
        if (strictest == GreenhousePresets.GreenhouseCoop.None) return
        if (System.currentTimeMillis() - lastUploadAtMs < LEAVE_GARDEN_UPLOAD_COOLDOWN.toMillis()) return
        if (hasChangesSinceLastUpload()) GreenhouseDataSync.uploadForCoop("leave garden")
    }

    fun onTick() {
        if (!isEligible || profileId == null) return
        val now = System.currentTimeMillis()

        val isInOwnGarden = SBLocation.OwnGarden.inside()
        if (isInOwnGarden != wasInOwnGarden) {
            wasInOwnGarden = isInOwnGarden
            if (isInOwnGarden) onGardenEntered() else onGardenLeft()
        }

        if (isJoinPullPending && GreenhouseData.greenhousesInitialized) {
            isJoinPullPending = false
            requestPull(holdScan = true)
        }
        settingSendDueAtMs?.takeIf { now >= it }?.let {
            settingSendDueAtMs = null
            sendHeartbeat(inGarden = SBLocation.OwnGarden.inside())
        }
        if (now - lastContactAtMs >= HEARTBEAT_INTERVAL.toMillis()) sendHeartbeat(inGarden = SBLocation.OwnGarden.inside())

        val isAlways = state?.takeIf { it.isCoop }?.strictest == GreenhousePresets.GreenhouseCoop.Always
        if (isAlways && SBLocation.OwnGarden.inside() && now - lastUploadAtMs >= ALWAYS_UPLOAD_INTERVAL.toMillis() && hasChangesSinceLastUpload()) {
            GreenhouseDataSync.uploadForCoop("coop interval")
        }
    }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        if (GreenhousePresets.greenhouseCoop() == lastSentCoopSetting) return
        val now = System.currentTimeMillis()
        val dueAt = maxOf(now, lastContactAtMs + SETTING_SEND_COOLDOWN.toMillis())
        settingSendDueAtMs = dueAt
    }

    fun isScanHeld(): Boolean {
        if (!isPullInFlight) return false
        if (System.currentTimeMillis() < scanHeldUntilMs) return true

        isPullInFlight = false
        Common.LOGGER.warn("coop pull did not answer within {}s, scanning without it", PULL_WAIT.toSeconds())
        ChatUtils.sendWithPrefix(PULL_FAILED_MESSAGE)
        return false
    }

    fun noteUploadSent(isCoop: Boolean?) {
        val now = System.currentTimeMillis()
        lastUploadAtMs = now
        lastContactAtMs = now
        lastSentCoopSetting = GreenhousePresets.greenhouseCoop()
        settingSendDueAtMs = null
        clearActivity()
        if (isCoop != null && state?.isCoop != isCoop) state = state?.let { CoopState(isCoop, it.members, it.strictest, it.version, it.online, it.inGarden) }
            ?: CoopState(isCoop, 0, GreenhousePresets.GreenhouseCoop.None, 0, emptyList(), emptyList())
    }

    fun activity(): Activity? {
        if (harvested == 0 && placed == 0 && watered == 0 && changedPlots.isEmpty()) return null
        return Activity(harvested, placed, watered, changedPlots.sorted())
    }

    fun noteHarvested(grid: GreenhouseGrid) {
        harvested++
        changedPlots += grid.layout.id
    }

    fun notePlaced(grid: GreenhouseGrid) {
        placed++
        changedPlots += grid.layout.id
    }

    fun noteWatered(grid: GreenhouseGrid) {
        watered++
        changedPlots += grid.layout.id
    }

    fun noteChanged(grid: GreenhouseGrid) {
        changedPlots += grid.layout.id
    }

    private fun clearActivity() {
        harvested = 0
        placed = 0
        watered = 0
        changedPlots.clear()
    }

    private fun hasChangesSinceLastUpload(): Boolean = GreenhouseData.greenhouseGrids.any { grid ->
        (grid.state.lastScanTime?.toEpochMilli() ?: 0L) > lastUploadAtMs || (grid.state.lastChangedAt ?: 0L) > lastUploadAtMs
    }

    private fun sendHeartbeat(inGarden: Boolean) {
        val profileId = profileId ?: return
        lastContactAtMs = System.currentTimeMillis()
        val setting = GreenhousePresets.greenhouseCoop()
        val body = JsonObject().apply {
            addProperty("profileId", profileId)
            addProperty("inGarden", inGarden)
            addProperty("coopSetting", setting.name)
        }
        ServerSession.sendAuthorized("/heartbeat") {
            header("Content-Type", "application/json")
            POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        }.thenAccept { response ->
            if (response?.statusCode() != ServerSession.HTTP_OK) {
                Common.LOGGER.warn("coop heartbeat refused: {}", response?.statusCode())
                return@thenAccept
            }
            lastSentCoopSetting = setting
            parseState(response)?.let { state = it }
        }
    }

    private fun parseState(response: HttpResponse<String>): CoopState? = runCatching {
        val json = JsonParser.parseString(response.body()).asJsonObject
        CoopState(
            isCoop = json.get("isCoop").asBoolean,
            members = json.get("members").asInt,
            strictest = runCatching { GreenhousePresets.GreenhouseCoop.valueOf(json.get("strictest").asString) }
                .getOrDefault(GreenhousePresets.GreenhouseCoop.None),
            version = json.get("version").asInt,
            online = json.getAsJsonArray("online").map { it.asString },
            inGarden = json.getAsJsonArray("inGarden").map { it.asString }
        )
    }.onFailure { Common.LOGGER.warn("could not read the coop state", it) }.getOrNull()

    private fun requestPull(holdScan: Boolean) {
        val profileId = profileId ?: return
        if (isPullInFlight) return
        val plots = JsonObject()
        GreenhouseData.greenhouseGrids.forEach { grid -> plots.addProperty(grid.layout.id, grid.state.lastScanTime?.toEpochMilli() ?: 0L) }
        val body = JsonObject().apply {
            addProperty("profileId", profileId)
            add("plots", plots)
        }
        if (holdScan) {
            isPullInFlight = true
            scanHeldUntilMs = System.currentTimeMillis() + PULL_WAIT.toMillis()
        }
        lastContactAtMs = System.currentTimeMillis()

        ServerSession.sendAuthorized("/greenhouse/pull") {
            header("Content-Type", "application/json")
            POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        }.thenAccept { response ->
            Minecraft.getInstance().execute { receivePull(response, holdScan) }
        }
    }

    private fun receivePull(response: HttpResponse<String>?, wasHoldingScan: Boolean) {
        val wasStillHeld = isPullInFlight
        isPullInFlight = false
        if (response?.statusCode() != ServerSession.HTTP_OK) {
            Common.LOGGER.warn("coop pull failed: {}", response?.statusCode() ?: "unreachable")
            if (wasHoldingScan && wasStillHeld) ChatUtils.sendWithPrefix(PULL_FAILED_MESSAGE)
            return
        }
        val json = runCatching { JsonParser.parseString(response.body()).asJsonObject }.getOrNull() ?: return
        lastPulledVersion = json.get("version")?.asInt
        val pulled = json.getAsJsonArray("plots").map { gson.fromJson(it, PulledPlot::class.java) }
        if (pulled.isEmpty()) return

        val adopted = pulled.mapNotNull { plot ->
            runCatching { GridBlob.decode(plot.grid) }
                .onFailure { Common.LOGGER.warn("could not read the pulled plot {}", plot.id, it) }
                .getOrNull()
                ?.let { grid -> GreenhouseData.adoptPulledGrid(grid, plot.lastChangedAt) }
        }
        Common.LOGGER.info("coop pull adopted {} of {} plots", adopted.size, pulled.size)
    }
}
