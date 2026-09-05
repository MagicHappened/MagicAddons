package org.magic.mixins;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
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

    /**
     * A ghost head is drawn cutout rather than translucent. A translucent model is sorted and drawn
     * in a pass of its own, which with a plan full of heads meant a pass a head; cutout models
     * batch by skin. The skins are opaque, so nothing looks different.
     */
    @WrapOperation(
            method = "resolveSkullRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;renderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType cutoutForGhosts(
            PlayerSkinRenderCache.RenderInfo info,
            Operation<RenderType> original,
            @Local(argsOnly = true) LivingEntityRenderState state
    ) {
        if (state instanceof WrappedEntityRenderState wrapped && wrapped.magicaddons$headOutlineColor() != 0) {
            return RenderTypes.entityCutout(info.playerSkin().body().texturePath());
        }
        return original.call(info);
    }

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
        // an outline meant for the head rather than the whole body arrives here instead of on
        // the render state, since a colour there outlines the whole body as well
        if (state instanceof WrappedEntityRenderState wrapped
                && wrapped.magicaddons$headOutlineColor() != 0) {
            outlineColor = wrapped.magicaddons$headOutlineColor();
        }

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

    /**
     * The tinted stand in for vanilla's skull submission. Neither pushes nor pops, as vanilla does
     * not: popping here took an entry nothing had put there.
     */
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
    }
}
