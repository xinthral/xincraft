package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.item.TinSpyglass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Tints the view green while looking through a tin spyglass. Drawn just below vanilla's camera
// overlay, so the spyglass scope's black ring drawn on top masks the tint to the lens.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class TinSpyglassTint {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin_spyglass_tint");
    // Translucent green (ARGB)
    private static final int TINT = 0x3040FF60;

    @SubscribeEvent
    public static void onAddLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addBelow(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.CAMERA_OVERLAY, TinSpyglassTint::extract);
    }

    // Same conditions vanilla uses for the scope overlay (Hud#extractCameraOverlays)
    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && minecraft.options.getCameraType().isFirstPerson() && TinSpyglass.isLookingThrough(player)) {
            graphics.fill(RenderPipelines.GUI, 0, 0, graphics.guiWidth(), graphics.guiHeight(), TINT);
        }
    }

    private TinSpyglassTint() {}
}
