package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid.GridState
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo.Estimated
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo.Known
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import java.time.Instant
import java.util.*

object Codecs {
    val GREENHOUSE_LAYOUT_CODEC: Codec<GreenhouseLayout> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("layout_id").forGetter { it.id },

                Codec.STRING
                    .optionalFieldOf("layout_name")
                    .forGetter { Optional.ofNullable(it.name) },

                GREENHOUSE_SLOT_CODEC.listOf()
                    .fieldOf("slots")
                    .forGetter { it.slots },

                GREENHOUSE_ELEMENT_INSTANCE_CODEC.listOf()
                    .fieldOf("element_instances")
                    .forGetter { it.elementInstances }
            ).apply(instance) { id, nameOpt, slots, elements ->
                GreenhouseLayout(
                    id = id,
                    name = nameOpt.orElse(null),
                    slots = slots,
                    elementInstances = elements.toMutableList()
                )
            }
        }
    }


    val GROWTH_STAGE_INFO_CODEC: Codec<GrowthStageInfo> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("type").forGetter {
                    when (it) {
                        is Known -> "known"
                        is Estimated -> "estimated"
                    }
                },

                Codec.INT.optionalFieldOf("stage").forGetter {
                    Optional.ofNullable((it as? Known)?.stage)
                },

                Codec.INT.fieldOf("min").forGetter {
                    (it as? Estimated)?.range?.first ?: 0
                },

                Codec.INT.fieldOf("max").forGetter {
                    (it as? Estimated)?.range?.last ?: 0
                }
            ).apply(instance) { type, stageOpt, min, max ->
                when (type) {
                    "known" -> Known(stageOpt.orElse(0))
                    else -> Estimated(min..max)
                }
            }
        }
    }

    val GREENHOUSE_ELEMENT_INSTANCE_CODEC: Codec<GreenhouseElementInstance> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter { it.elementId },
                GREENHOUSE_SLOT_CODEC.optionalFieldOf("slot")
                    .forGetter { Optional.ofNullable(it.slot) },

                Codec.INT.optionalFieldOf("waterLevel")
                    .forGetter { Optional.ofNullable(it.waterLevel) },

                GROWTH_STAGE_INFO_CODEC.optionalFieldOf("growthStage")
                    .forGetter { Optional.ofNullable(it.growthStage) },

                Codec.LONG.optionalFieldOf("age")
                    .forGetter { Optional.ofNullable(it.age) }
            ).apply(instance) { id, slot, waterOpt, growthOpt, ageOpt ->
                GreenhouseElementInstance(
                    elementId = id,
                    slot = slot.orElse(null),
                    waterLevel = waterOpt.orElse(null),
                    growthStage = growthOpt.orElse(null),
                    age = ageOpt.orElse(null),
                    cropDef = CropRegistry.get(id) ?: throw IllegalStateException("Unable to find crop for id $id")
                )
            }
        }
    }

    val GRID_STATE_CODEC: Codec<GridState> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.optionalFieldOf("lastUpdateTimestamp").forGetter {
                    Optional.ofNullable(it.lastUpdateTimestamp?.toEpochMilli())
                },
                Codec.STRING.optionalFieldOf("assigned_layout_id").forGetter {
                    Optional.ofNullable(it.assignedLayout?.id)
                }


            ).apply(instance) { lastUpdate, assignedLayout ->
                GridState(
                    lastUpdateTimestamp = lastUpdate.orElse(null)?.let { Instant.ofEpochMilli(it) },
                    assignedLayout = GreenhouseData.presetGrids.find { it.id == assignedLayout.orElse(null) }
                )
            }
        }
    }

    val GREENHOUSE_SLOT_CODEC: Codec<LayoutSlot> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("x").forGetter { it.x },
                Codec.INT.fieldOf("y").forGetter { it.y },

                BlockState.CODEC
                    .optionalFieldOf("block")
                    .forGetter { Optional.ofNullable(it.placedBlock) },
                Codec.INT.optionalFieldOf("slot_marking")
                    .forGetter { Optional.ofNullable(it.slotMark?.ordinal) }
            ).apply(instance) { x, y, block, marking ->
                LayoutSlot(
                    x,
                    y,
                    block.orElse(null),
                    marking.orElse(null)?.let { LayoutSlot.Marking.entries[it] }
                )
            }
        }
    }

    val GREENHOUSE_GRID_CODEC: Codec<GreenhouseGrid> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                GRID_STATE_CODEC
                    .fieldOf("state")
                    .forGetter { it.state },
                GREENHOUSE_LAYOUT_CODEC
                    .fieldOf("layout")
                    .forGetter { it.layout }

            ).apply(instance) { state, layout ->
                val grid = GreenhouseGrid(state, layout)

                grid
            }
        }
    }

    val MISC_GREENHOUSE_INFO_CODEC: Codec<MiscGreenhouseInfo> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.optionalFieldOf("next_tick")
                    .forGetter { Optional.ofNullable(it.nextTickTime?.toEpochMilli()) },
                Codec.INT.optionalFieldOf("crop_growth_value")
                    .forGetter { Optional.ofNullable(it.cropGrowthValue) },
                    Codec.INT.optionalFieldOf("crop_speed_upgrade")
                            .forGetter { Optional.ofNullable(it.cropSpeedUpgradeValue) },
                Codec.INT.optionalFieldOf("crop_yield_upgrade")
                        .forGetter { Optional.ofNullable(it.cropYieldUpgradeValue) },
                    Codec.BOOL.fieldOf("ignore_warnings")
                        .forGetter { it.shouldIgnoreWarning }


            ).apply(instance) { tick, cropGrowth, cropSpeed, cropYield, ignoreWarnings ->
                MiscGreenhouseInfo(
                    nextTickTime = tick.orElse(null)?.let { Instant.ofEpochMilli(it) } ,
                    cropGrowthValue = cropGrowth.orElse(null),
                    cropSpeedUpgradeValue = cropSpeed.orElse(null),
                    cropYieldUpgradeValue = cropYield.orElse(null),
                    ignoreWarnings
                    )
            }
        }


    }
}