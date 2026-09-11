package org.magic.magicaddons.data.config

import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import kotlin.collections.get

sealed class SettingNode<T>(
    val key: String,
    val displayName: String,
    val description: String,
    open var value: T,
    /** Live text under this setting's row, asked afresh every frame rather than stored. */
    val detail: (() -> SettingDetail?)? = null

) {
    open val children: List<SettingNode<*>>? = null

    /**
     * The key this node is stored under, namespaced by its parent ("Parent.Child"), so keys only
     * have to be unique among siblings.
     */
    fun pathIn(parentPath: String): String = if (parentPath.isEmpty()) key else "$parentPath.$key"

    open fun serializeSettings(parentPath: String = ""): MutableMap<String, Any>{
        val result = mutableMapOf<String, Any>()
        result[pathIn(parentPath)] = value as Any
        return result
    }
    open fun updateSettings(settings: Map<String, Any>, parentPath: String = "") {
        updateOwnValue(settings, parentPath)
    }

    protected fun updateOwnValue(settings: Map<String, Any>, parentPath: String) {
        val newValue = settings[pathIn(parentPath)] ?: return
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

/**
 * A list picked from a fixed set of names: the widget offers whatever [choices] returns that is not
 * already in the list, and each entry keeps its own on/off switch.
 */
class ToggleListSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: MutableList<ListEntry>,
    val choices: () -> List<String>,
    /** What the closed selector says. It searches the whole catalogue, listed and not. */
    val searchLabel: String = "Search",
    /** Whether a search box sits above the rows; a short fixed list has nothing worth searching. */
    val searchable: Boolean = true,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<MutableList<ListEntry>>(key, displayName, description, value, detail) {

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
            pathIn(parentPath) to value.map { entry ->
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
    override var value: Boolean,
    override var children: List<SettingNode<*>>? = null,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<Boolean>(key, displayName, description, value, detail) {

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        val map = super.serializeSettings(parentPath)
        val childPath = pathIn(parentPath)
        children?.forEach { child ->
            map.putAll(child.serializeSettings(childPath))
        }
        return map
    }
    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        super.updateSettings(settings, parentPath)
        val childPath = pathIn(parentPath)
        children?.forEach { child ->
            child.updateSettings(settings, childPath)
        }
    }
    override fun parseValue(value: Any): Boolean = value as Boolean

    /** A feature toggle is its own root: stored under its key, children at the top level. */
    fun serializeAsFeatureRoot(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>(key to value)
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

/**
 * A whole number picked by dragging a bar or typing one. The step is how far a drag moves it, never
 * a constraint: a typed number lands exactly where it was typed, clamped only to the range.
 */
class IntSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: Int,
    val range: IntRange,
    val step: Int = 1,
    /** Whether the wheel over the bar moves the number, for one a stray scroll should not change. */
    val scrollable: Boolean = true,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<Int>(key, displayName, description, value, detail) {

    /** Gson hands numbers back as doubles, and an older config may hold the number as text. */
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
    detail: (() -> SettingDetail?)? = null
) : SettingNode<String>(key, displayName, description, value, detail) {

    val history: MutableSet<String> = mutableSetOf()

    override fun parseValue(value: Any): String = value.toString()

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        return mutableMapOf(
            pathIn(parentPath) to mutableMapOf(
                "current_value" to value,
                "history" to history
            )
        )
    }

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        val nested = settings[pathIn(parentPath)] as? Map<*, *> ?: return

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


/**
 * A row with a button. Pressing it runs [onPressed], which may write what it produced back into
 * [value]; that value is what gets stored, so a file the player picked is still picked next time.
 */
class ActionSetting(
    key: String,
    displayName: String,
    description: String,
    /** What the button itself reads. */
    val buttonLabel: String,
    override var value: String = "",
    val onPressed: (ActionSetting) -> Unit,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<String>(key, displayName, description, value, detail) {

    override fun parseValue(value: Any): String = value.toString()
}

/**
 * Named copies of a group of settings. A preset holds the same map the config file itself stores, so
 * saving one is the settings serialised and applying one is that map handed back to them.
 */
class PresetLibrarySetting(
    key: String,
    displayName: String,
    description: String,
    /** The settings a preset is taken from and applied to. */
    val subject: () -> SettingNode<*>,
    /** The preset every list starts with, which cannot be written over or removed. */
    val defaultName: String,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<String>(key, displayName, description, defaultName, detail) {

    /** Each saved preset by name, holding what the settings looked like when it was saved. */
    val presets: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()

    /** What the settings looked like before any config was read, which is what Default restores. */
    private var shipped: MutableMap<String, Any>? = null

    /** The names to choose between, the default one first. */
    fun names(): List<String> = listOf(defaultName) + presets.keys.sorted()

    /** Remembers the settings as they ship, the first time anything asks. */
    fun rememberShipped() {
        if (shipped == null) shipped = subject().serializeSettings()
    }

    fun save(name: String) {
        if (name.isBlank() || name == defaultName) return

        presets[name] = subject().serializeSettings()
        value = name
    }

    fun delete(name: String) {
        if (name == defaultName) return

        presets.remove(name)
        if (value == name) apply(defaultName)
    }

    /** Puts a preset's settings back, or the shipped ones for the default. */
    fun apply(name: String) {
        val saved = if (name == defaultName) shipped else presets[name]

        value = name
        saved?.let { subject().updateSettings(it) }
    }

    /** Where edits would be saved: the picked preset, or the first free "Preset N" when it is the default. */
    fun saveTarget(): String {
        if (value != defaultName) return value

        return generateSequence(1) { it + 1 }.map { "Preset $it" }.first { it !in presets }
    }

    /** Whether the settings have moved away from the preset picked, so it shows as edited. */
    fun edited(): Boolean {
        val saved = if (value == defaultName) shipped else presets[value]

        return saved != null && saved != subject().serializeSettings()
    }

    override fun parseValue(value: Any): String = value.toString()

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> = mutableMapOf(
        pathIn(parentPath) to mutableMapOf<String, Any>(
            "current_value" to value,
            "presets" to presets
        )
    )

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        val nested = settings[pathIn(parentPath)] as? Map<*, *> ?: return

        nested["current_value"]?.let { value = parseValue(it) }

        val saved = nested["presets"] as? Map<*, *> ?: return

        presets.clear()
        saved.forEach { (name, contents) ->
            val asMap = contents as? Map<*, *> ?: return@forEach
            val entries = mutableMapOf<String, Any>()

            asMap.forEach { (key, entry) -> if (key is String && entry != null) entries[key] = entry }
            if (name is String) presets[name] = entries
        }
    }
}

/**
 * A heading with settings under it. It holds no value of its own, so nothing is written for the
 * heading itself, but it still namespaces what is under it: two settings may share a key as long as
 * they sit under different headings.
 */
class ParentSetting(
    key: String,
    displayName: String,
    description: String,
    override val children: List<SettingNode<*>>
) : SettingNode<Unit>(key, displayName, description, Unit) {

    override fun parseValue(value: Any) = Unit

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        val childPath = pathIn(parentPath)

        children.forEach { map.putAll(it.serializeSettings(childPath)) }

        return map
    }

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        val childPath = pathIn(parentPath)

        children.forEach { it.updateSettings(settings, childPath) }
    }
}

/**
 * A value picked from a list that is not known ahead of time, such as the files in a folder. The
 * options are asked for afresh each time the row is drawn.
 */
class ChoiceSetting(
    key: String,
    displayName: String,
    description: String,
    override var value: String = "",
    val options: () -> List<String>,
    val onChosen: ((ChoiceSetting) -> Unit)? = null,
    /** What to ask before a value is taken, or null for one that needs no asking. */
    val confirm: ((String) -> Confirmation?)? = null,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<String>(key, displayName, description, value, detail) {

    /** A question put before a value is taken, with a warning under it when there is one. */
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
    detail: (() -> SettingDetail?)? = null
) : SettingNode<T>(key, displayName, description, value, detail) {

    private var activeChildren: List<SettingNode<*>>? =
        childrenProvider?.invoke(value)

    /** The settings the current value brings with it, none when there is no provider. */
    val providedChildren: List<SettingNode<*>> get() = activeChildren.orEmpty()

    override var value: T = value
        set(newValue) {
            if (field == newValue) {return}
            field = newValue
            activeChildren = childrenProvider?.invoke(newValue)
        }

    /** The fixed settings under this one and the ones the picked value brought with it. */
    private fun everyChild(): List<SettingNode<*>> = children.orEmpty() + providedChildren

    override fun serializeSettings(parentPath: String): MutableMap<String, Any> {
        val map = super.serializeSettings(parentPath)
        val childPath = pathIn(parentPath)

        // what the value brought with it is stored too, or a picture picked under one value would be
        // forgotten the moment the game closed
        everyChild().forEach { child ->
            map.putAll(child.serializeSettings(childPath))
        }
        return map
    }

    override fun updateSettings(settings: Map<String, Any>, parentPath: String) {
        // the value is read first, so the settings it brings with it exist before they are read
        super.updateSettings(settings, parentPath)

        val childPath = pathIn(parentPath)
        everyChild().forEach { child ->
            child.updateSettings(settings, childPath)
        }
    }

    override fun parseValue(value: Any): T {
        return java.lang.Enum.valueOf(this.value.javaClass, value as String)
    }

}