package com.xincraft.xincraft.common.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// Lines of text under the crosshair on a dark backdrop, for what rose-tinted glasses reveal about the
// block the player is looking at
public final class CrosshairReadout {
    // Gap between the crosshair and the readout, and padding around its text
    private static final int OFFSET_Y = 12;
    private static final int PADDING = 3;
    private static final int BACKDROP = 0x90000000;

    public record Line(Component text, int color) {}

    public static void draw(GuiGraphicsExtractor graphics, List<Line> lines) {
        if (lines.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, font.width(line.text()));
        }
        int lineHeight = font.lineHeight + 1;
        int centerX = graphics.guiWidth() / 2;
        int top = graphics.guiHeight() / 2 + OFFSET_Y;
        graphics.fill(centerX - width / 2 - PADDING, top - PADDING,
            centerX + (width + 1) / 2 + PADDING, top + lines.size() * lineHeight - 1 + PADDING, BACKDROP);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            graphics.text(font, line.text(), centerX - font.width(line.text()) / 2, top + i * lineHeight, line.color(), true);
        }
    }

    private CrosshairReadout() {}
}
