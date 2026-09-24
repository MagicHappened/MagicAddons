package org.magic.magicaddons.data.config

import org.magic.magicaddons.ExtensionPack
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import kotlin.collections.get

sealed class SettingNode<T>(
    val key: String,
    val displayName: String,
    val description: String,
    open var value: T,
    val detail: (() -> SettingDetail?)? = null,
    val needsExtensionPack: Boolean = false
) {
    open val children: List<SettingNode<*>>? = null

    val isAvailable: Boolean get() = !needsExtensionPack || ExtensionPack.isInstalled

    val availableChildren: List<SettingNode<*>> get() = children.orEmpty().filter { it.isAvailable }

    fun settingKey(parentPath: String): String = if (parentPath.isEmpty()) key else "$parentPath.$key"

    open fun serializeSettings(parentPath: String = ""): MutableMap<String, Any>{
        val result = mutableMapOf<String, Any>()
        result[settingKey(parentPath)] = value as Any
        return result
    }
    open fun updateSettings(settings: Map<String, Any>, parentPath: String = "") {
        updateOwnValue(settings, parentPath)
    }

    protected fun updateOwnValue(settings: Map<String, Any>, parentPath: String) {
        val newValue = settings[settingKey(parentPath)] ?: return
        try {
            value = parseValue(newValue)
        } catch (_: Exception) {

        }
    }
    protected abstract fun parseValue(value: Any): T

    inline fun <reified R : SettingNode<*>> getChild(key: String): R? {
        return children?.filterIsInstance<R>()?.firstOrNull { it.key == key }
    }

    inline fun <reified R : SettingNode<*>> getChildOrThrow(key: String): R {
        return getChild<R>(key) ?: throw IllegalStateException("No child with key '$key' of type ${R::class.java.name}")
    }

}

class ToggleListSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: MutableList<ListEntry>,
    val choices: () -> List<String>,
    val searchLabel: String = "Search",
    val searchable: Boolean = true,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<MutableList<ListEntry>>(key, displayName, description, value, detail, needsExtensionPack) {

    override fun parseValue(value: Any): MutableList<ListEntry> {
        val list = value as? List<*> ?: return mutableListOf()

        return list.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null

            val name = map["name"]?.toString() ?: ""
            val strValue = map["value"]?.toString() ?: return@mapNotNull null

            val enabled = when (val e = map["enabled"]) {
                is Boolean -> e
                is String -> e.toBoolean()
                is Number -> e.toInt() != 0
                else -> true
            }

            ListEntry(
                name = name,
                value = strValue,
                enabled = enabled
            )
        }.toMutableList()
    }

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        return mutableMapOf(
            settingKey(parentPath) to value.map { entry ->
                mapOf(
                    "name" to entry.name,
                    "value" to entry.value,
                    "enabled" to entry.enabled
                )
            }
        )
    }
}

class BooleanSetting(
    key: String = "enabled",
    displayName: String,
    description: String,
    value: Boolean,
    override var children: List<SettingNode<*>>? = null,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<Boolean>(key, displayName, description, value, detail, needsExtensionPack) {

    private var storedValue: Boolean = value

    override var value: Boolean
        get() = storedValue && isAvailable
        set(newValue) {
            storedValue = newValue
        }

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>(settingKey(parentPath) to storedValue)
        val childPath = settingKey(parentPath)
        children?.forEach { child ->
            map.putAll(child.serializeSettings(childPath))
        }
        return map
    }
    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        super.updateSettings(settings, parentPath)
        val childPath = settingKey(parentPath)
        children?.forEach { child ->
            child.updateSettings(settings, childPath)
        }
    }
    override fun parseValue(value: Any): Boolean = value as Boolean

    fun serializeAsFeatureRoot(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>(key to storedValue)
        children?.forEach { child ->
            map.putAll(child.serializeSettings())
        }
        return map
    }

    fun updateAsFeatureRoot(settings: Map<String, Any>) {
        updateOwnValue(settings, "")
        children?.forEach { child ->
            child.updateSettings(settings)
        }
    }
}

class IntSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: Int,
    val range: IntRange,
    val step: Int = 1,
    val mouseScrollEnabled: Boolean = true,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<Int>(key, displayName, description, value, detail, needsExtensionPack) {

    override fun parseValue(value: Any): Int {
        val number = when (value) {
            is Number -> value.toInt()
            is String -> value.trim().toDoubleOrNull()?.toInt()
            else -> null
        } ?: throw IllegalArgumentException("Not a number: $value")

        return number.coerceIn(range)
    }
}

class TextSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: String,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<String>(key, displayName, description, value, detail, needsExtensionPack) {

    val history: MutableSet<String> = mutableSetOf()

    override fun parseValue(value: Any): String = value.toString()

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        return mutableMapOf(
            settingKey(parentPath) to mutableMapOf(
                "current_value" to value,
                "history" to history
            )
        )
    }

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        val nested = settings[settingKey(parentPath)] as? Map<*, *> ?: return

        val current = nested["current_value"]
        if (current != null) {
            value = parseValue(current)
        }

        val historyList = nested["history"] as? List<*> ?: return

        history.clear()

        historyList.forEach { entry ->
            val str = entry as? String
            if (str != null) {
                history.add(str)
            }
        }
    }

}


