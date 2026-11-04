package com.xincraft.xincraft.golems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.golems.BronzeChestBlockEntity;
import com.xincraft.xincraft.golems.GolemBlockEntities;
import com.xincraft.xincraft.golems.GolemMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * @brief Draws the bronze chest like vanilla's chests, with its own textures: vanilla picks a
 *        chest's texture from a fixed list.
 *
 * @see golems.BronzeChestBlock
 */
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class BronzeChestRenderer extends ChestRenderer<BronzeChestBlockEntity> {
    private static final MultiblockChestResources<SpriteId> TEXTURES = new MultiblockChestResources<>(
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bronze"),
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bronze_left"),
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bronze_right")).map(Sheets.CHEST_MAPPER::apply);

    private final SpriteGetter sprites;
    private final MultiblockChestResources<ChestModel> models;

    private BronzeChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        sprites = context.sprites();
        models = LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer)));
    }

    @Override
    public void submit(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.facing));
        float closed = 1.0F - state.open;
        float open = 1.0F - closed * closed * closed;
        collector.submitModel(models.select(state.type), open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
            TEXTURES.select(state.type), sprites, 0, state.breakProgress);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GolemBlockEntities.BRONZE_CHEST.get(), BronzeChestRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(GolemMenus.BRONZE_CHEST_DOUBLE.get(), TallChestScreen::new));
    }
}
