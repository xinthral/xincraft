package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
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

// The locomotive cart's inventory screen: the furnace screen's fuel slot and flame, plus one
// storage slot, and an energy bar when the energy upgrade is fitted
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class LocomotiveCartScreen extends AbstractContainerScreen<LocomotiveCartMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/gui/container/locomotive_cart.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    // Energy bar, inside its border
    private static final int BAR_X = 30;
    private static final int BAR_Y = 18;
    private static final int BAR_WIDTH = 8;
    private static final int BAR_HEIGHT = 50;

    public LocomotiveCartScreen(LocomotiveCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.LOCOMOTIVE_CART.get(), LocomotiveCartScreen::new));
    }

    @Override
    public void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    // Drawn the same way as the furnace's background and flame (AbstractFurnaceScreen)
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.isLit()) {
            int litProgressHeight = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - litProgressHeight,
                xo + 56, yo + 36 + 14 - litProgressHeight, 14, litProgressHeight);
        }
        if (menu.hasEnergyUpgrade()) {
            int x = xo + BAR_X;
            int y = yo + BAR_Y;
            // Bevelled like a slot: dark top and left edges, light bottom and right
            graphics.fill(x - 1, y - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF373737);
            graphics.fill(x, y, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFFFFFFFF);
            graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF3A1414);
            int capacity = Math.max(1, menu.getEnergyCapacity());
            int filled = (int) Math.ceil((long) BAR_HEIGHT * Math.min(menu.getEnergy(), capacity) / (double) capacity);
            graphics.fill(x, y + BAR_HEIGHT - filled, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFD02020);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.hasEnergyUpgrade() && isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("container.xincraft.locomotive_cart.energy",
                String.format("%,d", menu.getEnergy()), String.format("%,d", menu.getEnergyCapacity())), mouseX, mouseY);
        }
    }
}
