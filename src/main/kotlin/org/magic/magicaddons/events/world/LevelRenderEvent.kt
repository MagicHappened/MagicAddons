package org.magic.magicaddons.events.world

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.state.LevelRenderState

class LevelRenderEvent (
    val poseStack: PoseStack,
    val nodeStorage: SubmitNodeStorage,
    val levelRenderState: LevelRenderState
){
}