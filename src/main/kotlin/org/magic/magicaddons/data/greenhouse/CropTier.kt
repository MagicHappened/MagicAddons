package org.magic.magicaddons.data.greenhouse

enum class CropTier(val listingName: String, val heading: String) {
    BaseCrop("base crops", "Base Crops"),
    Common("common mutations", "Common"),
    Uncommon("uncommon mutations", "Uncommon"),
    Rare("rare mutations", "Rare"),
    Epic("epic mutations", "Epic"),
    Legendary("legendary mutations", "Legendary"),
    RareCrop("rare crops", "Rare Crops"),
    Other("other", "Misc")
}
