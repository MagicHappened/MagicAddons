package org.magic.magicaddons.data.server

import com.google.gson.Gson
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object GreenhouseDataSync {

    private val SYNC_COOLDOWN: Duration = Duration.ofMinutes(10)

    private val WAIT_ON_GAME_CLOSE: Duration = Duration.ofSeconds(2)

    private val SAME_UPLOAD_WINDOW: Duration = Duration.ofSeconds(10)

    private val gson = Gson()

    private var lastManualSyncAtMs: Long = 0

    @Volatile
    private var pendingUpload: CompletableFuture<SyncOutcome>? = null

    @Volatile
    private var lastUploadStartedAtMs: Long = 0

    sealed interface SyncOutcome {
        data object Sent : SyncOutcome
        data object NotLinked : SyncOutcome
        data object FeatureOff : SyncOutcome
        data class NoGreenhouseData(val missing: List<ServerGreenhouseData.MissingData>) : SyncOutcome
        data object OnAlpha : SyncOutcome
        data class CoolingDown(val waitMs: Long) : SyncOutcome
        data class Failed(val status: Int?) : SyncOutcome
    }

    fun init() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> uploadActiveProfile("disconnect") }
        ClientLifecycleEvents.CLIENT_STOPPING.register { uploadBeforeGameCloses() }
    }

    fun reportOnlineOnMainNetwork() {
        if (!LocationAPI.onHypixel || GreenhouseProfiles.holdsAlphaData || !ServerSession.isConnected) return

        ServerSession.sendAuthorized("/online") { POST(HttpRequest.BodyPublishers.noBody()) }.thenAccept { response ->
            if (response?.statusCode() != ServerSession.HTTP_OK) Common.LOGGER.warn("could not tell the magic-addons server you are online: {}", response?.statusCode())
        }
    }

    fun syncByCommand(): CompletableFuture<SyncOutcome> {
        val waitMs = lastManualSyncAtMs + SYNC_COOLDOWN.toMillis() - System.currentTimeMillis()
        if (waitMs > 0) return CompletableFuture.completedFuture(SyncOutcome.CoolingDown(waitMs))

        return uploadActiveProfile("command", isStillOnline = true).thenApply { outcome ->
            if (outcome == SyncOutcome.Sent) lastManualSyncAtMs = System.currentTimeMillis()
            outcome
        }
    }

    private fun uploadActiveProfile(reason: String, isStillOnline: Boolean = false): CompletableFuture<SyncOutcome> {
        lastUploadStartedAtMs = System.currentTimeMillis()

        if (GreenhouseProfiles.holdsAlphaData) return skippedUpload(reason, SyncOutcome.OnAlpha)
        if (!GreenhousePresets.discordIntegrationEnabled() || !ServerSession.isConnected) {
            return skippedUpload(reason, SyncOutcome.FeatureOff)
        }
        val data = ServerGreenhouseData.ofActiveProfile()?.copy(isStillOnline = isStillOnline.takeIf { it })
            ?: return skippedUpload(reason, SyncOutcome.NoGreenhouseData(ServerGreenhouseData.missingDataOfActiveProfile()))
        val body = gson.toJson(data)

        val upload = ServerSession.sendAuthorized("/greenhouse") {
            header("Content-Type", "application/json")
            PUT(HttpRequest.BodyPublishers.ofString(body))
        }.thenApply { response ->
            val outcome = syncOutcomeOf(response)
            Common.LOGGER.info("greenhouse data upload on {}: {} ({} bytes)", reason, outcome, body.length)
            outcome
        }
        pendingUpload = upload

        return upload
    }

    private fun skippedUpload(reason: String, outcome: SyncOutcome): CompletableFuture<SyncOutcome> {
        Common.LOGGER.info("greenhouse data upload on {} skipped: {}", reason, outcome)
        return CompletableFuture.completedFuture(outcome)
    }

    private fun syncOutcomeOf(response: HttpResponse<String>?): SyncOutcome = when {
        response == null -> SyncOutcome.Failed(null)
        response.statusCode() == ServerSession.HTTP_OK -> SyncOutcome.Sent
        response.body().contains("notLinked") -> SyncOutcome.NotLinked
        else -> SyncOutcome.Failed(response.statusCode())
    }

    private fun uploadBeforeGameCloses() {
        val hasJustUploaded = System.currentTimeMillis() - lastUploadStartedAtMs < SAME_UPLOAD_WINDOW.toMillis()
        val upload = if (hasJustUploaded) pendingUpload else uploadActiveProfile("game close")

        runCatching { upload?.get(WAIT_ON_GAME_CLOSE.toMillis(), TimeUnit.MILLISECONDS) }
    }
}
