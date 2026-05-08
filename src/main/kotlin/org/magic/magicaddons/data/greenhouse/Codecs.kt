package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid.GridState
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo.Estimated
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo.Known
import java.util.*
import kotlin.jvm.optionals.getOrNull

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
                    elementInstances = elements
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
                    .forGetter { Optional.ofNullable(it.growthStage) }
            ).apply(instance) { id, slot, waterOpt, growthOpt ->
                GreenhouseElementInstance(
                    elementId = id,
                    slot = slot.orElse(null),
                    waterLevel = waterOpt.orElse(null),
                    growthStage = growthOpt.orElse(null)
                )
            }
        }
    }

    val GRID_STATE_CODEC: Codec<GridState> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.fieldOf("lastUpdateTimestamp").forGetter {
                    it.lastUpdateTimestamp
                },
                Codec.BOOL.fieldOf("needsUpdate").forGetter {
                    it.needsUpdate
                },
                Codec.BOOL.fieldOf("initialized").forGetter {
                    it.initialized
                }


            ).apply(instance) { lastUpdate, needsUpdate, initialized ->
                GridState(lastUpdate, needsUpdate, initialized)
            }
        }
    }

    val GREENHOUSE_SLOT_CODEC: Codec<GreenhouseSlot> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("x").forGetter { it.x },
                Codec.INT.fieldOf("y").forGetter { it.y },

                BlockState.CODEC
                    .optionalFieldOf("block")
                    .forGetter { Optional.ofNullable(it.placedBlock) },
            ).apply(instance) { x, y, block ->
                GreenhouseSlot(x, y, block.getOrNull())
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
}