package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.inventory.TurbineMenu;
import com.xincraft.xincraft.registry.ModMenuTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// A turbine's screen: the fuel slot and furnace flame in the middle, the energy bar on the right,
// and (on the steam turbine) the water gauge on the left. Hovering a bar shows the exact amount.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class TurbineScreen extends AbstractContainerScreen<TurbineMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/gui/container/turbine.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    // Gauges, inside their borders
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_HEIGHT = 52;
    private static final int ENERGY_X = 140;
    private static final int ENERGY_WIDTH = 12;
    private static final int WATER_X = 24;
    private static final int WATER_WIDTH = 16;

    public TurbineScreen(TurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.TURBINE.get(), TurbineScreen::new));
    }

    @Override
    public void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.isLit()) {
            int litProgressHeight = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - litProgressHeight,
                xo + 80, yo + 36 + 14 - litProgressHeight, 14, litProgressHeight);
        }
        gauge(graphics, xo + ENERGY_X, yo + GAUGE_Y, ENERGY_WIDTH, menu.getEnergy(), menu.getEnergyCapacity(), 0xFF3A1414, 0xFFD02020);
        if (menu.hasTank()) {
            gauge(graphics, xo + WATER_X, yo + GAUGE_Y, WATER_WIDTH, menu.getWater(), menu.getWaterCapacity(), 0xFF141E3A, 0xFF3F76E4);
        }
    }

    // A slot-style bevelled bar, filled from the bottom
    private static void gauge(GuiGraphicsExtractor graphics, int x, int y, int width, int amount, int capacity, int empty, int full) {
        graphics.fill(x - 1, y - 1, x + width, y + GAUGE_HEIGHT, 0xFF373737);
        graphics.fill(x, y, x + width + 1, y + GAUGE_HEIGHT + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + width, y + GAUGE_HEIGHT, empty);
        int filled = (int) Math.ceil((long) GAUGE_HEIGHT * Math.min(amount, Math.max(1, capacity)) / (double) Math.max(1, capacity));
        graphics.fill(x, y + GAUGE_HEIGHT - filled, x + width, y + GAUGE_HEIGHT, full);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(ENERGY_X, GAUGE_Y, ENERGY_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("container.xincraft.turbine.energy",
                String.format("%,d", menu.getEnergy()), String.format("%,d", menu.getEnergyCapacity())), mouseX, mouseY);
        } else if (menu.hasTank() && isHovering(WATER_X, GAUGE_Y, WATER_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("container.xincraft.turbine.water",
                String.format("%,d", menu.getWater()), String.format("%,d", menu.getWaterCapacity())), mouseX, mouseY);
        }
    }
}
