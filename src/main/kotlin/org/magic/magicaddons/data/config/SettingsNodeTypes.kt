package org.magic.magicaddons.data.config

import org.magic.magicaddons.data.ListEntry
import kotlin.collections.get

sealed class SettingNode<T>(
    val key: String,
    val displayName: String,
    val tooltip: String,
    open var value: T

) {
    open val children: List<SettingNode<*>>? = null

    open fun serializeSettings(): MutableMap<String, Any>{
        val result = mutableMapOf<String, Any>()
        result[key] = value as Any
        return result
    }
    open fun updateSettings(settings: MutableMap<String, Any>) {
        val newValue = settings[key] ?: return
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
    override var value: MutableList<ListEntry>
) : SettingNode<MutableList<ListEntry>>(key, displayName, tooltip, value) {

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

    override fun serializeSettings(): MutableMap<String, Any> {
        return mutableMapOf(
            key to value.map { entry ->
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
    override var children: List<SettingNode<*>>? = null
) : SettingNode<Boolean>(key, displayName, tooltip, value) {

    override fun serializeSettings(): MutableMap<String, Any> {
        val map = super.serializeSettings()
        children?.forEach { child ->
            map.putAll(child.serializeSettings())
        }
        return map
    }
    override fun updateSettings(settings: MutableMap<String, Any>) {
        super.updateSettings(settings)
        children?.forEach { child ->
            child.updateSettings(settings)
        }
    }
    override fun parseValue(value: Any): Boolean = value as Boolean
}

class TextSetting(
    key: String,
    displayName: String,
    tooltip: String,
    override var value: String
) : SettingNode<String>(key, displayName, tooltip, value) {

    val history: MutableSet<String> = mutableSetOf()

    override fun parseValue(value: Any): String = value.toString()

    override fun serializeSettings(): MutableMap<String, Any> {
        return mutableMapOf(
            key to mutableMapOf(
                "current_value" to value,
                "history" to history
            )
        )
    }

    override fun updateSettings(settings: MutableMap<String, Any>) {
        val nested = settings[key] as? Map<*, *> ?: return

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
    override val children: List<SettingNode<*>>?,
    val childrenProvider: ((T) -> List<SettingNode<*>>)?
) : SettingNode<T>(key, displayName, tooltip, value) {

    private var activeChildren: List<SettingNode<*>>? =
        childrenProvider?.invoke(value)

    override var value: T = value
        set(newValue) {
            if (field == newValue) {return}
            field = newValue
            activeChildren = childrenProvider?.invoke(newValue)
        }

    override fun serializeSettings(): MutableMap<String, Any> {
        val map = super.serializeSettings()
        children?.forEach { child ->
            map.putAll(child.serializeSettings())
        }
        return map
    }

    override fun updateSettings(settings: MutableMap<String, Any>) {
        super.updateSettings(settings)
        children?.forEach { child ->
            child.updateSettings(settings)
        }
    }

    override fun parseValue(value: Any): T {
        return java.lang.Enum.valueOf(this.value.javaClass, value as String)
    }

}