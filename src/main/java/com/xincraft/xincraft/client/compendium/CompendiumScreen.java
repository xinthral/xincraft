package com.xincraft.xincraft.client.compendium;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// The compendium: a two-page book. The home spread lists the categories, with a search box, and the
// guide entries (or search results). A category lists its sub-categories and entries after its
// overview; an entry's pages run over as many spreads as they need. Arrows (or the arrow keys, or
// scrolling) turn the pages, the back button (or backspace) retraces your steps, and the house goes
// home. Clicking an item opens its entry. An entry's ribbon button bookmarks it, and bookmarks hang
// as tabs off the book's right edge. The book reopens where it was left.
public class CompendiumScreen extends Screen {
    public static final String CATEGORY_PREFIX = "category:";
    private static final String ENTRY_PREFIX = "entry:";
    private static final String HOME = "home";
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/gui/compendium/book.png");
    private static final int TEXTURE_SIZE_X = 512;
    private static final int TEXTURE_SIZE_Y = 256;
    private static final int BOOK_WIDTH = 292;
    private static final int BOOK_HEIGHT = 190;
    private static final int LEFT_PAGE_X = 18;
    private static final int RIGHT_PAGE_X = 158;
    private static final int PAGE_Y = 14;
    private static final int SEARCH_Y = 16;

    // Where the book is open: home, a category or an entry, and the spread
    private record Place(String view, int spread) {}

    private String view;
    private int spread;
    private final Deque<Place> history = new ArrayDeque<>();
    private List<Page> pages = List.of();
    private int left;
    private int top;
    private @Nullable EditBox search;
    private String query = "";
    // What the mouse was over last frame, for clicks
    private @Nullable String hoveredLink;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private @Nullable Runnable hoveredButton;

    private CompendiumScreen(String view, int spread) {
        super(Component.translatable("item.xincraft.compendium"));
        this.view = view;
        this.spread = spread;
    }

    // Opens the book where it was left
    public static void open() {
        String last = Bookmarks.lastView();
        boolean valid = last.equals(HOME) || last.startsWith(ENTRY_PREFIX) && CompendiumData.entry(last.substring(ENTRY_PREFIX.length())) != null
            || last.startsWith(CATEGORY_PREFIX) && CompendiumData.category(last.substring(CATEGORY_PREFIX.length())) != null;
        Minecraft.getInstance().gui.setScreen(new CompendiumScreen(valid ? last : HOME, valid ? Bookmarks.lastSpread() : 0));
    }

    // Opens the book at an entry, as the look-up key does
    public static void openEntry(String entry) {
        Minecraft.getInstance().gui.setScreen(new CompendiumScreen(ENTRY_PREFIX + entry, 0));
    }

    @Override
    protected void init() {
        left = (width - BOOK_WIDTH) / 2;
        top = (height - BOOK_HEIGHT) / 2;
        search = new EditBox(font, left + LEFT_PAGE_X, top + PAGE_Y + SEARCH_Y, Page.WIDTH, 12, search,
            Component.translatable("compendium.xincraft.search"));
        search.setHint(Component.translatable("compendium.xincraft.search").withStyle(style -> style.withColor(0xFF8A7A62)));
        search.setMaxLength(40);
        search.setResponder(value -> {
            query = value;
            spread = 0;
            rebuild();
        });
        addRenderableWidget(search);
        rebuild();
    }

