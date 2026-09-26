package org.magic.magicaddons.config

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.greenhouse.transfer.RawDeflate
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.features.customization.Customization

object ConfigShare {

    private const val SEPARATOR: Char = ':'

    enum class ConfigType(val prefix: String, val label: String) {
        Ui("MAUI1", "UI config"),
        Features("MACFG1", "feature config")
    }

    sealed interface Pasted {
        class Applied(val author: String, val settings: Int) : Pasted
        class Failed(val reason: String) : Pasted
    }

    private val gson = Gson()

    private val sectionType = object : TypeToken<
            MutableMap<String,
                    MutableMap<String,
                            MutableMap<String, Any>>>>() {}.type

    fun exportConfig(configType: ConfigType): String? {
        FeatureManager.syncToConfigJson()

        val section = exportConfigType(configType)
        if (section.isEmpty()) return null

        val code = listOf(
            configType.prefix,
            playerName(),
            RawDeflate.encode(gson.toJson(section).toByteArray())
        ).joinToString(SEPARATOR.toString())

        Minecraft.getInstance().keyboardHandler.clipboard = code
        return code
    }

    fun importConfig(configType: ConfigType): Pasted {
        val text = Minecraft.getInstance().keyboardHandler.clipboard.trim()
        if (text.isEmpty()) return Pasted.Failed("The clipboard is empty")

        val other = ConfigType.entries.firstOrNull { it != configType && text.startsWith("${it.prefix}$SEPARATOR") }
        if (other != null) return Pasted.Failed("That code is a ${other.label}")

        if (!text.startsWith("${configType.prefix}$SEPARATOR")) return Pasted.Failed("That is not a ${configType.label} code")

        val parts = text.split(SEPARATOR)
        if (parts.size < 3) return Pasted.Failed("That code is incomplete")

        val bytes = RawDeflate.decode(parts.last()) ?: return Pasted.Failed("That code is damaged")

        val section: MutableMap<String, MutableMap<String, MutableMap<String, Any>>> =
            runCatching { gson.fromJson<MutableMap<String, MutableMap<String, MutableMap<String, Any>>>>(String(bytes), sectionType) }
                .getOrNull() ?: return Pasted.Failed("That code is damaged")

        if (section.keys.any { (it == Customization.CATEGORY) != (configType == ConfigType.Ui) }) {
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
        if (configType == ConfigType.Ui) Customization.applyImportedAppearance()
        MagicAddonsConfigJsonHandler.save()

        return Pasted.Applied(parts[1], written)
    }

    private fun exportConfigType(configType: ConfigType): Map<String, MutableMap<String, MutableMap<String, Any>>> =
        MagicAddonsConfigJsonHandler.configMap.filter {
            (it.key == Customization.CATEGORY) == (configType == ConfigType.Ui)
        }

    private fun playerName(): String =
        Minecraft.getInstance().user.name.replace(SEPARATOR, ' ').takeIf { it.isNotBlank() } ?: "Unknown"
}
