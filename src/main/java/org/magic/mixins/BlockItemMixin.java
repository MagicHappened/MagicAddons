package org.magic.mixins;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.magic.magicaddons.util.ChatUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "place", at = @At("TAIL"))
    private void onBlockPlace(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<InteractionResult> cir){
        if (!cir.getReturnValue().consumesAction()) return;
        var text2 = getBlock().getName().getString();
        var text = blockPlaceContext.getItemInHand();
        ChatUtils.INSTANCE.sendWithPrefix("placed: "+ text " with");
    }
}
