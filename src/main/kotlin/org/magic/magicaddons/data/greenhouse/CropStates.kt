package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.DoublePlantBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

object CropStates {

    fun toFunctionString(state: BlockState): String? {
        return when (state.block) {
            Blocks.WHEAT ->
                "wheatState(${state.getValue(CropBlock.AGE)})"

            Blocks.MELON_STEM ->
                "melonStemState(${state.getValue(StemBlock.AGE)})"

            Blocks.PUMPKIN_STEM ->
                "pumpkinStemState(${state.getValue(StemBlock.AGE)})"

            Blocks.CARROTS ->
                "carrotState(${state.getValue(CropBlock.AGE)})"

            Blocks.POTATOES ->
                "potatoesState(${state.getValue(CropBlock.AGE)})"

            Blocks.NETHER_WART ->
                "netherwartState(${state.getValue(NetherWartBlock.AGE)})"

            Blocks.SUGAR_CANE ->
                "sugarcaneState()"

            Blocks.RED_MUSHROOM ->
                "redMushroomState()"

            Blocks.BROWN_MUSHROOM ->
                "brownMushroomState()"

            Blocks.CACTUS ->
                "cactusState()"

            Blocks.SUNFLOWER ->
                "sunflowerState()"

            Blocks.SHORT_GRASS ->
                "shortGrassState()"

            Blocks.ROSE_BUSH ->
                "roseBushState()"

            Blocks.DEAD_BUSH ->
                "deadBushState()"

            else -> null
        }
    }



    fun wheatState(age: Int): BlockState =
        Blocks.WHEAT.defaultBlockState()
            .setValue(CropBlock.AGE, age)

    fun melonStemState(age: Int): BlockState =
        Blocks.MELON_STEM.defaultBlockState()
            .setValue(StemBlock.AGE, age)

    fun pumpkinStemState(age: Int): BlockState =
        Blocks.PUMPKIN_STEM.defaultBlockState()
            .setValue(StemBlock.AGE, age)

    fun carrotState(age: Int): BlockState =
        Blocks.CARROTS.defaultBlockState()
            .setValue(CropBlock.AGE, age)

    fun potatoesState(age: Int): BlockState =
        Blocks.POTATOES.defaultBlockState()
            .setValue(CropBlock.AGE, age)

    fun netherwartState(age: Int): BlockState =
        Blocks.NETHER_WART.defaultBlockState()
            .setValue(NetherWartBlock.AGE, age)

    fun sugarcaneState(): BlockState = Blocks.SUGAR_CANE.defaultBlockState()
    fun redMushroomState(): BlockState = Blocks.RED_MUSHROOM.defaultBlockState()
    fun brownMushroomState(): BlockState = Blocks.BROWN_MUSHROOM.defaultBlockState()
    fun cactusState(): BlockState = Blocks.CACTUS.defaultBlockState()
    fun sunflowerState(): BlockState = Blocks.SUNFLOWER.defaultBlockState()
    fun shortGrassState(): BlockState = Blocks.SHORT_GRASS.defaultBlockState()
    fun roseBushState(half: DoubleBlockHalf): BlockState =
        Blocks.ROSE_BUSH.defaultBlockState()
            .setValue(DoublePlantBlock.HALF, half)
    fun deadBushState(): BlockState = Blocks.DEAD_BUSH.defaultBlockState()
}