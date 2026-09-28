package org.magic.magicaddons.features.farming.greenhousePresets.lookups

import com.github.kdgaming0.enhancedstorage.storage.StorageCache
import com.github.kdgaming0.enhancedstorage.storage.StorageKey
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import net.minecraft.world.item.ItemStack

object EnhancedStorageBridge {

    private const val ENHANCED_STORAGE_MOD_ID: String = "enhanced_storage"

    private val SUPPORTED_VERSIONS: VersionPredicate = VersionPredicate.parse(">=1.2.0")

    fun storageSearchCommand(query: String): String = "/ecs $query"

    val isEnhancedStorageLoaded: Boolean by lazy {
        FabricLoader.getInstance().getModContainer(ENHANCED_STORAGE_MOD_ID)
            .map { SUPPORTED_VERSIONS.test(it.metadata.version) }
            .orElse(false)
    }

    class PageItemCount(val pageName: String, val count: Int)

    fun pagesHolding(isWantedItem: (ItemStack) -> Boolean): List<PageItemCount> {
        if (!isEnhancedStorageLoaded) return emptyList()

        return EnhancedStorageReader.pagesHolding(isWantedItem)
    }
}

private object EnhancedStorageReader {

    fun pagesHolding(isWantedItem: (ItemStack) -> Boolean): List<EnhancedStorageBridge.PageItemCount> =
        StorageCache.getInstance().all().entries
            .sortedWith(compareBy(StorageKey.DISPLAY_ORDER) { it.key })
            .mapNotNull { (key, page) ->
                val count = page.items().filter { !it.isEmpty && isWantedItem(it) }.sumOf { it.count }
                if (count > 0) EnhancedStorageBridge.PageItemCount(key.displayName(), count) else null
            }
            .sortedByDescending { it.count }
}
