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
