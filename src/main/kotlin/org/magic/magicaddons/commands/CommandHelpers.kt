package org.magic.magicaddons.commands

import com.mojang.brigadier.context.StringRange
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import java.util.concurrent.CompletableFuture

/** Crop names as single command words, since a word cannot hold a space. */
object CropWords {

    /** A crop's name as one word: letters and digits only. */
    fun of(def: CropDefinition): String = def.name.filter { c -> c.isLetterOrDigit() }

    /** The crop whose word is [word], ignoring case, or null. */
    fun find(word: String): CropDefinition? =
        CropRegistry.all.firstOrNull { def -> of(def).equals(word, ignoreCase = true) }

    /**
     * Every crop word starting with what was typed, [leading] words first. Built by hand because
     * the builder sorts alphabetically and would bury the leading words among the crops.
     */
    fun suggest(builder: SuggestionsBuilder, leading: List<String> = emptyList()): CompletableFuture<Suggestions> {
        val typed = builder.remainingLowerCase
        val range = StringRange.between(builder.start, builder.input.length)
        val suggestions = (leading + CropRegistry.all.map { of(it) }.distinct())
            .filter { it.lowercase().startsWith(typed) }
            .map { Suggestion(range, it) }

        return CompletableFuture.completedFuture(Suggestions(range, suggestions))
    }
}

/** A position short enough to read in chat. */
fun fmt(pos: Vec3): String = "%.4f %.4f %.4f".format(pos.x, pos.y, pos.z)

/** Milliseconds as hours, minutes and seconds to the second: "1h 04m 30s". */
fun Long.toExactDuration(): String {
    val seconds = this / 1000

    return "%dh %02dm %02ds".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
}
