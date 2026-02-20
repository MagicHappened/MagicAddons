package org.magic.magicaddons.util

import org.magic.magicaddons.MagicAddons
import java.util.Collections.emptyList

object TablistUtils {
    var lines: List<String> = emptyList()

    private var tablistDirty: Boolean = false

    @JvmStatic
    fun markTabListDirty() {
        tablistDirty = true;
    }

    @JvmStatic
    fun updateTabList(){
        if (!tablistDirty) return
        lines = MagicAddons.client.player?.networkHandler?.playerList
            ?.mapNotNull { it.displayName?.string } ?: emptyList()
        // TODO implement event for tablist listeners?
        tablistDirty = false
    }

    fun lineExists(keyword: String): Boolean = lines.any { it.contains(keyword) }
}