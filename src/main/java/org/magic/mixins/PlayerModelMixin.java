package org.magic.mixins;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sizes the head for Smol People. The whole model is already scaled by the body factor, so the
 * head part is scaled by what is left to reach its own factor, about the neck it turns on. The
 * hat layer is a child of the head and follows it.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {

    protected PlayerModelMixin(ModelPart root) {
        super(root);
    }

    // the model is shared by every player drawn, so the head is set for each state, 1 when off
    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL")
    )
    private void scaleSmolHead(AvatarRenderState state, CallbackInfo ci) {
        WrappedEntityRenderState wrapped = (WrappedEntityRenderState) state;
        float headOverBody = wrapped.magicaddons$smolHead() / wrapped.magicaddons$smolBody();

        this.head.xScale = headOverBody;
        this.head.yScale = headOverBody;
        this.head.zScale = headOverBody;
    }
}
