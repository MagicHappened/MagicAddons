package org.magic.mixins;

import org.magic.magicaddons.util.ErrorReporter;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? if >=26.2 {
/*import net.minecraft.client.renderer.extract.LevelExtractor;
*///?} else {
import net.minecraft.client.renderer.LevelRenderer;
//?}
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >=26.2 {
/*@Mixin(LevelExtractor.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
public class LevelExtractorMixin {

    @WrapOperation(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    //? if >=26.2 {
                    /*target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
                    *///?} else {
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
                    //?}
    )
    private EntityRenderState wrapExtractEntity(
            //? if >=26.2 {
            /*LevelExtractor instance,
            *///?} else {
            LevelRenderer instance,
            //?}
            Entity entity,
            float partialTickTime,
            Operation<EntityRenderState> original
    ) {
        EntityRenderState state = original.call(instance, entity, partialTickTime);

        try {
            if (LayoutRenderState.INSTANCE.getBadStandsUUID().contains(entity.getUUID())){
                ((WrappedEntityRenderState)state).magicaddons$setWrappedEntity(true);
                ((WrappedEntityRenderState)state).magicaddons$setWrappedEntityTintColor(LayoutRenderState.RED_TINT);
            }
        } catch (Throwable error) {
            ErrorReporter.INSTANCE.report("the stand tinting", error);
        }
        return state;
    }
}
