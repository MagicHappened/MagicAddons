package org.magic.magicaddons.data.greenhouse

import com.mojang.datafixers.util.Either
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
                    // older files carry "unnamed" as the name the mod itself wrote, which is no name
                    name = nameOpt.orElse(null)?.takeUnless { it == "unnamed" },
                    slots = slots,
                    elementInstances = elements.toMutableList()
                )
            }
        }
    }

    val MASTER_LAYOUT_CODEC: Codec<MasterLayout> by lazy {
        val master = RecordCodecBuilder.create<MasterLayout> { instance ->
            instance.group(
                Codec.STRING.fieldOf("preset_id").forGetter { it.id },
                Codec.STRING.optionalFieldOf("preset_name").forGetter { Optional.ofNullable(it.name) },
                GREENHOUSE_LAYOUT_CODEC.listOf().fieldOf("plots").forGetter { it.plots }
            ).apply(instance) { id, nameOpt, plots ->
                MasterLayout(id, nameOpt.orElse(null), plots.toMutableList())
            }
        }

        // older files hold a bare plot where a preset is; it comes back as a preset of one plot
        Codec.either(master, GREENHOUSE_LAYOUT_CODEC).xmap(
            { either -> either.map({ it }, { MasterLayout.of(it) }) },
            { Either.left(it) }
        )
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
                    .forGetter { Optional.ofNullable(it.age) },

                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("readings")
                    .forGetter { Optional.of(it.readings.toMap()) },

                Codec.INT.optionalFieldOf("first_seen_stage")
                    .forGetter { Optional.ofNullable(it.firstSeenStage) }
            ).apply(instance) { id, slot, waterOpt, growthOpt, ageOpt, readingsOpt, firstSeenOpt ->
                GreenhouseElementInstance(
                    elementId = id,
                    slot = slot.orElse(null),
                    waterLevel = waterOpt.orElse(null),
                    growthStage = growthOpt.orElse(null),
                    age = ageOpt.orElse(null),
                    readings = readingsOpt.orElse(emptyMap()).toMutableMap(),
                    cropDef = CropRegistry.get(id) ?: throw IllegalStateException("Unable to find crop for id $id")
                ).also { plant ->
                    plant.firstSeenStage = firstSeenOpt.orElse(null)
                }
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
                    assignedLayout = GreenhouseData.allPlots().find { it.id == assignedLayout.orElse(null) }
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
                Codec.INT.optionalFieldOf("greenhouse_speed_attribute")
                        .forGetter { Optional.ofNullable(it.greenhouseSpeedAttribute) },
                    Codec.BOOL.fieldOf("ignore_warnings")
                        .forGetter { it.shouldIgnoreWarning }


            ).apply(instance) { tick, cropGrowth, cropSpeed, cropYield, speedAttribute, ignoreWarnings ->
                MiscGreenhouseInfo(
                    nextTickTime = tick.orElse(null)?.let { Instant.ofEpochMilli(it) } ,
                    cropGrowthValue = cropGrowth.orElse(null),
                    cropSpeedUpgradeValue = cropSpeed.orElse(null),
                    cropYieldUpgradeValue = cropYield.orElse(null),
                    greenhouseSpeedAttribute = speedAttribute.orElse(null),
                    ignoreWarnings
                    )
            }
        }


    }
}