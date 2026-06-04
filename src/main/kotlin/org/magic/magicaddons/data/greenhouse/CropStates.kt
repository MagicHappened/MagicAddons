package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.state.BlockState

object CropStates {
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
            .setValue(CropBlock.AGE, age)

    fun sugarcaneState(age: Int): BlockState =
        Blocks.SUGAR_CANE.defaultBlockState()
            .setValue(CropBlock.AGE, age)

    fun redMushroomState(): BlockState = Blocks.RED_MUSHROOM.defaultBlockState()
    fun brownMushroomState(): BlockState = Blocks.BROWN_MUSHROOM.defaultBlockState()
    fun cactusState(): BlockState = Blocks.CACTUS.defaultBlockState()
    fun sunflowerState(): BlockState = Blocks.SUNFLOWER.defaultBlockState()
    fun shortGrassState(): BlockState = Blocks.SHORT_GRASS.defaultBlockState()
    fun roseBushState(): BlockState = Blocks.ROSE_BUSH.defaultBlockState()
    fun deadBushState(): BlockState = Blocks.DEAD_BUSH.defaultBlockState()
}