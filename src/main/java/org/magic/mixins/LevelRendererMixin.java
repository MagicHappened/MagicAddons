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
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.magic.magicaddons.util.EntityUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract void doEntityOutline();


    @Shadow
    private @Nullable RenderTarget entityOutlineTarget;

    @Inject(method = "addMainPass", at = @At("HEAD"))
    private void enableGlow(FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame featureFrame, GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        levelRenderState.shouldShowEntityOutlines = true;
    }

//    @Inject(method = "render", at = @At("HEAD"))
//    private void initOutlineIfNeeded(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
//        if (this.entityOutlineTarget == null) {
//            this.doEntityOutline();
//        }
//    } maybe not needed

    @Inject(
            method = "submitEntities",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci){
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (Map.Entry<Entity, EntityUtils.HighlightSource> entry : EntityUtils.getResolvedMap().entrySet()) {
            Entity entity = entry.getKey();
            EntityUtils.HighlightSource source = entry.getValue();

            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof Player){
                living.setCustomNameVisible(false);
            }

            EntityRenderer<? super LivingEntity, ?> baseRenderer = entityRenderDispatcher.getRenderer(living);
            if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?> rawRenderer)) continue;

            @SuppressWarnings("unchecked")
            LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                    (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) rawRenderer;

            float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

            LivingEntityRenderState state = renderer.createRenderState(living, partialTicks);
            renderer.extractRenderState(living, state, partialTicks);

            poseStack.pushPose();

            Vec3 cam = levelRenderState.cameraRenderState.pos;

            poseStack.translate(
                    living.getX() - cam.x,
                    living.getY() - cam.y,
                    living.getZ() - cam.z
            );

            poseStack.scale(state.scale, state.scale, state.scale);
            poseStack.translate(0.0F, 0.0F, 0.0F);

            state.outlineColor = source.getHighlightColor();
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;



            levelRenderState.shouldShowEntityOutlines = true;

            renderer.submit(state, poseStack, submitNodeCollector, levelRenderState.cameraRenderState);

            poseStack.popPose();
        }
    }


}
