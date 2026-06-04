package org.magic.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {
    @WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private void magicaddons$overrideSkullRenderType(
            Model<?> instance,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int i, int j, int k,
            Operation<Void> original) {
        k = 0x40FFFFFF;
        original.call(instance, poseStack, vertexConsumer, i, j, k);
    }
}
