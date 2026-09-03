package org.magic.magicaddons.util

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.Common
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Whether a newer build of the mod exists. Release builds compare against the published releases,
 * beta builds against the head of the beta branch.
 */
object VersionChecker {

    private const val REPO = "MagicHappened/MagicAddons"
    private const val RELEASES_URL = "https://api.github.com/repos/$REPO/releases"
    private const val BETA_COMMIT_URL = "https://api.github.com/repos/$REPO/commits/beta"

    const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"
    const val BETA_PAGE = "https://github.com/$REPO/actions?query=branch%3Abeta"

    private val client: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    }

    /** What a check found, once it has been made. Null until then. */
    var result: Result? = null
        private set

    private var checking = false

    /** The outcome of a check: what is running, what is newest, and how far apart they are. */
    data class Result(
        val current: String,
        val latest: String,
        val versionsBehind: Int,
        val beta: Boolean
    ) {
        val outdated: Boolean get() = current != latest

        /** "(1.2.1 -> 1.5.3 - 5 version changes)", the count dropped when only one version passed. */
        fun span(): String = when {
            beta || versionsBehind <= 1 -> "($current -> $latest)"
            else -> "($current -> $latest - $versionsBehind version changes)"
        }

        fun headline(): String =
            if (beta) "New beta version available! ${span()}" else "New version available! ${span()}"

        fun page(): String = if (beta) BETA_PAGE else RELEASES_PAGE
    }

    /** The version this jar was built as, straight from its own metadata. */
    fun currentVersion(): String =
        FabricLoader.getInstance()
            .getModContainer(Common.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("unknown")

    /** Whether this build came off the beta branch, which its build metadata says. */
    fun onBeta(): Boolean = currentVersion().contains(".beta.")

    /** The release number without the Minecraft version and build tag: 1.2.1+26.1.2 is 1.2.1. */
    private fun releaseNumber(version: String): String = version.substringBefore('+')

    /** The commit a beta build came from: 1.2.1+26.1.2.beta.c1e57d1 is c1e57d1. */
    private fun betaCommit(version: String): String = version.substringAfter(".beta.", "")

    /**
     * Asks GitHub what the newest build is, once. Runs off the game thread and hands the answer
     * back through [result], which stays null when anything about the request fails.
     */
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

    /** The releases list, newest first: the top entry is the latest, and the rest give the count. */
    private fun fetchRelease(): Result? {
        val body = get(RELEASES_URL) ?: return null
        val tags = JsonParser.parseString(body).asJsonArray
            .mapNotNull { it.asJsonObject.get("tag_name")?.asString?.removePrefix("v") }

        if (tags.isEmpty()) return null

        val current = releaseNumber(currentVersion())
        val latest = tags.first()

        // how many releases sit above the one being run, so a jump of several says so
        val behind = tags.indexOf(current).let { if (it < 0) 1 else it }

        return Result(current, latest, behind, beta = false)
    }

    /** The beta branch head, since beta builds are told apart by the commit they were built from. */
    private fun fetchBeta(): Result? {
        val body = get(BETA_COMMIT_URL) ?: return null
        val head = JsonParser.parseString(body).asJsonObject.get("sha")?.asString ?: return null

        val current = currentVersion()
        val running = betaCommit(current)
        if (running.isEmpty()) return null

        val shortHead = head.take(running.length)

        return Result(
            current = "${releaseNumber(current)}.$running",
            latest = "${releaseNumber(current)}.$shortHead",
            versionsBehind = if (running == shortHead) 0 else 1,
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

    /** The chat line, with the download page behind a click. */
    fun message(found: Result): Component =
        ChatUtils.buildWithPrefix(
            Component.literal(found.headline()).setStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.WHITE)
                    .withClickEvent(ClickEvent.OpenUrl(URI(found.page())))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal(found.page())))
            )
        )
}
