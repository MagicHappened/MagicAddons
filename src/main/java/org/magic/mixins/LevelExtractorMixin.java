package org.magic.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelExtractorMixin {

    @WrapOperation(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
    )
    private EntityRenderState wrapExtractEntity(
            LevelRenderer instance,
            Entity entity,
            float partialTickTime,
            Operation<EntityRenderState> original
    ) {
        EntityRenderState state = original.call(instance, entity, partialTickTime);

        if (LayoutRenderState.INSTANCE.getBadStandsUUID().contains(entity.getUUID())){
            ((WrappedEntityRenderState)state).magicaddons$setWrappedEntity(true);
            ((WrappedEntityRenderState)state).magicaddons$setWrappedEntityTintColor(LayoutRenderState.RED_TINT);
        }
        return state;
    }
}
