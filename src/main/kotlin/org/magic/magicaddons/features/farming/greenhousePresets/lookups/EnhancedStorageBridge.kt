package org.magic.magicaddons.features.farming.greenhousePresets.lookups

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import net.minecraft.world.item.ItemStack
import com.github.kdgaming0.enhancedstorage.storage.StorageCache as StorageCacheV1
import com.github.kdgaming0.enhancedstorage.storage.StorageKey as StorageKeyV1
import com.github.kdgaming0.enhancedstorage.storage.data.PageCache as PageCacheV2
import com.github.kdgaming0.enhancedstorage.storage.data.StorageKey as StorageKeyV2

object EnhancedStorageBridge {

    private const val ENHANCED_STORAGE_MOD_ID: String = "enhanced_storage"

    private val STORAGE_CACHE_VERSIONS: VersionPredicate = VersionPredicate.parse(">=1.2.0 <2.0.0")
    private val PAGE_CACHE_VERSIONS: VersionPredicate = VersionPredicate.parse(">=2.0.0 <3.0.0")

    fun storageSearchCommand(query: String): String = "/ecs $query"

    private var reader: EnhancedStorageReader? = FabricLoader.getInstance().getModContainer(ENHANCED_STORAGE_MOD_ID)
        .map { container ->
            val version = container.metadata.version
            when {
                PAGE_CACHE_VERSIONS.test(version) -> PageCacheReader
                STORAGE_CACHE_VERSIONS.test(version) -> StorageCacheReader
                else -> null
            }
        }
        .orElse(null)

    class PageItemCount(val pageName: String, val count: Int)

    fun pagesHolding(isWantedItem: (ItemStack) -> Boolean): List<PageItemCount> {
        val current = reader ?: return emptyList()

        return try {
            current.pagesHolding(isWantedItem)
        } catch (_: LinkageError) {
            reader = null
            emptyList()
        }
    }
}

private interface EnhancedStorageReader {
    fun pagesHolding(isWantedItem: (ItemStack) -> Boolean): List<EnhancedStorageBridge.PageItemCount>
}

private fun List<EnhancedStorageBridge.PageItemCount>.mostFirst() = sortedByDescending { it.count }

private fun countOf(items: List<ItemStack>, isWantedItem: (ItemStack) -> Boolean): Int =
    items.filter { !it.isEmpty && isWantedItem(it) }.sumOf { it.count }

private object StorageCacheReader : EnhancedStorageReader {

    override fun pagesHolding(isWantedItem: (ItemStack) -> Boolean): List<EnhancedStorageBridge.PageItemCount> =
        StorageCacheV1.getInstance().all().entries
            .sortedWith(compareBy(StorageKeyV1.DISPLAY_ORDER) { it.key })
            .mapNotNull { (key, page) ->
                val count = countOf(page.items(), isWantedItem)
                if (count > 0) EnhancedStorageBridge.PageItemCount(key.displayName(), count) else null
            }
            .mostFirst()
}

private object PageCacheReader : EnhancedStorageReader {

    override fun pagesHolding(isWantedItem: (ItemStack) -> Boolean): List<EnhancedStorageBridge.PageItemCount> =
        PageCacheV2.getInstance().all().entries
            .sortedWith(compareBy(StorageKeyV2.DISPLAY_ORDER) { it.key })
            .mapNotNull { (key, page) ->
                val count = countOf(page.items(), isWantedItem)
                if (count > 0) EnhancedStorageBridge.PageItemCount(key.displayName(), count) else null
            }
            .mostFirst()
}
