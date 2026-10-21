package com.xincraft.xincraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.entity.CartUpgrade;
import com.xincraft.xincraft.entity.Motorboat;
import com.xincraft.xincraft.entity.MotorboatType;
import com.xincraft.xincraft.registry.ModEntityTypes;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RaftRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Draws a motorboat as the vanilla boat (or raft) it was made from, using vanilla's own renderer,
// plus a propeller at the stern, spinning while it cruises, and its upgrades as small blocks along
// the gunwales.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class MotorboatRenderer extends EntityRenderer<Motorboat, MotorboatRenderer.State> {
    // Vanilla's model for each kind of boat
    private static final Map<MotorboatType, ModelLayerLocation> MODELS = Map.of(
        MotorboatType.OAK, ModelLayers.OAK_BOAT,
        MotorboatType.SPRUCE, ModelLayers.SPRUCE_BOAT,
        MotorboatType.BIRCH, ModelLayers.BIRCH_BOAT,
        MotorboatType.JUNGLE, ModelLayers.JUNGLE_BOAT,
        MotorboatType.ACACIA, ModelLayers.ACACIA_BOAT,
        MotorboatType.CHERRY, ModelLayers.CHERRY_BOAT,
        MotorboatType.DARK_OAK, ModelLayers.DARK_OAK_BOAT,
        MotorboatType.PALE_OAK, ModelLayers.PALE_OAK_BOAT,
        MotorboatType.MANGROVE, ModelLayers.MANGROVE_BOAT,
        MotorboatType.BAMBOO, ModelLayers.BAMBOO_RAFT);
    // Degrees the propeller turns each tick while cruising
    private static final float PROPELLER_SPEED = 45.0F;
    private static final float PROPELLER_SCALE = 0.5F;
    private static final float UPGRADE_SCALE = 0.25F;

    private final AbstractBoatRenderer boat;
    private final ItemModelResolver itemModelResolver;
    private final BlockModelResolver blockModelResolver;

    public MotorboatRenderer(EntityRendererProvider.Context context, MotorboatType kind) {
        super(context);
        this.boat = kind.raft() ? new RaftRenderer(context, MODELS.get(kind)) : new BoatRenderer(context, MODELS.get(kind));
        this.itemModelResolver = context.getItemModelResolver();
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.8F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (MotorboatType kind : MotorboatType.values()) {
            event.registerEntityRenderer(ModEntityTypes.MOTORBOATS.get(kind).get(), context -> new MotorboatRenderer(context, kind));
        }
    }

    // Vanilla's boat state plus the propeller and upgrades
    public static class State extends BoatRenderState {
        public final ItemStackRenderState propeller = new ItemStackRenderState();
        public float propellerAngle;
        public final List<BlockModelRenderState> upgradeModels = new ArrayList<>();
        public int upgradeCount;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Motorboat entity, State state, float partialTicks) {
        boat.extractRenderState(entity, state, partialTicks);
        itemModelResolver.updateForTopItem(state.propeller, new ItemStack(ModItems.PROPELLER.get()), ItemDisplayContext.FIXED,
            entity.level(), null, entity.getId());
        state.propellerAngle = entity.isCruising() ? (entity.tickCount + partialTicks) * PROPELLER_SPEED : 0.0F;
        List<CartUpgrade> installed = entity.upgrades().installed();
        state.upgradeCount = installed.size();
        for (int i = 0; i < installed.size(); i++) {
            if (state.upgradeModels.size() <= i) {
                state.upgradeModels.add(new BlockModelRenderState());
            }
            blockModelResolver.update(state.upgradeModels.get(i), installed.get(i).block().defaultBlockState(),
                AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        boat.submit(state, poseStack, collector, camera);
        // In the boat's own frame (as vanilla turns it), where +Z is the stern and -Z the bow
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));

        poseStack.pushPose();
        poseStack.translate(0.0F, -0.15F, 0.95F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.propellerAngle));
        poseStack.scale(PROPELLER_SCALE, PROPELLER_SCALE, PROPELLER_SCALE);
        state.propeller.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();

        // Upgrades sit on the gunwales, alternating sides, working back from the bow
        for (int i = 0; i < state.upgradeCount; i++) {
            poseStack.pushPose();
            float side = i % 2 == 0 ? 0.55F : -0.55F;
            poseStack.translate(side - UPGRADE_SCALE / 2.0F, 0.0F, -0.45F + (i / 2) * 0.35F - UPGRADE_SCALE / 2.0F);
            poseStack.scale(UPGRADE_SCALE, UPGRADE_SCALE, UPGRADE_SCALE);
            state.upgradeModels.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
