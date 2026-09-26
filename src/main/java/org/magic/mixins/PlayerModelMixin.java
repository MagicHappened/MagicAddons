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

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {

    protected PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL")
    )
    private void scaleSmolHead(AvatarRenderState state, CallbackInfo ci) {
        WrappedEntityRenderState wrapped = (WrappedEntityRenderState) state;
        float headScale = wrapped.magicaddons$smolHead() / wrapped.magicaddons$smolBody();

        this.head.xScale = headScale;
        this.head.yScale = headScale;
        this.head.zScale = headScale;
    }
}
