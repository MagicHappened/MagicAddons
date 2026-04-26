package org.magic.mixins;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent;
import org.magic.magicaddons.events.interact.OnBlockPlacedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "gameEvent", at = @At("TAIL"))
    private void onGameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, CallbackInfo ci) {
        if (!(context.sourceEntity() instanceof LocalPlayer local)) return;
        if (holder == GameEvent.BLOCK_PLACE) {
            BlockPos pos = BlockPos.containing(vec3);
            OnBlockPlacedEvent event = new OnBlockPlacedEvent(pos, local, context.affectedState());
            EventBus.post(event);

        } else if (holder == GameEvent.BLOCK_DESTROY) {
            BlockPos pos = BlockPos.containing(vec3);
            OnBlockDestroyedEvent event = new OnBlockDestroyedEvent(pos, local, context.affectedState());
            EventBus.post(event);
        }
    }

}
