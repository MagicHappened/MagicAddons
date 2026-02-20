package org.magic.magicaddons.config

import org.magic.magicaddons.config.option.Position

object MagicAddonsConfig {

    val categories = mutableMapOf<String, MutableMap<String, FeatureConfig>>()

    init {
        categories["farming"] = mutableMapOf(
            "highlightFarmingEquipment" to FeatureConfig(
                enabled = true,
                extra = mutableMapOf(
                    "equipmentColor" to 0xFFFF0000,
                    "zorroSwapping" to false
                )
            )
        )

        categories["mining"] = mutableMapOf(
            "pureOresTracker" to FeatureConfig(
                enabled = true,
                extra = mutableMapOf(
                    "prefixColor" to 0xFFFF0000,
                    "valueColor" to 0xFFFF0000,
                    "hudPosition" to Position(10,10)
                )
            )
        )

        categories["misc"] = mutableMapOf(
            "npcBlocking" to FeatureConfig(
                enabled = false,
                extra = mutableMapOf(
                    "keywordTriggers" to listOf(
                        "Goon"
                    )
                )
            ),
            "jerryAnnouncer" to FeatureConfig(
                enabled = false
            )
        )
    }
}

