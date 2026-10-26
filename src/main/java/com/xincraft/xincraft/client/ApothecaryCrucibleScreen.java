package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.inventory.ApothecaryCrucibleMenu;
import com.xincraft.xincraft.item.ElementPowderItem;
import com.xincraft.xincraft.magic.CrucibleInput;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.registry.ModMenuTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// The apothecary crucible's screen: the two powders, catalyst and filter in a triangle, an arrow to
// the inscribed filter, and a row of element pips for nature's balance. A pip lights up when the
// inscription draws on its element, and turns red when the flora around the altar are short of it.
// The filter slot glows red when the filter is full or already lists the mob. Hovering a pip, or the
// empty output, says why.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class ApothecaryCrucibleScreen extends AbstractContainerScreen<ApothecaryCrucibleMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/gui/container/apothecary_crucible.png");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int FILTER_X = 50;
    private static final int FILTER_Y = 55;
    private static final int RESULT_X = 124;
    private static final int RESULT_Y = 34;
    private static final int ARROW_X = 94;
    private static final int ARROW_Y = 35;
    private static final int PIPS_X = 104;
    private static final int PIPS_Y = 60;
    private static final int PIP_SIZE = 7;
    private static final int PIP_SPACING = 9;
    private static final int SHORT_COLOR = 0xFFE03030;

    public ApothecaryCrucibleScreen(ApothecaryCrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.APOTHECARY_CRUCIBLE.get(), ApothecaryCrucibleScreen::new));
    }

    @Override
    public void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    // The elements the powders in the crucible would draw on, as a bitmask; none until both are in
    private int balancingMask() {
        ItemStack left = menu.getSlot(ApothecaryCrucibleMenu.LEFT_SLOT).getItem();
        ItemStack right = menu.getSlot(ApothecaryCrucibleMenu.RIGHT_SLOT).getItem();
        if (!(left.getItem() instanceof ElementPowderItem) || !(right.getItem() instanceof ElementPowderItem)) {
            return 0;
        }
        int mask = 0;
        for (Element element : CrucibleRecipe.balancing(new CrucibleInput(left, right, ItemStack.EMPTY, ItemStack.EMPTY))) {
            mask |= 1 << element.ordinal();
        }
        return mask;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.getStatus() == ApothecaryCrucibleMenu.STATUS_READY) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, 24, 16, 0, 0, xo + ARROW_X, yo + ARROW_Y, 24, 16);
        }
        if (menu.isFilterBlocked()) {
            graphics.fill(xo + FILTER_X - 1, yo + FILTER_Y - 1, xo + FILTER_X + 17, yo + FILTER_Y + 17, 0xC0D02020);
        }

        int balancing = balancingMask();
        for (Element element : Element.values()) {
            int bit = 1 << element.ordinal();
            int x = xo + PIPS_X + element.ordinal() * PIP_SPACING;
            int y = yo + PIPS_Y;
            graphics.fill(x - 1, y - 1, x + PIP_SIZE + 1, y + PIP_SIZE + 1, (menu.getShortMask() & bit) != 0 ? SHORT_COLOR : 0xFF373737);
            int color = (balancing & bit) != 0 ? ARGB.opaque(element.color())
                : ARGB.opaque(ARGB.scaleRGB(element.color(), 0.3F));
            graphics.fill(x, y, x + PIP_SIZE, y + PIP_SIZE, color);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int balancing = balancingMask();
        for (Element element : Element.values()) {
            if (isHovering(PIPS_X + element.ordinal() * PIP_SPACING - 1, PIPS_Y - 1, PIP_SIZE + 2, PIP_SIZE + 2, mouseX, mouseY)) {
                int bit = 1 << element.ordinal();
                String state = (menu.getShortMask() & bit) != 0 ? "short" : (balancing & bit) != 0 ? "draws" : "spared";
                graphics.setTooltipForNextFrame(Component.translatable("container.xincraft.apothecary_crucible." + state,
                    Component.translatable("block.xincraft." + element.floraId())), mouseX, mouseY);
            }
        }
        String reason = switch (menu.getStatus()) {
            case ApothecaryCrucibleMenu.STATUS_NO_FILTER -> "no_filter";
            case ApothecaryCrucibleMenu.STATUS_FULL -> "full";
            case ApothecaryCrucibleMenu.STATUS_DUPLICATE -> "duplicate";
            case ApothecaryCrucibleMenu.STATUS_SHORT -> "short_any";
            default -> null;
        };
        if (reason != null && isHovering(RESULT_X, RESULT_Y, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("container.xincraft.apothecary_crucible." + reason), mouseX, mouseY);
        }
    }
}