    // Lays the current view's pages out afresh
    private void rebuild() {
        List<Page> laid = new ArrayList<>();
        if (view.startsWith(ENTRY_PREFIX)) {
            CompendiumData.Entry entry = CompendiumData.entry(view.substring(ENTRY_PREFIX.length()));
            if (entry != null) {
                for (int i = 0; i < entry.pages().size(); i++) {
                    Page.layout(laid, entry.pages().get(i), font, i == 0 ? Component.literal(entry.title()) : null);
                }
            }
        } else if (view.startsWith(CATEGORY_PREFIX)) {
            CompendiumData.Category category = CompendiumData.category(view.substring(CATEGORY_PREFIX.length()));
            if (category != null) {
                Page.layout(laid, new Page.TextSpec(category.description()), font, Component.literal(category.title()));
                // The overview first, then the sub-categories, then the rest of the entries
                List<CompendiumData.Entry> entries = CompendiumData.entriesIn(category.id());
                List<Page.Row> rows = new ArrayList<>();
                entries.stream().filter(entry -> entry.sort() < 0).forEach(entry -> rows.add(Page.Row.of(entry)));
                CompendiumData.children(category.id()).forEach(child -> rows.add(Page.Row.of(child)));
                entries.stream().filter(entry -> entry.sort() >= 0).forEach(entry -> rows.add(Page.Row.of(entry)));
                Page.addLinks(laid, null, Component.translatable("compendium.xincraft.entries").getString(), rows);
            }
        } else {
            // The search results run on over more spreads when they don't fit the right page
            List<CompendiumData.Entry> found = query.isBlank() ? CompendiumData.entriesIn("") : CompendiumData.search(query);
            List<Page.Row> rows = found.stream().map(Page.Row::of).toList();
            int fits = (Page.HEIGHT - 16) / Page.ROW;
            laid.add(homeLeft());
            laid.add(homeRight(rows.subList(0, Math.min(rows.size(), fits)), found.size()));
            if (rows.size() > fits) {
                Page.addLinks(laid, null, "", rows.subList(fits, rows.size()));
            }
        }
        if (laid.isEmpty()) {
            view = HOME;
            rebuild();
            return;
        }
        pages = laid;
        spread = Math.max(0, Math.min(spread, spreads() - 1));
        showSearch();
    }

    // The search box is on the home spread's first page only
    private void showSearch() {
        if (search != null) {
            search.setVisible(view.equals(HOME) && spread == 0);
        }
    }

    // The categories, each with its sub-categories under it
    private Page homeLeft() {
        List<Page.Row> rows = new ArrayList<>();
        for (CompendiumData.Category category : CompendiumData.children(null)) {
            rows.add(Page.Row.of(category));
            for (CompendiumData.Category child : CompendiumData.children(category.id())) {
                rows.add(new Page.Row(child.icon(), Component.literal("  " + child.title()), CATEGORY_PREFIX + child.id()));
            }
        }
        Component title = Component.translatable("item.xincraft.compendium");
        return (context, x, y) -> {
            Page.drawTitle(context, x, y, title);
            Page.drawRows(context, x, y + SEARCH_Y + 18, rows.subList(0, Math.min(rows.size(), 6)));
        };
    }

    // The search results, or the guide entries
    private Page homeRight(List<Page.Row> rows, int found) {
        Component heading = query.isBlank() ? Component.translatable("compendium.xincraft.guide")
            : Component.translatable("compendium.xincraft.results", found);
        return (context, x, y) -> {
            Page.drawTitle(context, x, y, heading);
            if (rows.isEmpty()) {
                context.centered(Component.translatable("compendium.xincraft.nothing_found"), x, y + 24, Page.FAINT_COLOR);
            }
            Page.drawRows(context, x, y + 16, rows);
        };
    }

    private int spreads() {
        return (pages.size() + 1) / 2;
    }

    private void go(String newView) {
        history.push(new Place(view, spread));
        view = newView;
        spread = 0;
        rebuild();
        playPageSound();
    }

    private void back() {
        if (!history.isEmpty()) {
            Place place = history.pop();
            view = place.view();
            spread = place.spread();
            rebuild();
            playPageSound();
        }
    }

    private void turn(int by) {
        int next = spread + by;
        if (next >= 0 && next < spreads()) {
            spread = next;
            showSearch();
            playPageSound();
        }
    }

    private void open(String target) {
        if (target.startsWith(CATEGORY_PREFIX)) {
            go(target);
        } else if (CompendiumData.entry(target) != null && !view.equals(ENTRY_PREFIX + target)) {
            go(ENTRY_PREFIX + target);
        }
    }

