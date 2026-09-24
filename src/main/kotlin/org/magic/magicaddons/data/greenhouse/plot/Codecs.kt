package org.magic.magicaddons.data.greenhouse.plot

import com.mojang.datafixers.util.Either
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid.GridState
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.time.Instant
import java.util.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.crops.PlantStage.Estimated
import org.magic.magicaddons.data.greenhouse.crops.PlantStage.Known

object Codecs {
    val GREENHOUSE_LAYOUT_CODEC: Codec<PlotLayout> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("layout_id").forGetter { it.id },

                Codec.STRING
                    .optionalFieldOf("layout_name")
                    .forGetter { Optional.ofNullable(it.name) },

                GREENHOUSE_SLOT_CODEC.listOf()
                    .fieldOf("slots")
                    .forGetter { it.slots },

                GREENHOUSE_PLANT_CODEC.listOf()
                    .fieldOf("element_instances")
                    .forGetter { it.plants },

                Codec.BOOL.optionalFieldOf("explicit_air").forGetter { Optional.of(true) }
            ).apply(instance) { id, nameOpt, slots, elements, explicitAir ->
                if (!explicitAir.orElse(false)) {
                    slots.forEach { slot -> if (slot.soil == Blocks.AIR) slot.soil = null }
                }
                PlotLayout(
                    id = id,
                    name = nameOpt.orElse(null),
                    slots = slots,
                    plants = elements.toMutableList()
                )
            }
        }
    }

    val MASTER_LAYOUT_CODEC: Codec<GreenhouseLayout> by lazy {
        val master = RecordCodecBuilder.create<GreenhouseLayout> { instance ->
            instance.group(
                Codec.STRING.fieldOf("preset_id").forGetter { it.id },
                Codec.STRING.optionalFieldOf("preset_name").forGetter { Optional.ofNullable(it.name) },
                GREENHOUSE_LAYOUT_CODEC.listOf().fieldOf("plots").forGetter { it.plots }
            ).apply(instance) { id, nameOpt, plots ->
                GreenhouseLayout(id, nameOpt.orElse(null), plots.toMutableList())
            }
        }

        Codec.either(master, GREENHOUSE_LAYOUT_CODEC).xmap(
            { either -> either.map({ it }, { GreenhouseLayout.create(it) }) },
            { Either.left(it) }
        )
    }


    val GROWTH_STAGE_INFO_CODEC: Codec<PlantStage> by lazy {
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

    val GREENHOUSE_PLANT_CODEC: Codec<Plant> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter { it.elementId },
                GREENHOUSE_SLOT_CODEC.optionalFieldOf("slot")
                    .forGetter { Optional.ofNullable(it.slot) },

                Codec.DOUBLE.optionalFieldOf("waterLevel")
                    .forGetter { Optional.ofNullable(it.waterLevel) },

                GROWTH_STAGE_INFO_CODEC.optionalFieldOf("growthStage")
                    .forGetter { Optional.ofNullable(it.growthStage) },

                Codec.LONG.optionalFieldOf("appeared_at")
                    .forGetter { Optional.ofNullable(it.appearedAt) },

                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("readings")
                    .forGetter { Optional.of(it.readings.toMap()) },

                Codec.INT.optionalFieldOf("first_seen_stage")
                    .forGetter { Optional.ofNullable(it.firstSeenStage) },

                Codec.BOOL.optionalFieldOf("placed", false)
                    .forGetter { it.placed },

                Codec.BOOL.optionalFieldOf("water_exact", false)
                    .forGetter { it.waterExact },

                Codec.INT.optionalFieldOf("charge", 0)
                    .forGetter { it.charge },

                Codec.BOOL.optionalFieldOf("charge_known", false)
                    .forGetter { it.chargeKnown },

                Codec.STRING.listOf().optionalFieldOf("alternatives", emptyList())
                    .forGetter { plant -> plant.presetAlternatives.map { it.elementId } }
            ).apply(instance) { id, slot, waterOpt, growthOpt, appearedAtOpt, readingsOpt, firstSeenOpt, placed, waterExact, charge, chargeKnown, alternativeIds ->
                Plant(
                    elementId = id,
                    slot = slot.orElse(null),
                    waterLevel = waterOpt.orElse(null),
                    growthStage = growthOpt.orElse(null),
                    appearedAt = appearedAtOpt.orElse(null),
                    readings = readingsOpt.orElse(emptyMap()).toMutableMap(),
                    cropDef = CropRegistry.findByIdOrName(id) ?: throw IllegalStateException("Unable to find crop for id $id"),
                    presetAlternatives = alternativeIds.mapNotNull { CropRegistry.findByIdOrName(it) }.toMutableList()
                ).also { plant ->
                    plant.firstSeenStage = firstSeenOpt.orElse(null)
                    plant.placed = placed
                    plant.waterExact = waterExact
                    plant.charge = charge
                    plant.chargeKnown = chargeKnown
                }
            }
        }
    }

    val GRID_STATE_CODEC: Codec<GridState> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.optionalFieldOf("lastUpdateTimestamp").forGetter {
                    Optional.ofNullable(it.lastScanTime?.toEpochMilli())
                },
                Codec.STRING.optionalFieldOf("assigned_layout_id").forGetter {
                    Optional.ofNullable(it.assignedLayout?.id)
                },
                Codec.INT.optionalFieldOf("plan_turns", 0).forGetter { it.planTurns }
            ).apply(instance) { lastUpdate, assignedLayout, planTurns ->
                GridState(
                    lastScanTime = lastUpdate.orElse(null)?.let { Instant.ofEpochMilli(it) },
                    planTurns = planTurns
                ).also { it.assignedLayoutId = assignedLayout.orElse(null) }
            }
        }
    }


    private val SOIL_CODEC: Codec<Block> by lazy { BuiltInRegistries.BLOCK.byNameCodec() }

    val GREENHOUSE_SLOT_CODEC: Codec<LayoutSlot> by lazy {
        RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("x").forGetter { it.x },
                Codec.INT.fieldOf("y").forGetter { it.y },

                SOIL_CODEC
                    .optionalFieldOf("block")
                    .forGetter { Optional.ofNullable(it.soil) },
                Codec.INT.optionalFieldOf("slot_marking")
                    .forGetter { Optional.ofNullable(it.mark?.ordinal) }
            ).apply(instance) { x, y, block, marking ->
                LayoutSlot(
                    x,
                    y,
                    block.orElse(null),
                    marking.orElse(null)?.let { LayoutSlot.Marking.entries.getOrNull(it) }
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

            ).apply(instance) { state, layout -> GreenhouseGrid(state, layout) }
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
                Codec.STRING.listOf().optionalFieldOf("crops_without_info", emptyList())
                        .forGetter { it.cropsWithoutInfo.sorted() }
            ).apply(instance) { tick, cropGrowth, cropSpeed, cropYield, speedAttribute, cropsWithoutInfo ->
                MiscGreenhouseInfo(
                    nextTickTime = tick.orElse(null)?.let { Instant.ofEpochMilli(it) } ,
                    cropGrowthValue = cropGrowth.orElse(null),
                    cropSpeedUpgradeValue = cropSpeed.orElse(null),
                    cropYieldUpgradeValue = cropYield.orElse(null),
                    greenhouseSpeedAttribute = speedAttribute.orElse(null),
                    cropsWithoutInfo = cropsWithoutInfo.toMutableSet()
                    )
            }
        }


    }
}