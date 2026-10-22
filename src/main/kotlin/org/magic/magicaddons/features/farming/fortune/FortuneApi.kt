package org.magic.magicaddons.features.farming.fortune

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI

object FortuneApi {

    sealed interface Outcome {
        data class Filled(val fields: Int) : Outcome
        data class CoolingDown(val retryAfterSeconds: Int) : Outcome
        data object NotConnected : Outcome
        data object NoProfile : Outcome
        data object HypixelThrottled : Outcome
        data object HypixelUnavailable : Outcome
        data class Failed(val status: Int?) : Outcome
    }

    private const val HTTP_TOO_MANY_REQUESTS: Int = 429
    private const val HTTP_NOT_FOUND: Int = 404

    fun fetch(): CompletableFuture<Outcome> {
        if (!ServerSession.isConnected) return CompletableFuture.completedFuture(Outcome.NotConnected)
        val profileId = runCatching { ProfileAPI.profileId }.getOrNull() ?: GreenhouseProfiles.activeProfileId
            ?: return CompletableFuture.completedFuture(Outcome.NoProfile)

        return ServerSession.sendAuthorized("/fortune?profile=$profileId").thenApply(::outcomeOf)
    }

    private fun outcomeOf(response: HttpResponse<String>?): Outcome {
        if (response == null) return Outcome.Failed(null)
        val body = runCatching { JsonParser.parseString(response.body()).asJsonObject }.getOrNull()
        if (response.statusCode() == ServerSession.HTTP_OK && body != null) return Outcome.Filled(apply(body))

        val error = body?.get("error")?.asString
        return when {
            error == "rateLimited" -> Outcome.CoolingDown(body.get("retryAfterSeconds")?.asInt ?: 0)
            error == "hypixelThrottled" -> Outcome.HypixelThrottled
            error == "hypixelUnavailable" || error == "noHypixelKey" -> Outcome.HypixelUnavailable
            error == "noProfile" || response.statusCode() == HTTP_NOT_FOUND -> Outcome.NoProfile
            response.statusCode() == HTTP_TOO_MANY_REQUESTS -> Outcome.CoolingDown(0)
            else -> Outcome.Failed(response.statusCode())
        }
    }

    private fun apply(body: JsonObject): Int {
        var filled = 0
        fun fill(field: FortuneField, crop: CropDrop?, level: Double) {
            FortuneReadings.set(field, crop, level, FortuneReadings.ORIGIN_API)
            filled++
        }

        body.get("anita")?.takeIf { it.isJsonPrimitive }?.let { fill(FortuneField.Anita, null, it.asDouble) }
        body.get("cocoaUpgrades")?.takeIf { it.isJsonPrimitive }?.let { fill(FortuneField.CocoaUpgrades, null, it.asDouble) }
        body.getAsJsonObject("chips")?.get("cropshot")?.takeIf { it.isJsonPrimitive }?.let { fill(FortuneField.Cropshot, null, it.asDouble) }

        body.getAsJsonObject("pestKills")?.let { kills ->
            val byKey = kills.entrySet().associate { (key, value) -> key to value.asLong }
            fill(FortuneField.PestBestiary, null, FortuneTables.pestBrackets(byKey).toDouble())
        }

        body.getAsJsonObject("cropUpgrades")?.entrySet()?.forEach { (id, level) ->
            FortuneTables.HYPIXEL_CROP_IDS[id]?.forEach { fill(FortuneField.CropUpgrade, it, level.asDouble) }
        }

        val bestsUnlocked = body.get("personalBestsUnlocked")?.asBoolean ?: false
        body.getAsJsonObject("personalBests")?.entrySet()?.forEach { (id, best) ->
            FortuneTables.HYPIXEL_CROP_IDS[id]?.forEach { crop ->
                fill(FortuneField.PersonalBest, crop, if (bestsUnlocked) FortuneTables.personalBestFortune(crop, best.asLong) else 0.0)
            }
        }

        Common.LOGGER.info("Filled {} fortune readings from the Hypixel API", filled)
        return filled
    }

    fun describe(outcome: Outcome): String = when (outcome) {
        is Outcome.Filled -> "Filled ${outcome.fields} readings from the Hypixel API."
        is Outcome.CoolingDown -> "You can fetch again in ${(outcome.retryAfterSeconds + 59) / 60} minutes."
        Outcome.NotConnected -> "Not connected to the MagicAddons server."
        Outcome.NoProfile -> "The Hypixel API has no data for this profile yet."
        Outcome.HypixelThrottled -> "The Hypixel API is throttling the server, try again in a few minutes."
        Outcome.HypixelUnavailable -> "The Hypixel API could not be reached."
        is Outcome.Failed -> "The server did not answer (${outcome.status ?: "unreachable"})."
    }
}
