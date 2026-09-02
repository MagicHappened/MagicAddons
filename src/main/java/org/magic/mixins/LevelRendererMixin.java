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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//? if >=26.2 {
/*import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
*///?}
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

    //? if <26.2 {
    @Shadow
    public abstract void initOutline();

    @Shadow
    private @Nullable RenderTarget entityOutlineTarget;
    //?}

    //? if >=26.2 {
    /*@Inject(method = "addMainPass", at = @At("HEAD"))
    private void enableGlow(FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame featureFrame, GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        levelRenderState.shouldShowEntityOutlines = true;
    }
    *///?} else {
    @Inject(method = "addMainPass", at = @At("HEAD"))
    private void enableGlow(FrameGraphBuilder frame, Frustum frustum, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, boolean renderOutline, LevelRenderState levelRenderState, DeltaTracker deltaTracker, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        levelRenderState.haveGlowingEntities = true;
    }

    // 26.1.2 never builds the outline target on its own; 26.2 removed the call and does it itself
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void initOutlineIfNeeded(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        if (this.entityOutlineTarget == null) {
            this.initOutline();
        }
    }
    //?}

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

            if (entity instanceof Player){
                entity.setCustomNameVisible(false);
            }

            EntityRenderer<? super Entity, ?> baseRenderer = entityRenderDispatcher.getRenderer(entity);

            @SuppressWarnings("unchecked")
            EntityRenderer<Entity, EntityRenderState> renderer =
                    (EntityRenderer<Entity, EntityRenderState>) baseRenderer;

            float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

            EntityRenderState state = renderer.createRenderState(entity, partialTicks);
            renderer.extractRenderState(entity, state, partialTicks);

            poseStack.pushPose();

            Vec3 cam = levelRenderState.cameraRenderState.pos;

            poseStack.translate(
                    entity.getX() - cam.x,
                    entity.getY() - cam.y,
                    entity.getZ() - cam.z
            );

            //poseStack.scale(state.scale, state.scale, state.scale);
            //poseStack.translate(0.0F, 0.0F, 0.0F);

            state.outlineColor = source.highlightColor(entity);
            state.isInvisible = true;



            //? if >=26.2 {
            /*levelRenderState.shouldShowEntityOutlines = true;
            *///?} else {
            levelRenderState.haveGlowingEntities = true;
            //?}

            renderer.submit(state, poseStack, submitNodeCollector, levelRenderState.cameraRenderState);

            poseStack.popPose();
        }
    }


}
