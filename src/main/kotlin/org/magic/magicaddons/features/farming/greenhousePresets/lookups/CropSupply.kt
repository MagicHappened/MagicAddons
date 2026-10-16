package org.magic.magicaddons.features.farming.greenhousePresets.lookups

import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import tech.thatgravyboat.skyblockapi.api.profile.items.sacks.SacksAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object CropSupply {

    class SeedRecipe(val seedId: SkyBlockId, val sackItemId: SkyBlockId, val seedsPerSackItem: Int, val sackName: String)

    private val SEEDS_ID: SkyBlockId = SkyBlockItemId.item("SEEDS")

    private val SEED_RECIPE_BY_CROP_ID: Map<SkyBlockId, SeedRecipe> = mapOf(
        SkyBlockItemId.item("WHEAT") to SeedRecipe(SEEDS_ID, SEEDS_ID, 1, "seeds"),
        SkyBlockItemId.item("PUMPKIN") to SeedRecipe(SkyBlockItemId.item("PUMPKIN_SEEDS"), SkyBlockItemId.item("PUMPKIN"), 4, "pumpkin"),
        SkyBlockItemId.item("MELON") to SeedRecipe(SkyBlockItemId.item("MELON_SEEDS"), SkyBlockItemId.item("MELON"), 1, "melon")
    )

    fun seedRecipeOf(crop: CropDefinition): SeedRecipe? = crop.skyblockId?.let { SEED_RECIPE_BY_CROP_ID[it] }

    fun inventoryStacks(): List<ItemStack> {
        val player = Minecraft.getInstance().player ?: return emptyList()
        val inventory = player.inventory
        return (0 until inventory.containerSize).map { inventory.getItem(it) }
    }

    fun heldItemCount(id: SkyBlockId): Int =
        inventoryStacks().filter { !it.isEmpty && it.getSkyBlockId() == id }.sumOf { it.count }

    fun cropOfStack(stack: ItemStack): CropDefinition? =
        if (stack.isEmpty) null else stack.getSkyBlockId()?.id?.let { CropRegistry.findByIdOrName(it) }

    fun heldCropCount(crop: CropDefinition): Int =
        inventoryStacks().filter { cropOfStack(it) == crop }.sumOf { it.count }

    fun heldSeedCount(seedRecipe: SeedRecipe): Int =
        heldItemCount(seedRecipe.seedId) +
                if (seedRecipe.sackItemId == seedRecipe.seedId) 0 else heldItemCount(seedRecipe.sackItemId) * seedRecipe.seedsPerSackItem

    private fun sackCount(id: SkyBlockId): Int = SacksAPI.sackItems[id.skyblockId] ?: 0

    fun ownedCount(crop: CropDefinition): Int? {
        val cropId = crop.skyblockId ?: return null
        val seedRecipe = seedRecipeOf(crop) ?: return heldCropCount(crop) + sackCount(cropId)

        val seedsInSacks = sackCount(seedRecipe.seedId) +
                if (seedRecipe.sackItemId == seedRecipe.seedId) 0 else sackCount(seedRecipe.sackItemId) * seedRecipe.seedsPerSackItem
        return heldSeedCount(seedRecipe) + seedsInSacks
    }
}
