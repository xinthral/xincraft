package com.xincraft.xincraft.golems.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

// A chest screen for more rows than vanilla's texture has, repeating its first row as many times as
// needed; used for the double bronze chest's eight
public class TallChestScreen extends AbstractContainerScreen<ChestMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TOP = 17;
    private static final int ROW = 18;
    private static final int INVENTORY_V = 126;
    private static final int INVENTORY_HEIGHT = 96;

    public TallChestScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.getRowCount() * ROW);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, TOP, 256, 256);
        for (int row = 0; row < menu.getRowCount(); row++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos + TOP + row * ROW, 0.0F, TOP, imageWidth, ROW, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos + TOP + menu.getRowCount() * ROW, 0.0F, INVENTORY_V,
            imageWidth, INVENTORY_HEIGHT, 256, 256);
    }
}
