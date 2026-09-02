package org.magic.magicaddons.data.config

import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import kotlin.collections.get

sealed class SettingNode<T>(
    val key: String,
    val displayName: String,
    val tooltip: String,
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

class ToggleListSetting(
    key: String,
    displayName: String,
    tooltip: String,
    override var value: MutableList<ListEntry>,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<MutableList<ListEntry>>(key, displayName, tooltip, value, detail) {

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
    tooltip: String,
    override var value: Boolean,
    override var children: List<SettingNode<*>>? = null,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<Boolean>(key, displayName, tooltip, value, detail) {

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
    tooltip: String,
    override var value: Int,
    val range: IntRange,
    val step: Int = 1,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<Int>(key, displayName, tooltip, value, detail) {

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
    tooltip: String,
    override var value: String,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<String>(key, displayName, tooltip, value, detail) {

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


class EnumSetting<T : Enum<T>>(
    key: String,
    displayName: String,
    tooltip: String,
    value: T,
    override val children: List<SettingNode<*>>? = null,
    val childrenProvider: ((T) -> List<SettingNode<*>>)? = null,
    detail: (() -> SettingDetail?)? = null
) : SettingNode<T>(key, displayName, tooltip, value, detail) {

    private var activeChildren: List<SettingNode<*>>? =
        childrenProvider?.invoke(value)

    override var value: T = value
        set(newValue) {
            if (field == newValue) {return}
            field = newValue
            activeChildren = childrenProvider?.invoke(newValue)
        }

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

    override fun parseValue(value: Any): T {
        return java.lang.Enum.valueOf(this.value.javaClass, value as String)
    }

}