package org.magic.magicaddons.data.server

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.ProfileKeyPair
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.account.ServerConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.Signature
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object ServerSession {

    const val HTTP_OK: Int = 200

    private const val HTTP_TOO_MANY_REQUESTS: Int = 429
    private const val HTTP_SERVER_ERROR: Int = 500

    private const val DEFAULT_RETRY_AFTER_SECONDS: Long = 600
    private const val MIN_RETRY_JITTER_MS: Long = 1_000
    private const val MAX_RETRY_JITTER_MS: Long = 5_000
    private const val RELOGIN_MARGIN_MS: Long = 1_000

    private val UNREACHABLE_RETRY_DELAYS_MS: List<Long> = listOf(30_000, 120_000, 600_000, 1_800_000)

    private const val SERVER_URL: String = "https://magicaddons-server.magicaddons.workers.dev"

    private const val CHALLENGE_SIGNATURE_ALGORITHM: String = "SHA256withRSA"

    private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(10)

    private val client: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build()
    }

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenExpiresAtMs: Long = 0

    @Volatile
    private var isLoggingIn: Boolean = false

    @Volatile
    private var rateLimitedUntilMs: Long = 0

    @Volatile
    private var unreachableAttempts: Int = 0

    @Volatile
    private var scheduledRetryNumber: Int = 0

    private val hasValidToken: Boolean
        get() = token != null && System.currentTimeMillis() < tokenExpiresAtMs

    val isConnected: Boolean
        get() = ServerConnection.baseSetting.value && hasValidToken

    val status: ConnectionStatus
        get() = when {
            !ServerConnection.baseSetting.value -> ConnectionStatus.SwitchedOff
            hasValidToken -> ConnectionStatus.Authenticated
            isLoggingIn -> ConnectionStatus.Authenticating
            System.currentTimeMillis() < rateLimitedUntilMs -> ConnectionStatus.RateLimited(rateLimitedUntilMs)
            else -> ConnectionStatus.Failed
        }

    sealed interface ConnectionStatus {
        data object SwitchedOff : ConnectionStatus
        data object Authenticating : ConnectionStatus
        data object Authenticated : ConnectionStatus
        data class RateLimited(val retryAtMs: Long) : ConnectionStatus
        data object Failed : ConnectionStatus
    }

    sealed interface LoginOutcome {
        data object LoggedIn : LoginOutcome
        data object NoKeyPair : LoginOutcome
        data object KeyPairExpired : LoginOutcome
        data class RateLimited(val retryAfterSeconds: Long) : LoginOutcome
        data class Refused(val status: Int, val error: String) : LoginOutcome
        data class Unreachable(val reason: String) : LoginOutcome
    }

    fun init() {
        ClientLifecycleEvents.CLIENT_STARTED.register { connectIfNeeded() }
    }

    fun connectIfNeeded() {
        if (!ServerConnection.baseSetting.value || hasValidToken || isLoggingIn) return
        if (System.currentTimeMillis() < rateLimitedUntilMs) return

        isLoggingIn = true
        scheduledRetryNumber++
        logIn().thenAccept { outcome ->
            scheduleRetryAfter(outcome)
            isLoggingIn = false
            when (outcome) {
                LoginOutcome.LoggedIn -> Common.LOGGER.info("successfully authenticated to magic-addons server")
                else -> Common.LOGGER.warn("could not authenticate to magic-addons server: {}", outcome)
            }
        }
    }

    private fun scheduleRetryAfter(outcome: LoginOutcome) {
        when {
            outcome is LoginOutcome.LoggedIn -> {
                unreachableAttempts = 0
                connectIfNeededIn(tokenExpiresAtMs - System.currentTimeMillis() + RELOGIN_MARGIN_MS)
            }
            outcome is LoginOutcome.RateLimited -> {
                val delayMs = outcome.retryAfterSeconds * 1000 + Random.nextLong(MIN_RETRY_JITTER_MS, MAX_RETRY_JITTER_MS)
                rateLimitedUntilMs = System.currentTimeMillis() + delayMs
                connectIfNeededIn(delayMs)
            }
            shouldRetry(outcome) -> {
                val delayMs = UNREACHABLE_RETRY_DELAYS_MS[unreachableAttempts.coerceAtMost(UNREACHABLE_RETRY_DELAYS_MS.lastIndex)]
                unreachableAttempts++
                connectIfNeededIn(delayMs)
            }
        }
    }

    private fun shouldRetry(outcome: LoginOutcome): Boolean =
        outcome is LoginOutcome.Unreachable || outcome is LoginOutcome.NoKeyPair ||
                (outcome is LoginOutcome.Refused && outcome.status >= HTTP_SERVER_ERROR)

    private fun connectIfNeededIn(delayMs: Long) {
        val retryNumber = scheduledRetryNumber

        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute {
            if (retryNumber == scheduledRetryNumber) connectIfNeeded()
        }
    }

    fun logIn(): CompletableFuture<LoginOutcome> =
        Minecraft.getInstance().profileKeyPairManager.prepareKeyPair()
            .thenCompose { keyPair ->
                val pair = keyPair.orElse(null)

                when {
                    pair == null -> CompletableFuture.completedFuture(LoginOutcome.NoKeyPair)
                    pair.publicKey.data.hasExpired() -> CompletableFuture.completedFuture(LoginOutcome.KeyPairExpired)
                    else -> logInWithKeyPair(pair)
                }
            }
            .exceptionally { failure -> LoginOutcome.Unreachable(describeFailure(failure)) }

    fun sendAuthorized(path: String, configure: HttpRequest.Builder.() -> Unit = {}): CompletableFuture<HttpResponse<String>?> {
        val heldToken = token?.takeIf { isConnected } ?: return CompletableFuture.completedFuture(null)
        val request = HttpRequest.newBuilder(URI("$SERVER_URL$path"))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Bearer $heldToken")
            .apply(configure)
            .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply<HttpResponse<String>?> { it }
            .exceptionally { null }
    }

    private fun loginBodyOf(pair: ProfileKeyPair, challenge: ByteArray): JsonObject {
        val encoder = Base64.getEncoder()
        val data = pair.publicKey.data

        return JsonObject().apply {
            addProperty("uuid", Minecraft.getInstance().user.profileId.toString())
            addProperty("publicKey", encoder.encodeToString(data.key.encoded))
            addProperty("keySignature", encoder.encodeToString(data.keySignature))
            addProperty("expiresAt", data.expiresAt.toEpochMilli())
            addProperty("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge))
            addProperty("challengeSignature", encoder.encodeToString(signChallenge(pair, challenge)))
        }
    }

    private fun logInWithKeyPair(pair: ProfileKeyPair): CompletableFuture<LoginOutcome> {
        val challengeRequest = HttpRequest.newBuilder(URI("$SERVER_URL/auth/challenge")).timeout(REQUEST_TIMEOUT).GET().build()

        return client.sendAsync(challengeRequest, HttpResponse.BodyHandlers.ofString())
            .thenCompose { challengeResponse ->
                if (challengeResponse.statusCode() == HTTP_TOO_MANY_REQUESTS) {
                    val retryAfterSeconds = challengeResponse.headers().firstValue("Retry-After").orElse(null)?.toLongOrNull()
                    return@thenCompose CompletableFuture.completedFuture<LoginOutcome>(
                        LoginOutcome.RateLimited(retryAfterSeconds ?: DEFAULT_RETRY_AFTER_SECONDS)
                    )
                }
                if (challengeResponse.statusCode() != HTTP_OK) {
                    return@thenCompose CompletableFuture.completedFuture<LoginOutcome>(refusalOf(challengeResponse))
                }

                val challengeText = JsonParser.parseString(challengeResponse.body()).asJsonObject.get("challenge").asString
                val challenge = Base64.getUrlDecoder().decode(challengeText)
                val loginRequest = HttpRequest.newBuilder(URI("$SERVER_URL/auth/login"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginBodyOf(pair, challenge).toString()))
                    .build()

                client.sendAsync(loginRequest, HttpResponse.BodyHandlers.ofString()).thenApply { loginResponse ->
                    if (loginResponse.statusCode() != HTTP_OK) return@thenApply refusalOf(loginResponse)

                    val issuedToken = JsonParser.parseString(loginResponse.body()).asJsonObject.get("token").asString
                    tokenExpiresAtMs = expiryOf(issuedToken)
                    token = issuedToken
                    LoginOutcome.LoggedIn
                }
            }
    }

    private fun expiryOf(issuedToken: String): Long {
        val tokenBody = String(Base64.getUrlDecoder().decode(issuedToken.substringBefore('.')), Charsets.UTF_8)
        return JsonParser.parseString(tokenBody).asJsonObject.get("expiresAt").asLong
    }

    private fun refusalOf(response: HttpResponse<String>): LoginOutcome.Refused {
        val error = runCatching { JsonParser.parseString(response.body()).asJsonObject.get("error").asString }
            .getOrDefault(response.body())

        return LoginOutcome.Refused(response.statusCode(), error)
    }

    private fun describeFailure(failure: Throwable): String {
        val cause = failure.cause ?: failure
        return cause.message ?: cause.javaClass.simpleName
    }

    private fun signChallenge(pair: ProfileKeyPair, challenge: ByteArray): ByteArray =
        Signature.getInstance(CHALLENGE_SIGNATURE_ALGORITHM).apply {
            initSign(pair.privateKey)
            update(challenge)
        }.sign()
}
