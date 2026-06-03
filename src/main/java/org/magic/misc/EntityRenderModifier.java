package org.magic.misc;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface EntityRenderModifier {
    void modify(Entity entity, EntityRenderState state);
}