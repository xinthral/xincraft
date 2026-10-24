package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.item.RoseTintedGlassesItem;
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

// Tints the view a faint rose while wearing rose-tinted glasses, in first person. Drawn just below
// vanilla's camera overlay, like the tin spyglass's tint (TinSpyglassTint).
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class RoseTintedGlassesTint {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "rose_tinted_glasses_tint");
    // Faint, translucent rose (ARGB)
    private static final int TINT = 0x1CFF5C8A;

    @SubscribeEvent
    public static void onAddLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addBelow(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.CAMERA_OVERLAY, RoseTintedGlassesTint::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && minecraft.options.getCameraType().isFirstPerson() && RoseTintedGlassesItem.isWearing(player)) {
            graphics.fill(RenderPipelines.GUI, 0, 0, graphics.guiWidth(), graphics.guiHeight(), TINT);
        }
    }

    private RoseTintedGlassesTint() {}
}
