package org.magic.magicaddons.config.data

sealed class SettingNode<T>(
    val key: String,
    val displayName: String,
    val tooltip: String,
    open var value: T

) {
    open val children: List<SettingNode<*>>? = null

    open fun serializeSettings(): MutableMap<String, String>{
        val result = mutableMapOf<String, String>()
        result[key] = value.toString()
        return result
    }
    open fun updateSettings(settings: MutableMap<String, String>) {
        val newValue = settings[key]
        if (newValue != null) {
            value = parseValue(newValue)
        }
    }
    protected abstract fun parseValue(value: String): T
}

open class BooleanSetting(
    key: String,
    displayName: String,
    tooltip: String,
    override var value: Boolean,
    override var children: List<SettingNode<*>>? = null
) : SettingNode<Boolean>(key, displayName, tooltip, value) {

    override fun serializeSettings(): MutableMap<String, String> {
        val map = super.serializeSettings()
        children?.forEach { child ->
            map.putAll(child.serializeSettings())
        }
        return map
    }
    override fun updateSettings(settings: MutableMap<String, String>) {
        super.updateSettings(settings)
        children?.forEach { child ->
            child.updateSettings(settings)
        }
    }
    override fun parseValue(value: String): Boolean = value.toBoolean()
}

class TextSetting(
    key: String,
    displayName: String,
    tooltip: String,
    override var value: String
) : SettingNode<String>(key, displayName, tooltip, value) {
    override fun parseValue(value: String): String = value
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

    override fun serializeSettings(): MutableMap<String, String> {
        val map = super.serializeSettings()
        children?.forEach { child ->
            map.putAll(child.serializeSettings())
        }
        return map
    }

    override fun updateSettings(settings: MutableMap<String, String>) {
        super.updateSettings(settings)
        children?.forEach { child ->
            child.updateSettings(settings)
        }
    }




    override fun parseValue(value: String): T {
        return java.lang.Enum.valueOf(this.value.javaClass, value)
    }

}