package org.magic.magicaddons.config

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.greenhouse.transfer.RawDeflate
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.features.customization.Customization

/**
 * The config as one line of text, so a look or a set of features can be handed to someone else. The
 * two halves travel separately: hardly anyone wants a friend's keybinds along with their colours.
 */
object ConfigShare {

    private const val SEPARATOR: Char = ':'

    /** Which half of the config a code carries, named in the code so the wrong one is turned away. */
    enum class Kind(val prefix: String, val label: String) {
        Ui("MAUI1", "UI config"),
        Features("MACFG1", "feature config")
    }

    sealed interface Pasted {
        /** [author] is whoever copied it, which the code carries so a share can be recognised. */
        class Applied(val author: String, val settings: Int) : Pasted
        class Failed(val reason: String) : Pasted
    }

    private val gson = Gson()

    private val sectionType = object : TypeToken<
            MutableMap<String,
                    MutableMap<String,
                            MutableMap<String, Any>>>>() {}.type

    /** Writes this half of the config to the clipboard, or null when there is nothing stored yet. */
    fun copy(kind: Kind): String? {
        FeatureManager.syncToConfigJson()

        val section = sectionOf(kind)
        if (section.isEmpty()) return null

        val code = listOf(
            kind.prefix,
            playerName(),
            RawDeflate.encode(gson.toJson(section).toByteArray())
        ).joinToString(SEPARATOR.toString())

        Minecraft.getInstance().keyboardHandler.clipboard = code
        return code
    }

    /**
     * Reads this half of the config off the clipboard. Only the settings the code names are written,
     * so a code from an older version leaves everything it never knew about alone.
     */
    fun paste(kind: Kind): Pasted {
        val text = Minecraft.getInstance().keyboardHandler.clipboard?.trim().orEmpty()
        if (text.isEmpty()) return Pasted.Failed("The clipboard is empty")

        val other = Kind.entries.firstOrNull { it != kind && text.startsWith("${it.prefix}$SEPARATOR") }
        if (other != null) return Pasted.Failed("That code is a ${other.label}")

        if (!text.startsWith("${kind.prefix}$SEPARATOR")) return Pasted.Failed("That is not a ${kind.label} code")

        val parts = text.split(SEPARATOR)
        if (parts.size < 3) return Pasted.Failed("That code is incomplete")

        val bytes = RawDeflate.decode(parts.last()) ?: return Pasted.Failed("That code is damaged")

        val section: MutableMap<String, MutableMap<String, MutableMap<String, Any>>> =
            runCatching { gson.fromJson<MutableMap<String, MutableMap<String, MutableMap<String, Any>>>>(String(bytes), sectionType) }
                .getOrNull() ?: return Pasted.Failed("That code is damaged")

        if (section.keys.any { (it == Customization.CATEGORY) != (kind == Kind.Ui) }) {
            return Pasted.Failed("That code holds the other half of the config")
        }

        var written = 0
        section.forEach { (category, features) ->
            val stored = MagicAddonsConfigJsonHandler.configMap.getOrPut(category) { mutableMapOf() }
            features.forEach { (feature, settings) ->
                stored.getOrPut(feature) { mutableMapOf() }.putAll(settings)
                written += settings.size
            }
        }

        FeatureManager.syncFromConfigJson()
        if (kind == Kind.Ui) Customization.reapplyAppearance()
        MagicAddonsConfigJsonHandler.save()

        return Pasted.Applied(parts[1], written)
    }

    /** The look, or everything else; the two never overlap. */
    private fun sectionOf(kind: Kind): Map<String, MutableMap<String, MutableMap<String, Any>>> =
        MagicAddonsConfigJsonHandler.configMap.filter {
            (it.key == Customization.CATEGORY) == (kind == Kind.Ui)
        }

    /** Written into the code so a shared one says whose it is. */
    private fun playerName(): String =
        Minecraft.getInstance().user?.name?.replace(SEPARATOR, ' ')?.takeIf { it.isNotBlank() } ?: "Unknown"
}
