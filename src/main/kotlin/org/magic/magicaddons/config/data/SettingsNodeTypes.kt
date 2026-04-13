package org.magic.magicaddons.config.data

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
        val newValue = settings[key]
        if (newValue != null) {
            value = parseValue(newValue)
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

class BooleanSetting(
    key: String,
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
    val childrenProvider: ((T) -> List<SettingNode<*>>)?
) : SettingNode<T>(key, displayName, tooltip, value) {

    private var cachedChildren: List<SettingNode<*>>? =
        childrenProvider?.invoke(value)

    override val children: List<SettingNode<*>>?
        get() {
            return cachedChildren
        }

    override var value: T = value
        set(newValue) {
            if (field == newValue) {return}
            field = newValue
            cachedChildren = childrenProvider?.invoke(newValue)
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