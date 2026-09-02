package org.magic.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.magic.magicaddons.commands.debug.FarmingDebug;
import org.magic.magicaddons.commands.debug.CropCollector;
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState;
import org.magic.magicaddons.util.EntityUtils;
import org.magic.misc.EntityRenderModifier;
import org.magic.misc.WrappedEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

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

    // 26.1.2 never builds the outline target on its own, so the glow had nothing to draw into
    // until something else asked for one. 26.2 removed the call and does it itself
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

            // any entity, not just living ones: the grass treasure and several safari uniques
            // are item displays, and renderFakeEntity is generic over the renderer already
            if (entity instanceof Player) {
                entity.setCustomNameVisible(false);
            }
            renderFakeEntity(
                    entity,
                    poseStack,
                    levelRenderState,
                    submitNodeCollector,
                    (ent, state) -> {

                        // highlight-specific logic
                        state.outlineColor = source.highlightColor(ent);
                        state.isInvisible = true;
                    },
                    true
            );
        }

        // the layout plan, drawn from this pass so it is placed against the camera the frame is
        // actually drawn with rather than one read at some other moment
        LayoutRenderState.INSTANCE.submit(
                poseStack,
                submitNodeCollector,
                levelRenderState.cameraRenderState.pos
        );

        // whatever the farming debug last listed, lit up so it can be counted by eye
        FarmingDebug.INSTANCE.submitHighlights(
                poseStack,
                submitNodeCollector,
                levelRenderState.cameraRenderState.pos
        );

        // whatever the crop collector last grouped, held up for confirmation
        CropCollector.INSTANCE.submitHighlights(
                poseStack,
                submitNodeCollector,
                levelRenderState.cameraRenderState.pos
        );

        // the stands a ghosted crop is made of, drawn alongside its blocks
        for (ArmorStand stand : LayoutRenderState.INSTANCE.getGhostStands()) {
            renderFakeEntity(
                    stand,
                    poseStack,
                    levelRenderState,
                    submitNodeCollector,
                    (ent, state) -> {
                        // the stand is scaffolding for the head it holds, and the three ways a
                        // body gets drawn all have to be closed. Visible draws it as itself.
                        // Invisible-to-player draws it translucent. Invisible while carrying an
                        // outline colour draws it as an outline of its whole self, which is how
                        // the highlighted mobs are outlined and was this stand tracing its own
                        // arms and legs. So it is hidden, not translucent, and carries no colour
                        state.isInvisible = true;
                        state.outlineColor = EntityRenderState.NO_OUTLINE;

                        if (state instanceof LivingEntityRenderState living) {
                            living.isInvisibleToPlayer = false;
                        }

                        // the head's own outline, which the head layer reads instead. The outline
                        // alone says this head is a plan rather than a plant
                        if (state instanceof WrappedEntityRenderState wrapped) {
                            wrapped.magicaddons$setHeadOutlineColor(
                                    LayoutRenderState.GHOST_OUTLINE_COLOR
                            );
                        }
                    },
                    false
            );
        }


    }

    @Unique
    private void renderFakeEntity(
            Entity entity,
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector submitNodeCollector,
            EntityRenderModifier modifier,
            Boolean shouldPartialTick
    ) {
        float partialTicks = shouldPartialTick ? Minecraft.getInstance()
                .getDeltaTracker()
                .getGameTimeDeltaPartialTick(false) : 1.0f;


        EntityRenderer<? super Entity, ?> baseRenderer =
                entityRenderDispatcher.getRenderer(entity);

        @SuppressWarnings("unchecked")
        EntityRenderer<Entity, EntityRenderState> renderer =
                (EntityRenderer<Entity, EntityRenderState>) baseRenderer;


        EntityRenderState state = renderer.createRenderState(entity, partialTicks);

        // the modifier speaks last: extractRenderState writes outlineColor and isInvisible from
        // the entity itself, so a modifier run before it was silently overwritten, and the glow
        // it asked for never survived to the submit
        renderer.extractRenderState(entity, state, partialTicks);
        modifier.modify(entity, state);



        poseStack.pushPose();

        Vec3 cam = levelRenderState.cameraRenderState.pos;

        poseStack.translate(
                entity.getX() - cam.x,
                entity.getY() - cam.y,
                entity.getZ() - cam.z
        );

            //? if >=26.2 {
            /*levelRenderState.shouldShowEntityOutlines = true;
            *///?} else {
            levelRenderState.haveGlowingEntities = true;
            //?}

        renderer.submit(state, poseStack, submitNodeCollector, levelRenderState.cameraRenderState);
        poseStack.popPose();
    }

}
