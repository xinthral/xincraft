package com.xincraft.xincraft.transport.cart.client;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.transport.cart.LocomotiveCart;
import com.xincraft.xincraft.transport.train.CartUpgrade;
import com.xincraft.xincraft.transport.train.UpgradableCart;
import com.xincraft.xincraft.transport.TransportEntityTypes;
import com.xincraft.xincraft.transport.train.client.CellCage;
import com.xincraft.xincraft.transport.train.client.LinkingBar;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * @brief Draws the mod's minecarts as vanilla minecarts with their own textures.
 *
 * Vanilla's minecart renderer hardcodes its texture, so this hands it a collector that swaps the
 * texture on the way through. It also draws a cart's upgrades as small models of their blocks, the
 * linking bar to the cart it tows, and a locomotive's handbrake lever.
 *
 * @see transport.train.CartUpgrades
 * @see transport.train.client.CellCage
 * @see transport.train.client.LinkingBar
 */
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class RetexturedMinecartRenderer extends MinecartRenderer {
    /** @brief The size upgrade blocks are drawn at, sitting on the cart's end walls. */
    private static final float UPGRADE_SCALE = 0.25F;
    /** @brief How far the end walls' top is above the cart's centre. */
    private static final float WALL_TOP = 5.0F / 16.0F;
    /** @brief How far the end walls' middle is either side of the cart's centre. */
    private static final float END_WALL = 9.0F / 16.0F;
    /** @brief Half-length of the cell upgrade's cage, which stands on the cart's walls. */
    private static final float CART_HALF_LENGTH = 10.0F / 16.0F;
    /** @brief Half-width of the cell upgrade's cage. */
    private static final float CART_HALF_WIDTH = 8.0F / 16.0F;
    /**
     * @brief Height of the cell upgrade's cage, tall enough to hold the spawn egg shown for the mob
     *        inside.
     */
    private static final float CAGE_HEIGHT = 0.8F;
    /** @brief The size the handbrake lever is drawn at. */
    private static final float LEVER_SCALE = 0.5F;
    /** @brief How far the side wall the lever stands on is from the cart's centre. */
    private static final float SIDE_WALL = 7.0F / 16.0F;

    private final Identifier texture;
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;

    public RetexturedMinecartRenderer(EntityRendererProvider.Context context, String texture) {
        super(context, ModelLayers.MINECART);
        this.texture = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/entity/" + texture + ".png");
        this.blockModelResolver = context.getBlockModelResolver();
        this.itemModelResolver = context.getItemModelResolver();
    }

    /**
     * @brief Vanilla's render state plus the upgrade block models, and the linking bar to a towed
     *        cart, if it tows one.
     */
    public static class UpgradedMinecartRenderState extends MinecartRenderState {
        public final List<BlockModelRenderState> upgradeModels = new ArrayList<>();
        public int upgradeCount;
        public final CellCage.Models cage = new CellCage.Models();
        public LinkingBar.@Nullable Link link;
        public final BlockModelRenderState lever = new BlockModelRenderState();
        public boolean hasLever;
    }

    @Override
    public MinecartRenderState createRenderState() {
        return new UpgradedMinecartRenderState();
    }

    @Override
    public void extractRenderState(AbstractMinecart cart, MinecartRenderState state, float partialTicks) {
        super.extractRenderState(cart, state, partialTicks);
        if (!(state instanceof UpgradedMinecartRenderState upgraded)) {
            return;
        }
        List<CartUpgrade> installed = cart instanceof UpgradableCart upgradable ? upgradable.upgrades().installed() : List.of();
        // The cell (and its reinforcement) is drawn as the cage rather than blocks of their own
        boolean caged = installed.contains(CartUpgrade.CELL);
        CellCage.extract(blockModelResolver, upgraded.cage, caged, installed.contains(CartUpgrade.REINFORCED_CAGE));
        installed = installed.stream().filter(upgrade -> upgrade != CartUpgrade.CELL && upgrade != CartUpgrade.REINFORCED_CAGE).toList();
        upgraded.upgradeCount = installed.size();
        for (int i = 0; i < installed.size(); i++) {
            if (upgraded.upgradeModels.size() <= i) {
                upgraded.upgradeModels.add(new BlockModelRenderState());
            }
            blockModelResolver.update(upgraded.upgradeModels.get(i), installed.get(i).block().defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
        }
        upgraded.hasLever = cart instanceof LocomotiveCart;
        if (cart instanceof LocomotiveCart locomotive) {
            // Facing along the cart, so it throws forward and back
            blockModelResolver.update(upgraded.lever, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.EAST)
                .setValue(LeverBlock.POWERED, locomotive.isHandbrakeOn()), BLOCK_DISPLAY_CONTEXT);
        }
        upgraded.link = LinkingBar.extract(cart, new Vec3(state.x, state.y, state.z), partialTicks);
        CellCage.extractEggs(itemModelResolver, upgraded.cage, cart, new Vec3(state.x, state.y, state.z), partialTicks);
    }

    @Override
    protected AABB getBoundingBoxForCulling(AbstractMinecart cart) {
        return LinkingBar.cullingBox(cart, super.getBoundingBoxForCulling(cart));
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TransportEntityTypes.ENDER_MINECART.get(),
            context -> new RetexturedMinecartRenderer(context, "ender_minecart"));
        event.registerEntityRenderer(TransportEntityTypes.LOCOMOTIVE_CART.get(),
            context -> new RetexturedMinecartRenderer(context, "locomotive_cart"));
        event.registerEntityRenderer(TransportEntityTypes.JINGLE_CART.get(),
            context -> new RetexturedMinecartRenderer(context, "jingle_cart"));
        event.registerEntityRenderer(TransportEntityTypes.CARRIAGE.get(),
            context -> new RetexturedMinecartRenderer(context, "carriage"));
        event.registerEntityRenderer(TransportEntityTypes.FREIGHT_CART.get(),
            context -> new RetexturedMinecartRenderer(context, "freight_cart"));
    }

    @Override
    public void submit(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state instanceof UpgradedMinecartRenderState upgraded) {
            if (upgraded.link != null) {
                LinkingBar.submit(upgraded.link, state.lightCoords, poseStack, collector);
            }
            CellCage.submitEggs(upgraded.cage, poseStack, collector, state.lightCoords, state.outlineColor, camera);
        }
        super.submit(state, poseStack, new Retextured(collector, model, model.renderType(texture), state), camera);
    }

    /**
     * @brief Draws the upgrade blocks in the cart model's own frame (flipped upside down, as
     *        vanilla draws the model), so they move and tilt with the cart.
     *
     * @param state The cart's render state.
     * @param poseStack The current transform.
     * @param collector Collects what gets drawn.
     */
    private static void submitUpgrades(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!(state instanceof UpgradedMinecartRenderState upgraded)) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        CellCage.submit(upgraded.cage, CART_HALF_LENGTH, CART_HALF_WIDTH, WALL_TOP, CAGE_HEIGHT, poseStack, collector,
            state.lightCoords, state.outlineColor);
        poseStack.popPose();
        if (upgraded.hasLever) {
            poseStack.pushPose();
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-LEVER_SCALE / 2.0F, WALL_TOP, SIDE_WALL - LEVER_SCALE / 2.0F);
            poseStack.scale(LEVER_SCALE, LEVER_SCALE, LEVER_SCALE);
            upgraded.lever.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        for (int i = 0; i < upgraded.upgradeCount; i++) {
            poseStack.pushPose();
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            float end = i % 2 == 0 ? END_WALL : -END_WALL;
            poseStack.translate(end - UPGRADE_SCALE / 2.0F, WALL_TOP, -UPGRADE_SCALE / 2.0F);
            poseStack.scale(UPGRADE_SCALE, UPGRADE_SCALE, UPGRADE_SCALE);
            upgraded.upgradeModels.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }

    /**
     * @brief Every submitModel overload funnels into the abstract one, so that's the only place the
     *        cart model's render type needs replacing, and where the upgrades are added to the
     *        cart.
     *
     * @param delegate The collector everything else is passed along to.
     * @param target The cart model.
     * @param renderType The render type to use in its place.
     * @param cart The cart being drawn.
     */
    private record Retextured(SubmitNodeCollector delegate, Model<?> target, RenderType renderType, MinecartRenderState cart)
            implements SubmitNodeCollector {
        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite,
                int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            delegate.submitModel(model, state, poseStack, model == target ? this.renderType : renderType,
                lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
            if (model == target) {
                submitUpgrades(cart, poseStack, delegate);
            }
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
