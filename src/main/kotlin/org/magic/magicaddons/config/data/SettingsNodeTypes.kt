package org.magic.magicaddons.config.data

sealed class SettingNode(
    val key: String,
    val displayName: String,
    val tooltip: String
) {
    abstract fun getChildren(): List<SettingNode>?
}

open class BooleanSetting(
    key: String,
    displayName: String,
    tooltip: String,
    var value: Boolean,
    var children: List<SettingNode>? = null
) : SettingNode(key, displayName, tooltip) {
    override fun getChildren(): List<SettingNode>? {
        return children
    }
}

class TextSetting(
    key: String,
    displayName: String,
    tooltip: String,
    var value: String
) : SettingNode(key, displayName, tooltip) {
    override fun getChildren(): List<SettingNode>? {
        return null
    } // text input setting doesn't need to be expandable
}


class EnumSetting<T : Enum<T>>(
    key: String,
    displayName: String,
    tooltip: String,
    var value: T,
    val childrenProvider: ((T) -> List<SettingNode>)?
) : SettingNode(key, displayName, tooltip) {

    override fun getChildren(): List<SettingNode>? {
        return childrenProvider?.invoke(value)
    }
}