    private void playPageSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, BOOK_WIDTH, BOOK_HEIGHT, TEXTURE_SIZE_X, TEXTURE_SIZE_Y);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        ContextMap slots = SlotDisplayContext.fromLevel(minecraft.level);
        Page.Context context = new Page.Context(graphics, font, mouseX, mouseY, slots, System.currentTimeMillis());
        int first = spread * 2;
        if (first < pages.size()) {
            pages.get(first).render(context, left + LEFT_PAGE_X, top + PAGE_Y);
        }
        if (first + 1 < pages.size()) {
            pages.get(first + 1).render(context, left + RIGHT_PAGE_X, top + PAGE_Y);
        }
        hoveredButton = null;
        drawButtons(graphics, mouseX, mouseY);
        drawBookmarks(context);
        super.extractRenderState(graphics, mouseX, mouseY, a);

        hoveredLink = context.hoveredLink;
        hoveredStack = context.hoveredStack;
        if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hoveredStack, mouseX, mouseY);
        }
    }

    // A button from the book texture, darkened when hovered
    private void button(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int u, int v, int width, int height,
            Component tooltip, Runnable action) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, hovered ? v + 20 : v, width, height, TEXTURE_SIZE_X, TEXTURE_SIZE_Y);
        if (hovered) {
            hoveredButton = action;
            graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
        }
    }

    private void drawButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int buttonsY = top + BOOK_HEIGHT - 18;
        if (spread > 0) {
            button(graphics, mouseX, mouseY, left + 16, buttonsY, 18, 200, 18, 10, Component.translatable("compendium.xincraft.previous"), () -> turn(-1));
        }
        if (spread < spreads() - 1) {
            button(graphics, mouseX, mouseY, left + BOOK_WIDTH - 34, buttonsY, 0, 200, 18, 10, Component.translatable("compendium.xincraft.next"), () -> turn(1));
        }
        if (!history.isEmpty()) {
            button(graphics, mouseX, mouseY, left + BOOK_WIDTH / 2 - 22, buttonsY, 36, 200, 18, 10, Component.translatable("compendium.xincraft.back"), this::back);
        }
        if (!view.equals(HOME)) {
            button(graphics, mouseX, mouseY, left + BOOK_WIDTH / 2 + 5, buttonsY - 1, 54, 200, 14, 12, Component.translatable("compendium.xincraft.home"), () -> go(HOME));
        }
        if (view.startsWith(ENTRY_PREFIX)) {
            String entry = view.substring(ENTRY_PREFIX.length());
            boolean marked = Bookmarks.has(entry);
            button(graphics, mouseX, mouseY, left + BOOK_WIDTH - 26, top - 10, marked ? 82 : 70, 200, 10, 14,
                Component.translatable(marked ? "compendium.xincraft.unbookmark" : "compendium.xincraft.bookmark"), () -> {
                    if (!Bookmarks.toggle(entry)) {
                        minecraft.player.sendOverlayMessage(Component.translatable("compendium.xincraft.bookmarks_full", Bookmarks.MAX));
                    }
                });
        }
    }

    // The bookmarks, as ribbon tabs down the book's right edge
    private void drawBookmarks(Page.Context context) {
        List<String> bookmarks = Bookmarks.all();
        for (int i = 0; i < bookmarks.size(); i++) {
            CompendiumData.Entry entry = CompendiumData.entry(bookmarks.get(i));
            if (entry == null) {
                continue;
            }
            int x = left + BOOK_WIDTH - 4;
            int y = top + 20 + i * 20;
            boolean hovered = context.hovering(x, y, 24, 18);
            context.graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, hovered ? x + 2 : x, y, 96, 200, 24, 18, TEXTURE_SIZE_X, TEXTURE_SIZE_Y);
            context.graphics.item(entry.icon(), (hovered ? x + 2 : x) + 5, y + 1);
            if (hovered) {
                context.hoveredLink = entry.id();
                context.graphics.setTooltipForNextFrame(Component.literal(entry.title()), context.mouseX, context.mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (hoveredButton != null) {
            hoveredButton.run();
            return true;
        }
        if (hoveredLink != null) {
            open(hoveredLink);
            return true;
        }
        if (!hoveredStack.isEmpty()) {
            CompendiumData.Entry entry = CompendiumData.entryFor(hoveredStack.getItem());
            if (entry != null) {
                open(entry.id());
                return true;
            }
        }
        if (search != null && search.isFocused()) {
            search.setFocused(false);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean typing = search != null && search.isFocused() && search.isVisible();
        if (!typing) {
            switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
                    turn(-1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
                    turn(1);
                    return true;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    back();
                    return true;
                }
                default -> {
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        Bookmarks.setLast(view, spread);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
