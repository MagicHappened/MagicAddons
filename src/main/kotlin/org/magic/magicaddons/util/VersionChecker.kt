package org.magic.magicaddons.util

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

object VersionChecker {

    private const val REPO = "MagicHappened/MagicAddons"
    private const val RELEASES_URL = "https://api.github.com/repos/$REPO/releases"
    private const val BETA_COMPARE_URL = "https://api.github.com/repos/$REPO/compare/%s...beta"

    const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"
    const val BETA_PAGE = "https://github.com/$REPO/actions?query=branch%3Abeta"

    private val client: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    }

    var result: Result? = null
        private set

    private var checking = false

    data class Result(
        val current: String,
        val latest: String,
        val versionsBehind: Int,
        val beta: Boolean
    ) {
        val outdated: Boolean get() = current != latest

        fun span(): String = when {
            beta && versionsBehind > 1 -> "($current -> $latest - $versionsBehind commits behind)"
            beta || versionsBehind <= 1 -> "($current -> $latest)"
            else -> "($current -> $latest - $versionsBehind version changes)"
        }

        fun headline(): String =
            if (beta) "New beta version available! ${span()}" else "New version available! ${span()}"

        fun page(): String = if (beta) BETA_PAGE else RELEASES_PAGE
    }

    fun currentVersion(): String =
        FabricLoader.getInstance()
            .getModContainer(Common.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("unknown")

    fun onBeta(): Boolean = currentVersion().contains(".beta.")

    private fun releaseNumber(version: String): String = version.substringBefore('+')

    private fun betaCommit(version: String): String = version.substringAfter(".beta.", "").removeSuffix("-dirty")

    fun check(onDone: (Result) -> Unit = {}) {
        if (checking) return
        result?.let {
            onDone(it)
            return
        }

        checking = true

        CompletableFuture.supplyAsync { fetch() }
            .thenAccept { found ->
                checking = false
                found ?: return@thenAccept

                result = found
                onDone(found)
            }
            .exceptionally {
                checking = false
                Common.LOGGER.warn("Version check failed", it)
                null
            }
    }

    private fun fetch(): Result? = if (onBeta()) fetchBeta() else fetchRelease()

    private fun fetchRelease(): Result? {
        val body = get(RELEASES_URL) ?: return null
        val tags = JsonParser.parseString(body).asJsonArray
            .mapNotNull { it.asJsonObject.get("tag_name")?.asString?.removePrefix("v") }

        if (tags.isEmpty()) return null

        val current = releaseNumber(currentVersion())
        val latest = tags.first()

        val behind = tags.indexOf(current).let { if (it < 0) 1 else it }

        return Result(current, latest, behind, beta = false)
    }

    private fun fetchBeta(): Result? {
        val current = currentVersion()
        val running = betaCommit(current)
        if (running.isEmpty()) return null

        val body = get(BETA_COMPARE_URL.format(running)) ?: return null
        val compared = JsonParser.parseString(body).asJsonObject
        val commits = compared.getAsJsonArray("commits")
        val behind = compared.get("status")?.asString == "ahead" && commits != null && commits.size() > 0

        val latest = if (behind) commits.last().asJsonObject.get("sha").asString.take(running.length) else running

        return Result(
            current = "${releaseNumber(current)}.$running",
            latest = "${releaseNumber(current)}.$latest",
            versionsBehind = if (behind) compared.get("ahead_by")?.asInt ?: 1 else 0,
            beta = true
        )
    }

    private fun get(url: String): String? {
        val request = HttpRequest.newBuilder(URI(url))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", Common.MOD_NAME)
            .timeout(Duration.ofSeconds(10))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body().takeIf { response.statusCode() == 200 }
    }

    fun updateMessage(found: Result): Component =
        ChatUtils.buildWithPrefix(
            ChatUtils.buildStyled(
                found.headline(),
                ChatFormatting.WHITE,
                Component.literal(found.page()),
                ClickEvent.OpenUrl(URI(found.page())),
            )
        )
}