class ActionSetting(
    key: String,
    displayName: String,
    description: String,
    val buttonLabel: String,
    override var value: String = "",
    val onPressed: (ActionSetting) -> Unit,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<String>(key, displayName, description, value, detail, needsExtensionPack) {

    override fun parseValue(value: Any): String = value.toString()
}

class PresetLibrarySetting(
    key: String,
    displayName: String,
    description: String,
    val settingUnder: () -> SettingNode<*>,
    val defaultName: String,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<String>(key, displayName, description, defaultName, detail, needsExtensionPack) {

    val configSettingPresets: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()

    private var defaultConfigOptions: MutableMap<String, Any>? = null

    fun presetNames(): List<String> = listOf(defaultName) + configSettingPresets.keys.sorted()

    fun storeDefault() {
        if (defaultConfigOptions == null) defaultConfigOptions = settingUnder().serializeSettings()
    }

    fun save(name: String) {
        if (name.isBlank() || name == defaultName) return //todo add a warning

        configSettingPresets[name] = settingUnder().serializeSettings()
        value = name
    }

    fun delete(name: String) {
        if (name == defaultName) return

        configSettingPresets.remove(name) //todo add confirmation
        if (value == name) applyPreset(defaultName)
    }

    fun applyPreset(name: String) {
        val saved = if (name == defaultName) defaultConfigOptions else configSettingPresets[name]

        value = name
        saved?.let { settingUnder().updateSettings(it) }
    }

    fun savePreset(): String {
        if (value != defaultName) return value

        return generateSequence(1) { it + 1 }.map { "Preset $it" }.first { it !in configSettingPresets }
    }

    fun settingsDirty(): Boolean {
        val savedSettings = if (value == defaultName) defaultConfigOptions else configSettingPresets[value]

        return savedSettings != null && savedSettings != settingUnder().serializeSettings()
    }

    override fun parseValue(value: Any): String = value.toString()

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> = mutableMapOf(
        settingKey(parentPath) to mutableMapOf(
            "current_value" to value,
            "presets" to configSettingPresets
        )
    )

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        val nested = settings[settingKey(parentPath)] as? Map<*, *> ?: return

        nested["current_value"]?.let { value = parseValue(it) }

        val saved = nested["presets"] as? Map<*, *> ?: return

        configSettingPresets.clear()
        saved.forEach { (name, contents) ->
            val asMap = contents as? Map<*, *> ?: return@forEach
            val entries = mutableMapOf<String, Any>()

            asMap.forEach { (key, entry) -> if (key is String && entry != null) entries[key] = entry }
            if (name is String) configSettingPresets[name] = entries
        }
    }
}

// just a header with no setting on itself, but still namespaces the key.
class ParentSetting(
    key: String,
    displayName: String,
    description: String,
    override val children: List<SettingNode<*>>,
    needsExtensionPack: Boolean = false
) : SettingNode<Unit>(key, displayName, description, Unit, needsExtensionPack = needsExtensionPack) {

    override fun parseValue(value: Any) = Unit

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        val childPath = settingKey(parentPath)

        children.forEach { map.putAll(it.serializeSettings(childPath)) }

        return map
    }

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        val childPath = settingKey(parentPath)

        children.forEach { it.updateSettings(settings, childPath) }
    }
}

class ChoiceSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: String = "",
    val options: () -> List<String>,
    val onChosen: ((ChoiceSetting) -> Unit)? = null,
    val confirm: ((String) -> Confirmation?)? = null,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<String>(key, displayName, description, value, detail, needsExtensionPack) {

    class Confirmation(val question: String, val warning: String? = null)

    override fun parseValue(value: Any): String = value.toString()
}

class EnumSetting<T : Enum<T>>(
    key: String,
    displayName: String,
    description: String,
    value: T,
    override val children: List<SettingNode<*>>? = null,
    val childrenProvider: ((T) -> List<SettingNode<*>>)? = null,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<T>(key, displayName, description, value, detail, needsExtensionPack) {

    private var activeChildren: List<SettingNode<*>>? =
        childrenProvider?.invoke(value)

    val providedChildren: List<SettingNode<*>>
        get() = activeChildren.orEmpty().filter { it.isAvailable }

    override var value: T = value
        set(newValue) {
            if (field == newValue) {return}
            field = newValue
            activeChildren = childrenProvider?.invoke(newValue)
        }

    private fun everyChild(): List<SettingNode<*>> = children.orEmpty() + providedChildren

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        val map = super.serializeSettings(parentPath)
        val childPath = settingKey(parentPath)

        everyChild().forEach { child ->
            map.putAll(child.serializeSettings(childPath))
        }
        return map
    }

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        super.updateSettings(settings, parentPath)

        val childPath = settingKey(parentPath)
        everyChild().forEach { child ->
            child.updateSettings(settings, childPath)
        }
    }

    override fun parseValue(value: Any): T {
        return java.lang.Enum.valueOf(this.value.javaClass, value as String)
    }

}