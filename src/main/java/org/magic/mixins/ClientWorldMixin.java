package org.magic.mixins;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.EntityList;
import org.magic.magicaddons.util.TablistUtils;
import org.magic.magicaddons.util.world.WorldEntities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Final
    @Shadow
    EntityList entityList;


    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void onWorldTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TablistUtils.updateTabList();
        WorldEntities.onWorldTick(entityList);
    }


}
