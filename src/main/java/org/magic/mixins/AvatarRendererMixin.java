package org.magic.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.magic.magicaddons.features.misc.SmolPeople;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Unique
    private static final float BODY_UNITS = 24f;
    @Unique
    private static final float HEAD_UNITS = 8f;

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void rememberSmolFactors(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        boolean smol = SmolPeople.applies(entity);
        float body = smol ? SmolPeople.bodyFactor() : 1f;
        float head = smol ? SmolPeople.headFactor() : 1f;

        ((WrappedEntityRenderState) state).magicaddons$setSmolFactors(body, head);

        if (!smol) return;

        float heightFactor = (BODY_UNITS * body + HEAD_UNITS * head) / (BODY_UNITS + HEAD_UNITS);
        if (state.nameTagAttachment != null) {
            state.nameTagAttachment = state.nameTagAttachment.multiply(1.0, heightFactor, 1.0);
        }
        state.shadowRadius *= body;
    }

    @Inject(
            method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("TAIL")
    )
    private void scaleSmolBody(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        float body = ((WrappedEntityRenderState) state).magicaddons$smolBody();
        poseStack.scale(body, body, body);
    }
}
