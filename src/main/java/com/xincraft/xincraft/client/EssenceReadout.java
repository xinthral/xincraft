package com.xincraft.xincraft.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.ApothecaryAltarBlockEntity;
import com.xincraft.xincraft.block.FloraBlockEntity;
import com.xincraft.xincraft.item.RoseTintedGlassesItem;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.magic.EssenceHelper;
import com.xincraft.xincraft.magic.EssenceReservoir;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// While the player wears or holds rose-tinted glasses, looking at an elemental flora shows how much
// essence it holds out of its capacity, and looking at an apothecary altar shows the flora in its
// range, element by element: how many, and their essence and capacity between them. Drawn under the
// crosshair; the flora's amounts are synced from the server (FloraBlockEntity's update tag).
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class EssenceReadout {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "essence_readout");
    // Gap between the crosshair and the readout, and padding around its text
    private static final int OFFSET_Y = 12;
    private static final int PADDING = 3;
    private static final int BACKDROP = 0x90000000;
    private static final int HEADER_COLOR = 0xFFFFFFFF;
    private static final int NONE_COLOR = 0xFFAAAAAA;

    private record Line(Component text, int color) {}

    // Rebuilt every client tick from what the player is looking at
    private static List<Line> lines = List.of();

    @SubscribeEvent
    public static void onAddLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.CROSSHAIR, EssenceReadout::extract);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !RoseTintedGlassesItem.isUsing(player)
                || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            lines = List.of();
            return;
        }
        BlockEntity target = level.getBlockEntity(hit.getBlockPos());
        if (target instanceof FloraBlockEntity flora) {
            lines = List.of(new Line(Component.translatable("message.xincraft.flora_essence",
                floraName(flora.getElement()), flora.getEssence(), flora.getCapacity()), textColor(flora.getElement())));
        } else if (target instanceof ApothecaryAltarBlockEntity altar) {
            lines = altarLines(altar);
        } else {
            lines = List.of();
        }
    }

    private static List<Line> altarLines(ApothecaryAltarBlockEntity altar) {
        int range = altar.getRange();
        Map<Element, List<EssenceReservoir>> byElement = new EnumMap<>(Element.class);
        for (EssenceReservoir reservoir : EssenceHelper.findReservoirs(altar.getLevel(), altar.getBlockPos(), range)) {
            byElement.computeIfAbsent(reservoir.getElement(), element -> new ArrayList<>()).add(reservoir);
        }
        List<Line> found = new ArrayList<>();
        found.add(new Line(Component.translatable("hud.xincraft.altar_flora", range), HEADER_COLOR));
        if (byElement.isEmpty()) {
            found.add(new Line(Component.translatable("hud.xincraft.altar_flora.none"), NONE_COLOR));
        }
        byElement.forEach((element, flora) -> {
            int capacity = flora.stream().mapToInt(EssenceReservoir::getCapacity).sum();
            found.add(new Line(Component.translatable("hud.xincraft.altar_flora.element",
                floraName(element), flora.size(), EssenceHelper.total(flora), capacity), textColor(element)));
        });
        return found;
    }

    private static Component floraName(Element element) {
        return Component.translatable("block." + XincraftMod.MODID + "." + element.floraId());
    }

    // Opaque, as text needs; the darker elements are lifted a little to read against the backdrop
    private static int textColor(Element element) {
        int rgb = element.color();
        int r = Math.max((rgb >> 16) & 0xFF, 0x70);
        int g = Math.max((rgb >> 8) & 0xFF, 0x50);
        int b = Math.max(rgb & 0xFF, 0x50);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Line> shown = lines;
        if (shown.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        int width = 0;
        for (Line line : shown) {
            width = Math.max(width, font.width(line.text()));
        }
        int lineHeight = font.lineHeight + 1;
        int centerX = graphics.guiWidth() / 2;
        int top = graphics.guiHeight() / 2 + OFFSET_Y;
        graphics.fill(centerX - width / 2 - PADDING, top - PADDING,
            centerX + (width + 1) / 2 + PADDING, top + shown.size() * lineHeight - 1 + PADDING, BACKDROP);
        for (int i = 0; i < shown.size(); i++) {
            Line line = shown.get(i);
            graphics.text(font, line.text(), centerX - font.width(line.text()) / 2, top + i * lineHeight, line.color(), true);
        }
    }

    private EssenceReadout() {}
}
