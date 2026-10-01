package org.magic.magicaddons.data.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.magic.magicaddons.ExtensionPack
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.ui.widgets.config.SettingDetail

sealed class SettingNode<T>(
    val key: String,
    val displayName: String,
    val description: String,
    open var value: T,
    val detail: (() -> SettingDetail?)? = null,
    val needsExtensionPack: Boolean = false,
    val requiresServer: Boolean = false
) {
    open val children: List<SettingNode<*>>? = null

    var parent: SettingNode<*>? = null
        private set

    protected fun setAsParentOf(childSettings: List<SettingNode<*>>?) {
        childSettings?.forEach { it.parent = this }
    }

    protected open val isSwitchedOn: Boolean get() = true

    protected open fun isShowingChild(child: SettingNode<*>): Boolean = true

    val isEnabled: Boolean
        get() {
            if (!isAvailable || !isSwitchedOn) return false

            val settingAbove = parent ?: return true
            return settingAbove.isShowingChild(this) && settingAbove.isEnabled
        }

    val valueIfEnabled: T? get() = value.takeIf { isEnabled }

    val isAvailable: Boolean get() = !needsExtensionPack || ExtensionPack.isInstalled

    val availableChildren: List<SettingNode<*>> get() = children.orEmpty().filter { it.isAvailable }

    fun settingKey(parentPath: String): String = if (parentPath.isEmpty()) key else "$parentPath.$key"

    protected open val savedChildren: List<SettingNode<*>> get() = children.orEmpty()

    protected abstract fun valueToJson(): JsonElement?

    protected abstract fun valueFromJson(json: JsonElement): T

    fun writeTo(settingsJson: JsonObject, parentPath: String = "") {
        val path = settingKey(parentPath)
        valueToJson()?.let { settingsJson.add(path, it) }
        savedChildren.forEach { it.writeTo(settingsJson, path) }
    }

    fun readFrom(settingsJson: JsonObject, parentPath: String = "") {
        val path = settingKey(parentPath)
        settingsJson.get(path)?.let { json -> runCatching { value = valueFromJson(json) } }
        savedChildren.forEach { it.readFrom(settingsJson, path) }
    }

    fun toSettingsJson(): JsonObject = JsonObject().also { writeTo(it) }

    inline fun <reified R : SettingNode<*>> getChild(key: String): R? {
        return children?.filterIsInstance<R>()?.firstOrNull { it.key == key }
    }

    inline fun <reified R : SettingNode<*>> getChildOrThrow(key: String): R {
        return getChild<R>(key) ?: throw IllegalStateException("No child with key '$key' of type ${R::class.java.name}")
    }

    companion object {
        const val SERVER_ICON_TOKEN: String = "{icon}"
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

    override fun valueFromJson(json: JsonElement): MutableList<ListEntry> =
        json.asJsonArray.mapNotNull { entryJson ->
            val entry = entryJson as? JsonObject ?: return@mapNotNull null
            val entryValue = entry.get("value")?.asString ?: return@mapNotNull null
            val enabledJson = entry.get("enabled") as? JsonPrimitive

            val isEntryEnabled = when {
                enabledJson == null -> true
                enabledJson.isBoolean -> enabledJson.asBoolean
                enabledJson.isNumber -> enabledJson.asInt != 0
                else -> enabledJson.asString.toBoolean()
            }

            ListEntry(name = entry.get("name")?.asString ?: "", value = entryValue, enabled = isEntryEnabled)
        }.toMutableList()

    override fun valueToJson(): JsonElement = JsonArray().also { array ->
        value.forEach { entry ->
            array.add(JsonObject().apply {
                addProperty("name", entry.name)
                addProperty("value", entry.value)
                addProperty("enabled", entry.enabled)
            })
        }
    }
}

class BooleanSetting(
    key: String = "enabled",
    displayName: String,
    description: String,
    value: Boolean,
    override val children: List<SettingNode<*>>? = null,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false,
    requiresServer: Boolean = false,
    val valueChanged: ((Boolean) -> Unit)? = null
) : SettingNode<Boolean>(key, displayName, description, value, detail, needsExtensionPack, requiresServer) {

    init {
        setAsParentOf(children)
    }

    private var storedValue: Boolean = value

    override val isSwitchedOn: Boolean get() = value

    override var value: Boolean
        get() = storedValue && isAvailable
        set(newValue) {
            storedValue = newValue
        }

    override fun valueToJson(): JsonElement = JsonPrimitive(storedValue)

    override fun valueFromJson(json: JsonElement): Boolean = json.asBoolean

    fun writeAsFeatureRoot(settingsJson: JsonObject) {
        settingsJson.addProperty(key, storedValue)
        savedChildren.forEach { it.writeTo(settingsJson) }
    }

    fun readAsFeatureRoot(settingsJson: JsonObject) {
        settingsJson.get(key)?.let { json -> runCatching { value = valueFromJson(json) } }
        savedChildren.forEach { it.readFrom(settingsJson) }
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

    override fun valueToJson(): JsonElement = JsonPrimitive(value)

    override fun valueFromJson(json: JsonElement): Int {
        val primitive = json.asJsonPrimitive
        val number = if (primitive.isNumber) primitive.asDouble.toInt() else primitive.asString.trim().toDouble().toInt()
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

    override fun valueToJson(): JsonElement = JsonObject().apply {
        addProperty("current_value", value)
        add("history", JsonArray().also { array -> history.forEach { array.add(it) } })
    }

    override fun valueFromJson(json: JsonElement): String {
        val stored = json.asJsonObject
        stored.getAsJsonArray("history")?.let { historyJson ->
            history.clear()
            historyJson.forEach { entry -> runCatching { history.add(entry.asString) } }
        }
        return stored.get("current_value")?.asString ?: value
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

    override fun valueToJson(): JsonElement = JsonPrimitive(value)

    override fun valueFromJson(json: JsonElement): String = json.asString
}

class PresetLibrarySetting(
    key: String,
    displayName: String,
    description: String,
    val settingUnder: SettingNode<*>,
    val defaultName: String,
    detail: (() -> SettingDetail?)? = null,
    needsExtensionPack: Boolean = false
) : SettingNode<String>(key, displayName, description, defaultName, detail, needsExtensionPack) {

    val configSettingPresets: MutableMap<String, JsonObject> = mutableMapOf()

    private val defaultConfigOptions: JsonObject = settingUnder.toSettingsJson()

    fun presetNames(): List<String> = listOf(defaultName) + configSettingPresets.keys.sorted()

    fun save(name: String) {
        if (name.isBlank() || name == defaultName) return //todo add a warning

        configSettingPresets[name] = settingUnder.toSettingsJson()
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
        saved?.let { settingUnder.readFrom(it) }
    }

    fun savePreset(): String {
        if (value != defaultName) return value

        return generateSequence(1) { it + 1 }.map { "Preset $it" }.first { it !in configSettingPresets }
    }

    fun settingsDirty(): Boolean {
        val savedSettings = if (value == defaultName) defaultConfigOptions else configSettingPresets[value]

        return savedSettings != null && savedSettings != settingUnder.toSettingsJson()
    }

    override fun valueToJson(): JsonElement = JsonObject().apply {
        addProperty("current_value", value)
        add("presets", JsonObject().also { presetsJson -> configSettingPresets.forEach { (name, settings) -> presetsJson.add(name, settings) } })
    }

    override fun valueFromJson(json: JsonElement): String {
        val stored = json.asJsonObject
        stored.getAsJsonObject("presets")?.let { presetsJson ->
            configSettingPresets.clear()
            presetsJson.entrySet().forEach { (name, settings) -> (settings as? JsonObject)?.let { configSettingPresets[name] = it } }
        }
        return stored.get("current_value")?.asString ?: value
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

    init {
        setAsParentOf(children)
    }

    override fun valueToJson(): JsonElement? = null

    override fun valueFromJson(json: JsonElement) = Unit
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

    override fun valueToJson(): JsonElement = JsonPrimitive(value)

    override fun valueFromJson(json: JsonElement): String = json.asString
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

    init {
        setAsParentOf(children)
        setAsParentOf(activeChildren)
    }

    override fun isShowingChild(child: SettingNode<*>): Boolean =
        children.orEmpty().any { it === child } || activeChildren.orEmpty().any { it === child }

    val providedChildren: List<SettingNode<*>>
        get() = activeChildren.orEmpty().filter { it.isAvailable }

    override var value: T = value
        set(newValue) {
            if (field == newValue) {return}
            field = newValue
            activeChildren = childrenProvider?.invoke(newValue)
            setAsParentOf(activeChildren)
        }

    override val savedChildren: List<SettingNode<*>> get() = children.orEmpty() + providedChildren

    override fun valueToJson(): JsonElement = JsonPrimitive(value.name)

    override fun valueFromJson(json: JsonElement): T = java.lang.Enum.valueOf(value.javaClass, json.asString)

}