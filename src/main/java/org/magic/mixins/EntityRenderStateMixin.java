package org.magic.mixins;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.magic.misc.FakeEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements FakeEntityState {

    @Unique
    public boolean magicaddons$isFakeEntity;

    @Unique
    public int magicaddons$fakeEntityTintColor;

    @Override
    public int magicaddons$fakeEntityTintColor() {
        return  magicaddons$fakeEntityTintColor;
    }

    @Override
    public boolean magicaddons$isFakeEntity() {
        return magicaddons$isFakeEntity;
    }

    @Override
    public void magicaddons$setFakeEntityTintColor(int color){
        magicaddons$fakeEntityTintColor = color;
    }

    @Override
    public void magicaddons$setFakeEntity(boolean value) {
        magicaddons$isFakeEntity = value;
    }
}
