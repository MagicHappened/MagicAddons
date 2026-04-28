package org.magic.mixins;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.magic.magicaddons.util.ChatUtils;
import org.magic.misc.BlockEventBufferAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientLevel.class)
public class ClientLevelMixin implements BlockEventBufferAccess {

    @Unique
    public final Map<BlockPos, BlockState> pendingPlaces = new HashMap<>();

    @Unique
    public final Map<BlockPos, BlockState> pendingBreaks = new HashMap<>();

    @Override
    public Map<BlockPos, BlockState> magicaddons$getPendingPlaces() {
        return pendingPlaces;
    }

    @Override
    public Map<BlockPos, BlockState> magicaddons$getPendingBreaks() {
        return pendingBreaks;
    }

    @Inject(method = "gameEvent", at = @At("TAIL"))
    private void onGameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, CallbackInfo ci) {
        //if (!(context.sourceEntity() instanceof LocalPlayer)) return;

        BlockPos pos = BlockPos.containing(vec3);
        if (holder == GameEvent.BLOCK_PLACE) {
            pendingPlaces.put(pos, context.affectedState());
        }
        else if (holder == GameEvent.BLOCK_DESTROY) {

            pendingBreaks.put(pos, context.affectedState());
        }
    }


}
