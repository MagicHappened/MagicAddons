package org.magic.magicaddons.features.farming.greenhousePresets

import com.github.kdgaming0.enhancedstorage.storage.StorageCache
import com.github.kdgaming0.enhancedstorage.storage.StorageKey
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import net.minecraft.world.item.ItemStack

/** gets ender chest and backpack contents from Enhanced Storage 1.2+ (if installed) */
object StorageBridge {

    private const val MOD_ID: String = "enhanced_storage"

    private val SUPPORTED_VERSIONS: VersionPredicate = VersionPredicate.parse(">=1.2.0")

    fun openStorageCommand(query: String): String = "/ecs $query"

    val available: Boolean by lazy {
        FabricLoader.getInstance().getModContainer(MOD_ID)
            .map { SUPPORTED_VERSIONS.test(it.metadata.version) }
            .orElse(false)
    }

    class Holding(val page: String, val count: Int)

    /** every page holding items that [matches], most first; empty without the mod */
    fun pagesHolding(matches: (ItemStack) -> Boolean): List<Holding> {
        if (!available) return emptyList()

        return EnhancedStorageCache.pagesHolding(matches)
    }
}

/** names Enhanced Storage's classes; only loaded when the mod is present */
private object EnhancedStorageCache {

    fun pagesHolding(matches: (ItemStack) -> Boolean): List<StorageBridge.Holding> =
        StorageCache.getInstance().all().entries
            .sortedWith(compareBy(StorageKey.DISPLAY_ORDER) { it.key })
            .mapNotNull { (key, page) ->
                val count = page.items().filter { !it.isEmpty && matches(it) }.sumOf { it.count }
                if (count > 0) StorageBridge.Holding(key.displayName(), count) else null
            }
            .sortedByDescending { it.count }
}
