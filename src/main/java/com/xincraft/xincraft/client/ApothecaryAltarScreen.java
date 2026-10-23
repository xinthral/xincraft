package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.inventory.ApothecaryAltarMenu;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.registry.ModMenuTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// The apothecary altar's screen, laid out like a furnace: the plant slot over the flame and fuel
// slot, then the burn progress arrow pointing at a row of element pips. A pip is dark with no flora
// of its element in range, dim when they're all full, and bright when one has room; it's outlined
// when the plant in the slot gives that element. Hovering a pip says which.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class ApothecaryAltarScreen extends AbstractContainerScreen<ApothecaryAltarMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/gui/container/apothecary_altar.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int PIPS_X = 107;
    private static final int PIPS_Y = 39;
    private static final int PIP_SIZE = 7;
    private static final int PIP_SPACING = 9;

    public ApothecaryAltarScreen(ApothecaryAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.APOTHECARY_ALTAR.get(), ApothecaryAltarScreen::new));
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
                xo + 56, yo + 36 + 14 - litProgressHeight, 14, litProgressHeight);
        }
        int burnProgressWidth = Mth.ceil(menu.getBurnProgress() * 24.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, xo + 79, yo + 34, burnProgressWidth, 16);

        int plantMask = menu.getPlantMask();
        for (Element element : Element.values()) {
            int bit = 1 << element.ordinal();
            int x = xo + PIPS_X + element.ordinal() * PIP_SPACING;
            int y = yo + PIPS_Y;
            graphics.fill(x - 1, y - 1, x + PIP_SIZE + 1, y + PIP_SIZE + 1, (plantMask & bit) != 0 ? 0xFFFFFFFF : 0xFF373737);
            int color = (menu.getRoomMask() & bit) != 0 ? ARGB.opaque(element.color())
                : (menu.getFloraMask() & bit) != 0 ? ARGB.opaque(ARGB.scaleRGB(element.color(), 0.45F))
                : 0xFF2A2A2A;
            graphics.fill(x, y, x + PIP_SIZE, y + PIP_SIZE, color);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        for (Element element : Element.values()) {
            if (isHovering(PIPS_X + element.ordinal() * PIP_SPACING - 1, PIPS_Y - 1, PIP_SIZE + 2, PIP_SIZE + 2, mouseX, mouseY)) {
                int bit = 1 << element.ordinal();
                String state = (menu.getRoomMask() & bit) != 0 ? "room" : (menu.getFloraMask() & bit) != 0 ? "full" : "none";
                graphics.setTooltipForNextFrame(Component.translatable("container.xincraft.apothecary_altar." + state,
                    Component.translatable("block.xincraft." + element.floraId())), mouseX, mouseY);
            }
        }
    }
}
