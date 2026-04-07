package org.magic.mixins;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnAnyPlayerSwingEvent;
import org.magic.magicaddons.util.ChatUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Final
    @Shadow
    protected EntityEquipment equipment;




    @Inject(
            method = "swingHand(Lnet/minecraft/util/Hand;Z)V",
            at = @At("HEAD")
    )
    private void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof AbstractClientPlayerEntity player)) return;
        Text display = player.getDisplayName();
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

        ItemStack stack = player.getStackInHand(hand);
        boolean isSelf = player instanceof ClientPlayerEntity;

        OnAnyPlayerSwingEvent event =
                new OnAnyPlayerSwingEvent(player, stack, isSelf);

        EventBus.post(event);
    }

    @Unique
    public EntityEquipment getEquipmentPublic(){
        return this.equipment;
    }

}
