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
import org.magic.misc.FakeEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {

    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private void onSubmitSkull(
            Direction direction,
            float yaw, float anim,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int light,
            SkullModelBase skullModelBase,
            RenderType renderType,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            Operation<Void> original,
            @Local(argsOnly = true) LivingEntityRenderState state
            ){
        if (state instanceof FakeEntityState fakeState
                && fakeState.magicaddons$isFakeEntity()) {

            int tintColor = fakeState.magicaddons$fakeEntityTintColor();

            submitSkullWithTint(
                    direction,
                    yaw,
                    anim,
                    poseStack,
                    submitNodeCollector,
                    light,
                    skullModelBase,
                    renderType,
                    tintColor,
                    outlineColor,
                    crumblingOverlay
            );
            return;
        }

        original.call(
                direction,
                yaw,
                anim,
                poseStack,
                submitNodeCollector,
                light,
                skullModelBase,
                renderType,
                outlineColor,
                crumblingOverlay
        );
    }

    @Unique
    private static void submitSkullWithTint(
            Direction direction,
            float yaw,
            float anim,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            SkullModelBase model,
            RenderType renderType,
            int tintColor,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay overlay
    ) {
        poseStack.pushPose();

        if (direction == null) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            poseStack.translate(0.5F - (float)direction.getStepX() * 0.25F, 0.25F, 0.5F - (float)direction.getStepZ() * 0.25F);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);

        SkullModelBase.State state = new SkullModelBase.State();
        state.animationPos = anim;
        state.yRot = yaw;

        collector.submitModel(
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
