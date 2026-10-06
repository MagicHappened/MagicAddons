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
                    .forGetter { plant -> plant.presetAlternatives.map { it.elementId } },

                MUTATION_TRACKING_CODEC.optionalFieldOf("mutation_tracking")
                    .forGetter { Optional.of(MutationTrackingRecord.of(it)) }
            ).apply(instance) { id, slot, waterOpt, growthOpt, appearedAtOpt, readingsOpt, firstSeenOpt, placed, waterExact, charge, chargeKnown, alternativeIds, mutationTracking ->
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
                    mutationTracking.ifPresent { it.applyTo(plant) }
                }
            }
        }
    }

    private val DIAGNOSIS_READING_CODEC: Codec<DiagnosisReading> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.INT.fieldOf("times_mutated").forGetter { it.timesMutated },
            Codec.INT.optionalFieldOf("combined").forGetter { Optional.ofNullable(it.combinedRemaining) },
            Codec.LONG.fieldOf("read_at").forGetter { it.readAt },
            Codec.INT.optionalFieldOf("seen_since", 0).forGetter { it.spawnsSeenSince },
            Codec.BOOL.optionalFieldOf("exact", false).forGetter { it.isStillExact }
        ).apply(instance) { timesMutated, combined, readAt, seenSince, isStillExact ->
            DiagnosisReading(timesMutated, combined.orElse(null), readAt, seenSince, isStillExact)
        }
    }

    class MutationTrackingRecord(
        val spawned: Int,
        val isMinimum: Boolean,
        val seenSpawned: Int,
        val isTracked: Boolean,
        val isCountedFromStart: Boolean,
        val hasUncertainCredit: Boolean,
        val decayAttemptAt: Long?,
        val lastReading: DiagnosisReading?
    ) {
        fun applyTo(plant: Plant) {
            plant.mutationsSpawned = spawned
            plant.mutationsSpawnedIsMinimum = isMinimum
            plant.seenSpawnsHelped = seenSpawned
            plant.isMutationCountTracked = isTracked
            plant.isCountedFromStart = isCountedFromStart
            plant.hasUncertainCredit = hasUncertainCredit
            plant.decayAttemptAt = decayAttemptAt
            plant.lastDiagnosisReading = lastReading
        }

        companion object {
            fun of(plant: Plant): MutationTrackingRecord = MutationTrackingRecord(
                plant.mutationsSpawned,
                plant.mutationsSpawnedIsMinimum,
                plant.seenSpawnsHelped,
                plant.isMutationCountTracked,
                plant.isCountedFromStart,
                plant.hasUncertainCredit,
                plant.decayAttemptAt,
                plant.lastDiagnosisReading
            )
        }
    }

    private val MUTATION_TRACKING_CODEC: Codec<MutationTrackingRecord> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.INT.optionalFieldOf("spawned", 0).forGetter { it.spawned },
            Codec.BOOL.optionalFieldOf("is_minimum", true).forGetter { it.isMinimum },
            Codec.INT.optionalFieldOf("seen_spawned").forGetter { Optional.of(it.seenSpawned) },
            Codec.INT.optionalFieldOf("stationary_spawned").forGetter { Optional.empty() },
            Codec.BOOL.optionalFieldOf("tracked", false).forGetter { it.isTracked },
            Codec.BOOL.optionalFieldOf("counted_from_start", false).forGetter { it.isCountedFromStart },
            Codec.BOOL.optionalFieldOf("uncertain_credit", false).forGetter { it.hasUncertainCredit },
            Codec.LONG.optionalFieldOf("decay_attempt_at").forGetter { Optional.ofNullable(it.decayAttemptAt) },
            DIAGNOSIS_READING_CODEC.optionalFieldOf("last_reading").forGetter { Optional.ofNullable(it.lastReading) }
        ).apply(instance) { spawned, isMinimum, seenSpawned, stationarySpawned, isTracked, isCountedFromStart, hasUncertainCredit, decayAttemptAt, lastReading ->
            MutationTrackingRecord(spawned, isMinimum, seenSpawned.or { stationarySpawned }.orElse(0), isTracked, isCountedFromStart, hasUncertainCredit, decayAttemptAt.orElse(null), lastReading.orElse(null))
        }
    }

    private fun slotKeyOf(slot: Pair<Int, Int>): String = "${slot.first},${slot.second}"

    private fun slotOf(key: String): Pair<Int, Int>? {
        val (x, y) = key.split(',').mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return null
        return x to y
    }

    private val BLIND_SPAWNS_CODEC: Codec<BlindSpawns> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.INT.fieldOf("x").forGetter { it.x },
            Codec.INT.fieldOf("y").forGetter { it.y },
            Codec.STRING.fieldOf("crop").forGetter { it.cropId },
            Codec.STRING.listOf().fieldOf("contributors").forGetter { blind -> blind.contributorSlots.map(::slotKeyOf) },
            Codec.LONG.fieldOf("created_at").forGetter { it.createdAt },
            Codec.BOOL.optionalFieldOf("before_tracking", false).forGetter { it.isBeforeTracking },
            Codec.INT.optionalFieldOf("at_least", 0).forGetter { it.atLeast },
            Codec.BOOL.optionalFieldOf("exact", false).forGetter { it.isExact }
        ).apply(instance) { x, y, crop, contributors, createdAt, isBeforeTracking, atLeast, isExact ->
            BlindSpawns(x, y, crop, contributors.mapNotNull(::slotOf).toSet(), createdAt, isBeforeTracking, atLeast, isExact)
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
                Codec.INT.optionalFieldOf("plan_turns", 0).forGetter { it.planTurns },
                Codec.BOOL.optionalFieldOf("no_rotate_assigned_layout", false).forGetter { it.noRotateAssignedLayout },
                Codec.INT.optionalFieldOf("ticks_since_last_scan", 0).forGetter { it.ticksSinceLastScan },
                Codec.DOUBLE.listOf().optionalFieldOf("chorus_loss_chance_by_tick").forGetter {
                    Optional.ofNullable(it.chorusLossChanceByTick?.toList())
                },
                BLIND_SPAWNS_CODEC.listOf().optionalFieldOf("blind_spawns", emptyList()).forGetter { it.blindSpawns }
            ).apply(instance) { lastUpdate, assignedLayout, planTurns, noRotateAssignedLayout, ticksSinceLastScan, chorusLossChances, blindSpawns ->
                GridState(
                    lastScanTime = lastUpdate.orElse(null)?.let { Instant.ofEpochMilli(it) },
                    planTurns = planTurns,
                    noRotateAssignedLayout = noRotateAssignedLayout,
                    ticksSinceLastScan = ticksSinceLastScan
                ).also {
                    it.assignedLayoutId = assignedLayout.orElse(null)
                    it.chorusLossChanceByTick = chorusLossChances.orElse(null)?.toDoubleArray()
                    it.blindSpawns += blindSpawns
                }
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
                        .forGetter { it.cropsWithoutInfo.sorted() },
                Codec.DOUBLE.optionalFieldOf("last_seen_mutation_chance_percent")
                        .forGetter { Optional.ofNullable(it.lastSeenMutationChancePercent) }
            ).apply(instance) { tick, cropGrowth, cropSpeed, cropYield, speedAttribute, cropsWithoutInfo, mutationChancePercent ->
                MiscGreenhouseInfo(
                    nextTickTime = tick.orElse(null)?.let { Instant.ofEpochMilli(it) } ,
                    cropGrowthValue = cropGrowth.orElse(null),
                    cropSpeedUpgradeValue = cropSpeed.orElse(null),
                    cropYieldUpgradeValue = cropYield.orElse(null),
                    greenhouseSpeedAttribute = speedAttribute.orElse(null),
                    lastSeenMutationChancePercent = mutationChancePercent.orElse(null),
                    cropsWithoutInfo = cropsWithoutInfo.toMutableSet()
                    )
            }
        }


    }
}