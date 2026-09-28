package org.magic.magicaddons.util

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.WorldTickEvent
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

object VersionChecker {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    private const val GITHUB_REPO = "MagicHappened/MagicAddons"
    private const val RELEASES_URL = "https://api.github.com/repos/$GITHUB_REPO/releases"
    private const val BETA_COMPARE_URL = "https://api.github.com/repos/$GITHUB_REPO/compare/%s...beta"

    const val RELEASES_PAGE = "https://github.com/$GITHUB_REPO/releases/latest"
    const val BETA_PAGE = "https://github.com/$GITHUB_REPO/actions?query=branch%3Abeta"

    private val ANNOUNCE_DELAY: Duration = Duration.ofSeconds(10)

    private val client: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    }

    var lastCheck: UpdateCheck? = null
        private set

    private var isChecking = false

    private val pendingCallbacks = mutableListOf<(UpdateCheck?) -> Unit>()

    private var announceAt: Instant? = null
    private var isAnnounced = false

    data class UpdateCheck(
        val current: String,
        val latest: String,
        val versionsBehind: Int,
        val isBeta: Boolean
    ) {
        val isOutdated: Boolean get() = current != latest

        fun versionGapText(): String = when {
            isBeta && versionsBehind > 1 -> "($current -> $latest - $versionsBehind commits behind)"
            isBeta || versionsBehind <= 1 -> "($current -> $latest)"
            else -> "($current -> $latest - $versionsBehind version changes)"
        }

        fun updateHeadline(): String =
            if (isBeta) "New beta version available! ${versionGapText()}" else "New version available! ${versionGapText()}"

        fun downloadPage(): String = if (isBeta) BETA_PAGE else RELEASES_PAGE
    }

    fun currentVersion(): String =
        FabricLoader.getInstance()
            .getModContainer(Common.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("unknown")

    fun isOnBeta(): Boolean = currentVersion().contains(".beta.")

    private fun releaseNumberOf(version: String): String = version.substringBefore('+')

    private fun betaCommitOf(version: String): String = version.substringAfter(".beta.", "").removeSuffix("-dirty")

    fun check(forceRefresh: Boolean = false, onDone: (UpdateCheck?) -> Unit = {}) {
        val cached = lastCheck
        if (cached != null && !forceRefresh) {
            onDone(cached)
            return
        }

        pendingCallbacks += onDone
        if (isChecking) return
        isChecking = true

        CompletableFuture.supplyAsync { fetchLatest() }
            .exceptionally {
                Common.LOGGER.warn("Version check failed", it)
                null
            }
            .thenAccept { update -> Minecraft.getInstance().execute { finishCheck(update) } }
    }

    private fun finishCheck(update: UpdateCheck?) {
        isChecking = false
        if (update != null) lastCheck = update

        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it(update) }
    }

    private fun fetchLatest(): UpdateCheck? = if (isOnBeta()) fetchBeta() else fetchRelease()

    private fun fetchRelease(): UpdateCheck? {
        val body = fetchText(RELEASES_URL) ?: return null
        val tags = JsonParser.parseString(body).asJsonArray
            .mapNotNull { it.asJsonObject.get("tag_name")?.asString?.removePrefix("v") }

        if (tags.isEmpty()) return null

        val current = releaseNumberOf(currentVersion())
        val latest = tags.first()

        val releasesBehind = tags.indexOf(current).let { if (it < 0) 1 else it }

        return UpdateCheck(current, latest, releasesBehind, isBeta = false)
    }

    private fun fetchBeta(): UpdateCheck? {
        val current = currentVersion()
        val runningCommit = betaCommitOf(current)
        if (runningCommit.isEmpty()) return null

        val body = fetchText(BETA_COMPARE_URL.format(runningCommit)) ?: return null
        val comparison = JsonParser.parseString(body).asJsonObject
        val commits = comparison.getAsJsonArray("commits")
        val isBehind = comparison.get("status")?.asString == "ahead" && commits != null && commits.size() > 0

        val latest = if (isBehind) commits.last().asJsonObject.get("sha").asString.take(runningCommit.length) else runningCommit

        return UpdateCheck(
            current = "${releaseNumberOf(current)}.$runningCommit",
            latest = "${releaseNumberOf(current)}.$latest",
            versionsBehind = if (isBehind) comparison.get("ahead_by")?.asInt ?: 1 else 0,
            isBeta = true
        )
    }

    private fun fetchText(url: String): String? {
        val request = HttpRequest.newBuilder(URI(url))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", Common.MOD_NAME)
            .timeout(Duration.ofSeconds(10))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body().takeIf { response.statusCode() == 200 }
    }

    fun updateChatMessage(update: UpdateCheck): Component =
        ChatUtils.buildWithPrefix(
            ChatUtils.buildStyled(
                update.updateHeadline(),
                ChatFormatting.WHITE,
                Component.literal(update.downloadPage()),
                ClickEvent.OpenUrl(URI(update.downloadPage())),
            )
        )

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (isAnnounced) return

        announceAt = Instant.now().plus(ANNOUNCE_DELAY)
        check()
    }

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        val due = announceAt ?: return
        if (Instant.now().isBefore(due)) return

        val player = Minecraft.getInstance().player ?: return
        val update = lastCheck ?: return

        announceAt = null
        isAnnounced = true

        if (!update.isOutdated) return

        player.sendSystemMessage(updateChatMessage(update))
    }
}
