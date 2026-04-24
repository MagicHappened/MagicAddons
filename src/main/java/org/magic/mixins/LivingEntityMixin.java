package org.magic.mixins;


import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnAnyPlayerSwingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {



    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void onSwingHand(InteractionHand interactionHand, boolean bl, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;
        Component display = player.getDisplayName();
        if (display == null) return;

        var siblings = display.getSiblings();
        var hasLevel = false;
        for (var sibling : siblings) {

            String siblingStr = sibling.getString();
            String safeStr = siblingStr.replaceAll("§.", "");

            if (safeStr.matches(".*\\[\\d+].*")) {
                hasLevel = true;
                break;
            }
        }

        if (!hasLevel) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        boolean isSelf = player instanceof LocalPlayer;

        OnAnyPlayerSwingEvent event =
                new OnAnyPlayerSwingEvent(player, stack, isSelf);

        EventBus.post(event);
    }

}
