package org.magic.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract void doEntityOutline();

    @Shadow
    public abstract void initOutline();


    @Shadow
    private @Nullable RenderTarget entityOutlineTarget;

    @Inject(method = "addMainPass", at = @At("HEAD"))
    private void enableGlow(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Matrix4f matrix4f, GpuBufferSlice gpuBufferSlice, boolean bl, LevelRenderState levelRenderState, DeltaTracker deltaTracker, ProfilerFiller profilerFiller, CallbackInfo ci) {
        //todo add check if any highlighting exists
        levelRenderState.haveGlowingEntities = true;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void initOutlineIfNeeded(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2, CallbackInfo ci) {
        if (this.entityOutlineTarget == null) {
            this.initOutline();
        }
    }

    @Inject(
            method = "submitEntities",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci){
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        //todo change it so its not just pig lmao
        Pig pig = mc.level.getEntitiesOfClass(Pig.class, mc.player.getBoundingBox().inflate(50))
                .stream()
                .findFirst()
                .orElse(null);

        if (pig == null) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> baseRenderer = dispatcher.getRenderer(pig);

        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?> rawRenderer)) return;

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<Pig, LivingEntityRenderState, ?> renderer =
                (LivingEntityRenderer<Pig, LivingEntityRenderState, ?>) rawRenderer;

        LivingEntityRenderState state = renderer.createRenderState(pig, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));

        poseStack.pushPose();

        Vec3 cam = levelRenderState.cameraRenderState.pos;
        // Position relative to camera
        poseStack.translate(
                 pig.getX() - cam.x,
                pig.getY() - cam.y,
                pig.getZ() - cam.z
        );

        float scale = state.scale;
        poseStack.scale(scale, scale, scale);

        //todo make this actually be inside the model
        poseStack.translate(0.0F, 1.501F, 0.0F);

        renderer.extractRenderState(pig,state,Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        state.outlineColor = 0xFFFFFF00;
        levelRenderState.haveGlowingEntities = true;
        state.isInvisible = true;
        state.isInvisibleToPlayer = true;

        renderer.submit(state,poseStack,submitNodeCollector,new CameraRenderState());

        poseStack.popPose();
    }


}
