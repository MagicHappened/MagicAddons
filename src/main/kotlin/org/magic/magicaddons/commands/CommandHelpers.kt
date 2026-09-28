package org.magic.magicaddons.commands

import com.mojang.brigadier.context.StringRange
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry

object CropWords {

    fun of(def: CropDefinition): String = def.name.filter { c -> c.isLetterOrDigit() }

    fun find(word: String): CropDefinition? =
        CropRegistry.allCrops.firstOrNull { def -> of(def).equals(word, ignoreCase = true) }

    fun suggestCrops(builder: SuggestionsBuilder, leading: List<String> = emptyList()): CompletableFuture<Suggestions> {
        val typed = builder.remainingLowerCase
        val range = StringRange.between(builder.start, builder.input.length)
        val suggestions = (leading + CropRegistry.allCrops.map { of(it) }.distinct())
            .filter { it.lowercase().startsWith(typed) }
            .map { Suggestion(range, it) }

        return CompletableFuture.completedFuture(Suggestions(range, suggestions))
    }
}

fun formatPosition(pos: Vec3): String = "%.4f %.4f %.4f".format(pos.x, pos.y, pos.z)

fun Long.toExactDuration(): String {
    val seconds = this / 1000

    return "%dh %02dm %02ds".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
}
