package com.xincraft.xincraft.client;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModEntityTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

// Draws the ender minecart as a vanilla minecart with its own texture. Vanilla's minecart
// renderer hardcodes its texture, so we hand it a collector that swaps the texture on the way
// through and passes everything else along untouched.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class EnderMinecartRenderer extends MinecartRenderer {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/entity/ender_minecart.png");

    public EnderMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.ENDER_MINECART.get(), EnderMinecartRenderer::new);
    }

    @Override
    public void submit(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, new Retextured(collector, model, model.renderType(TEXTURE)), camera);
    }

    // Every submitModel overload funnels into the abstract one, so that's the only place the
    // cart model's render type needs replacing
    private record Retextured(SubmitNodeCollector delegate, Model<?> target, RenderType renderType) implements SubmitNodeCollector {
        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite,
                int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            delegate.submitModel(model, state, poseStack, model == target ? this.renderType : renderType,
                lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
        }

        @Override public OrderedSubmitNodeCollector order(int order) { return delegate.order(order); }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
            delegate.submitShadow(poseStack, radius, pieces);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name,
                boolean seeThrough, int lightCoords, CameraRenderState camera) {
            delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
                Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
            delegate.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
            delegate.submitFlame(poseStack, renderState, rotation);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            delegate.submitLeash(poseStack, leashState);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
            delegate.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts,
                int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
            delegate.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
            delegate.submitBreakingBlockModel(poseStack, parts, progress);
        }

        @Override
        public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
            delegate.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
                int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
            delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
            delegate.submitCustomGeometry(poseStack, renderType, renderer);
        }

        @Override
        public void submitQuadParticleGroup(QuadParticleRenderState particles) {
            delegate.submitQuadParticleGroup(particles);
        }

        @Override
        public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
            delegate.submitGizmoPrimitives(group, camera, onTop);
        }
    }
}
