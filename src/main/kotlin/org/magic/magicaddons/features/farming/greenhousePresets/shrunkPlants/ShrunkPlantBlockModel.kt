package org.magic.magicaddons.features.farming.greenhousePresets.shrunkPlants

import java.util.function.Predicate
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState

class ShrunkPlantBlockModel(wrapped: BlockStateModel) : WrapperBlockStateModel(wrapped) {

    override fun emitQuads(
        emitter: QuadEmitter,
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        random: RandomSource,
        cullTest: Predicate<Direction?>
    ) {
        val blockState = ShorterCaneCrops.hiddenBlockAt(pos)

        if (blockState == null) {
            super.emitQuads(emitter, level, pos, state, random, cullTest)
            return
        }
        if (ShorterCaneCrops.isDrawingAir(blockState)) return

        val replacementModel = Minecraft.getInstance().modelManager.blockStateModelSet.get(blockState)

        if (replacementModel is ShrunkPlantBlockModel) {
            replacementModel.emitWrappedModelQuads(emitter, level, pos, blockState, random, cullTest)
        } else {
            replacementModel.emitQuads(emitter, level, pos, blockState, random, cullTest)
        }
    }

    private fun emitWrappedModelQuads(
        emitter: QuadEmitter,
        blockAndTintGetter: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        random: RandomSource,
        cullTest: Predicate<Direction?>
    ) {
        super.emitQuads(emitter, blockAndTintGetter, pos, state, random, cullTest)
    }

    override fun createGeometryKey(level: BlockAndTintGetter, pos: BlockPos, state: BlockState, random: RandomSource): Any? {
        if (ShorterCaneCrops.hiddenBlockAt(pos) != null) return null

        return super.createGeometryKey(level, pos, state, random)
    }
}
