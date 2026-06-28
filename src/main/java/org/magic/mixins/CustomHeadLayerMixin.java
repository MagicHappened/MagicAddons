package org.magic.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {

    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private void onSubmitSkull(
            float animationValue,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            SkullModelBase model,
            RenderType renderType,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay breakProgress,
            Operation<Void> original,
            @Local(argsOnly = true, name = "state") LivingEntityRenderState state
            ){
        if (state instanceof WrappedEntityRenderState fakeState
                && fakeState.magicaddons$isWrappedEntity()) {

            int tintColor = fakeState.magicaddons$entityTintColor();

            submitSkullWithTint(
                    animationValue,
                    poseStack,
                    submitNodeCollector,
                    lightCoords,
                    model,
                    renderType,
                    tintColor,
                    outlineColor,
                    breakProgress
            );
            return;
        }

        original.call(
        animationValue,
        poseStack,
        submitNodeCollector,
        lightCoords,
        model,
        renderType,
        outlineColor,
        breakProgress
        );
    }

    @Unique
    private static void submitSkullWithTint(
            float animationValue,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int light,
            SkullModelBase model,
            RenderType renderType,
            int tintColor,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay overlay
    ) {
        SkullModelBase.State state = new SkullModelBase.State();
        state.animationPos = animationValue;

        submitNodeCollector.submitModel(
                model,
                state,
                poseStack,
                renderType,
                light,
                OverlayTexture.NO_OVERLAY,
                tintColor,
                null,
                outlineColor,
                overlay
        );

        poseStack.popPose();
    }
}
