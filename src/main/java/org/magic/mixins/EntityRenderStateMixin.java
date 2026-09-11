package org.magic.mixins;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements WrappedEntityRenderState {

    @Unique
    public boolean magicaddons$isFakeEntity;

    @Unique
    public int magicaddons$fakeEntityTintColor;

    @Unique
    public int magicaddons$headOutline;

    @Unique
    public float magicaddons$smolBodyFactor = 1f;

    @Unique
    public float magicaddons$smolHeadFactor = 1f;

    @Override
    public float magicaddons$smolBody() {
        return magicaddons$smolBodyFactor;
    }

    @Override
    public float magicaddons$smolHead() {
        return magicaddons$smolHeadFactor;
    }

    @Override
    public void magicaddons$setSmolFactors(float body, float head) {
        magicaddons$smolBodyFactor = body;
        magicaddons$smolHeadFactor = head;
    }

    @Override
    public int magicaddons$headOutlineColor() {
        return magicaddons$headOutline;
    }

    @Override
    public void magicaddons$setHeadOutlineColor(int value) {
        magicaddons$headOutline = value;
    }

    @Override
    public int magicaddons$entityTintColor() {
        return  magicaddons$fakeEntityTintColor;
    }

    @Override
    public boolean magicaddons$isWrappedEntity() {
        return magicaddons$isFakeEntity;
    }

    @Override
    public void magicaddons$setWrappedEntityTintColor(int color){
        magicaddons$fakeEntityTintColor = color;
    }

    @Override
    public void magicaddons$setWrappedEntity(boolean value) {
        magicaddons$isFakeEntity = value;
    }
}
