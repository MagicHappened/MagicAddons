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
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {

    /**
     * A ghost head is drawn cutout unless plants are see-through: translucent models are drawn a
     * pass each, cutout models batch by skin.
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
            Identifier texture = info.playerSkin().body().texturePath();
            return GreenhousePresets.plantAlpha() < OPAQUE
                    ? RenderTypes.entityTranslucent(texture)
                    : RenderTypes.entityCutout(texture);
        }
        return original.call(info);
    }

    private static final int OPAQUE = 0xFF;

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

        // a ghost head is faded by the plant transparency; a stand in the way is tinted red
        boolean ghost = state instanceof WrappedEntityRenderState wrapped && wrapped.magicaddons$headOutlineColor() != 0;
        if (ghost && GreenhousePresets.plantAlpha() < OPAQUE) {
            submitSkullWithTint(
                    animationValue,
                    poseStack,
                    submitNodeCollector,
                    lightCoords,
                    model,
                    renderType,
                    ARGB.color(GreenhousePresets.plantAlpha(), 0xFFFFFF),
                    outlineColor,
                    breakProgress
            );
            return;
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
