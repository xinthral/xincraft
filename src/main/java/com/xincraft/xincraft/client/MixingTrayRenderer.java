package com.xincraft.xincraft.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.MixingTrayBlockEntity;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

// Shows what's on a mixing tray: the flowers laid flat and fanned around the middle, or the pollen
// on its own in the centre. Drawn the way vanilla's campfire draws its food.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class MixingTrayRenderer implements BlockEntityRenderer<MixingTrayBlockEntity, MixingTrayRenderer.State> {
    private static final float SIZE = 0.4F;
    // How far each flower sits from the middle
    private static final float SPREAD = 0.16F;

    private final ItemModelResolver itemModelResolver;

    public MixingTrayRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MIXING_TRAY.get(), MixingTrayRenderer::new);
    }

    public static class State extends BlockEntityRenderState {
        public List<ItemStackRenderState> items = Collections.emptyList();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MixingTrayBlockEntity tray, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tray, state, partialTicks, cameraPosition, breakProgress);
        int seed = (int) tray.getBlockPos().asLong();
        state.items = new ArrayList<>();
        for (int slot = 0; slot < tray.getItems().size(); slot++) {
            ItemStack stack = tray.getItems().get(slot);
            if (stack.isEmpty()) {
                break;
            }
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, tray.getLevel(), null, seed + slot);
            state.items.add(itemState);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int count = state.items.size();
        for (int slot = 0; slot < count; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 2.0F / 16.0F + 0.005F * slot, 0.5F);
            // A lone item sits in the middle; flowers fan out, a third of a turn apart
            poseStack.mulPose(Axis.YP.rotationDegrees(slot * 120.0F));
            if (count > 1) {
                poseStack.translate(0.0F, 0.0F, SPREAD);
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(SIZE, SIZE, SIZE);
            state.items.get(slot).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
