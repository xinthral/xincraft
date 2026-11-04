package com.xincraft.xincraft.compendium.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xincraft.xincraft.magic.AltarRecipe;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.common.CommonItems;
import com.xincraft.xincraft.magic.MagicItems;
import com.xincraft.xincraft.magic.client.MagicRecipes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import org.jspecify.annotations.Nullable;

/**
 * @brief A page of the compendium.
 *
 * Entries are written as specs, which are laid out into pages of the book's size when opened, so
 * long text runs on over as many pages as it needs. A laid-out page draws itself and reports what's
 * under the mouse through the Context.
 */
public interface Page {
    int WIDTH = 116;
    int HEIGHT = 150;
    int LINE = 10;
    int TEXT_COLOR = 0xFF3B2A1A;
    int TITLE_COLOR = 0xFF5A2E0C;
    int LINK_COLOR = 0xFF1F5AA6;
    int FAINT_COLOR = 0xFF8A7A62;
    String LINK_PREFIX = "compendium:";

    void render(Context context, int x, int y);

    /** @brief What's under the mouse this frame, gathered while drawing. */
    final class Context {
        public final GuiGraphicsExtractor graphics;
        public final Font font;
        public final int mouseX;
        public final int mouseY;
        public final ContextMap slots;
        public final long time;
        public ItemStack hoveredStack = ItemStack.EMPTY;
        public @Nullable String hoveredLink;

        public Context(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, ContextMap slots, long time) {
            this.graphics = graphics;
            this.font = font;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.slots = slots;
            this.time = time;
        }

        boolean hovering(int x, int y, int width, int height) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        /**
         * @brief An item in a slot frame; hovering it shows its tooltip, clicking it opens its
         *        entry.
         *
         * @param stack The item stack.
         * @param x The x position.
         * @param y The y position.
         * @param frame Whether to draw a slot frame behind it.
         */
        void item(ItemStack stack, int x, int y, boolean frame) {
            if (frame) {
                slot(x, y);
            }
            if (!stack.isEmpty()) {
                graphics.item(stack, x, y);
                graphics.itemDecorations(font, stack, x, y);
                if (hovering(x, y, 16, 16)) {
                    hoveredStack = stack;
                }
            }
        }

        void slot(int x, int y) {
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B7B66);
            graphics.fill(x - 1, y - 1, x + 16, y, 0xFF5E5040);
            graphics.fill(x - 1, y - 1, x, y + 16, 0xFF5E5040);
            graphics.fill(x, y, x + 16, y + 16, 0xFFC9B99B);
        }

        /**
         * @brief One of the slot's items, changing every second when there are several (e.g. a
         *        tag).
         *
         * @param display The slot to show.
         * @return The item to show now.
         */
        ItemStack cycle(SlotDisplay display) {
            List<ItemStack> stacks = display.resolveForStacks(slots);
            return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get((int) (time / 1000 % stacks.size()));
        }

        void centered(Component text, int x, int y, int color) {
            graphics.text(font, text, x + (WIDTH - font.width(text)) / 2, y, color, false);
        }

        /**
         * @brief A line of text, noting any link under the mouse.
         *
         * @param line The line to draw.
         * @param x The x position.
         * @param y The y position.
         */
        void line(FormattedText line, int x, int y) {
            graphics.text(font, Language.getInstance().getVisualOrder(line), x, y, TEXT_COLOR, false);
            if (mouseY >= y - 1 && mouseY < y + LINE - 1 && mouseX >= x) {
                Style style = styleAt(line, mouseX - x);
                if (style != null && style.getInsertion() != null && style.getInsertion().startsWith(LINK_PREFIX)) {
                    hoveredLink = style.getInsertion().substring(LINK_PREFIX.length());
                }
            }
        }

        private @Nullable Style styleAt(FormattedText line, int dx) {
            int[] left = {dx};
            return line.visit((style, text) -> {
                int width = font.width(FormattedText.of(text, style));
                if (left[0] < width) {
                    return Optional.of(style);
                }
                left[0] -= width;
                return Optional.empty();
            }, Style.EMPTY).orElse(null);
        }
    }

    // ---- Specs, as written in the entry files ----

    sealed interface Spec {
        /**
         * @brief The entries this page links to, for checking.
         *
         * @return The linked entries' ids.
         */
        List<String> links();

        static Spec parse(JsonObject json, String where) {
            String type = GsonHelper.getAsString(json, "type", "text");
            String text = GsonHelper.getAsString(json, "text", "");
            return switch (type) {
                case "text" -> new TextSpec(text);
                case "spotlight" -> {
                    List<Item> items = new ArrayList<>();
                    for (JsonElement item : GsonHelper.getAsJsonArray(json, "items")) {
                        CompendiumData.item(item.getAsString(), where).ifPresent(items::add);
                    }
                    yield new SpotlightSpec(List.copyOf(items), text);
                }
                case "recipe" -> new RecipeSpec(Identifier.parse(GsonHelper.getAsString(json, "recipe")), text);
                case "crucible" -> new CrucibleSpec(CompendiumData.entityType(GsonHelper.getAsString(json, "mob"), where).orElse(null), text);
                case "crucible_index" -> new CrucibleIndexSpec();
                case "altar" -> new AltarSpec(Identifier.parse(GsonHelper.getAsString(json, "recipe")), text);
                case "links" -> {
                    List<String> entries = new ArrayList<>();
                    for (JsonElement entry : GsonHelper.getAsJsonArray(json, "entries")) {
                        entries.add(entry.getAsString());
                    }
                    yield new LinksSpec(GsonHelper.getAsString(json, "title", ""), List.copyOf(entries));
                }
                case "image" -> new ImageSpec(Identifier.parse(GsonHelper.getAsString(json, "texture")),
                    GsonHelper.getAsInt(json, "width"), GsonHelper.getAsInt(json, "height"), text);
                default -> throw new IllegalArgumentException("unknown page type " + type);
            };
        }
    }

    record TextSpec(String text) implements Spec {
        @Override
        public List<String> links() {
            return linksIn(text);
        }
    }

    record SpotlightSpec(List<Item> items, String text) implements Spec {
        @Override
        public List<String> links() {
            return linksIn(text);
        }
    }

    record RecipeSpec(Identifier recipe, String text) implements Spec {
        @Override
        public List<String> links() {
            return linksIn(text);
        }
    }

    record AltarSpec(Identifier recipe, String text) implements Spec {
        @Override
        public List<String> links() {
            return linksIn(text);
        }
    }

    record CrucibleSpec(@Nullable EntityType<?> mob, String text) implements Spec {
        @Override
        public List<String> links() {
            return linksIn(text);
        }
    }

    record CrucibleIndexSpec() implements Spec {
        @Override
        public List<String> links() {
            return List.of();
        }
    }

    record LinksSpec(String title, List<String> entries) implements Spec {
        @Override
        public List<String> links() {
            return entries;
        }
    }

    record ImageSpec(Identifier texture, int width, int height, String text) implements Spec {
        @Override
        public List<String> links() {
            return linksIn(text);
        }
    }

    // ---- Text ----

    Pattern MARKUP = Pattern.compile("\\*\\*(.+?)\\*\\*|\\[\\[([a-z0-9_]+)(?:\\|(.+?))?]]");

    private static List<String> linksIn(String text) {
        List<String> links = new ArrayList<>();
        Matcher matcher = MARKUP.matcher(text);
        while (matcher.find()) {
            if (matcher.group(2) != null) {
                links.add(matcher.group(2));
            }
        }
        return links;
    }

    /**
     * @brief One paragraph's markup as styled text.
     *
     * @param text The markup.
     * @return The styled text.
     */
    private static Component parse(String text) {
        MutableComponent out = Component.empty();
        Matcher matcher = MARKUP.matcher(text);
        int at = 0;
        while (matcher.find()) {
            out.append(Component.literal(text.substring(at, matcher.start())));
            if (matcher.group(1) != null) {
                out.append(Component.literal(matcher.group(1)).withStyle(style -> style.withBold(true)));
            } else {
                String id = matcher.group(2);
                CompendiumData.Entry entry = CompendiumData.entry(id);
                String label = matcher.group(3) != null ? matcher.group(3) : entry != null ? entry.title() : id;
                out.append(Component.literal(label).withStyle(style -> style.withColor(LINK_COLOR).withUnderlined(true)
                    .withInsertion(LINK_PREFIX + id)));
            }
            at = matcher.end();
        }
        out.append(Component.literal(text.substring(at)));
        return out;
    }

    /**
     * @brief Text wrapped to the page, a null line for each gap between paragraphs.
     *
     * @param font The font to measure text with.
     * @param text The markup.
     * @return The wrapped lines.
     */
    static List<@Nullable FormattedText> wrap(Font font, String text) {
        List<@Nullable FormattedText> lines = new ArrayList<>();
        if (text.isBlank()) {
            return lines;
        }
        String[] paragraphs = text.strip().split("\n");
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            if (paragraph.isBlank()) {
                lines.add(null);
                continue;
            }
            lines.addAll(font.getSplitter().splitLines(parse(paragraph), WIDTH, Style.EMPTY));
        }
        return lines;
    }

    // ---- Laying out ----

    /**
     * @brief Lays the spec out onto the end of pages; the first page of an entry has its title on
     *        top.
     *
     * A text page carries on from the last page if that has room, so text flows on without gaps.
     *
     * @param pages The pages so far, which the new ones are added to.
     * @param spec The spec to lay out.
     * @param font The font to measure text with.
     * @param title The entry's title, for its first page, or null.
     */
    static void layout(List<Page> pages, Spec spec, Font font, @Nullable Component title) {
        int top = title != null ? 16 : 0;
        switch (spec) {
            case TextSpec text -> addText(pages, font, title, top, text.text(), null);
            case SpotlightSpec spotlight -> addText(pages, font, title, top + 26, spotlight.text(),
                (context, x, y) -> drawSpotlight(context, x, y + top, spotlight.items()));
            case RecipeSpec recipe -> addText(pages, font, title, top + 82, recipe.text(),
                (context, x, y) -> drawRecipe(context, x, y + top, recipe.recipe()));
            case CrucibleSpec crucible -> addText(pages, font, title, top + 80, crucible.text(),
                (context, x, y) -> drawCrucible(context, x, y + top, crucible.mob()));
            case AltarSpec altar -> addText(pages, font, title, top + 66, altar.text(),
                (context, x, y) -> drawAltar(context, x, y + top, altar.recipe()));
            case ImageSpec image -> addText(pages, font, title, top + Math.min(image.height(), HEIGHT - top) + 6, image.text(),
                (context, x, y) -> {
                    int width = Math.min(image.width(), WIDTH);
                    context.graphics.blit(RenderPipelines.GUI_TEXTURED, image.texture(), x + (WIDTH - width) / 2, y + top,
                        0.0F, 0.0F, width, image.height(), image.width(), image.height());
                });
            case LinksSpec links -> addLinks(pages, title, links.title(), links.entries().stream()
                .map(CompendiumData::entry).filter(entry -> entry != null).map(Row::of).toList());
            case CrucibleIndexSpec index -> addCrucibleIndex(pages, title);
        }
    }

    /** @brief Something drawn above a page's text. */
    @FunctionalInterface
    interface Header {
        void render(Context context, int x, int y);
    }

    /** @brief A page of text, under an optional title and header. */
    final class TextPage implements Page {
        private final @Nullable Component title;
        private final @Nullable Header header;
        private final int textTop;
        private final List<@Nullable FormattedText> lines = new ArrayList<>();

        TextPage(@Nullable Component title, @Nullable Header header, int textTop) {
            this.title = title;
            this.header = header;
            this.textTop = textTop;
        }

        int room() {
            return (HEIGHT - textTop) / LINE - lines.size();
        }

        @Override
        public void render(Context context, int x, int y) {
            if (title != null) {
                drawTitle(context, x, y, title);
            }
            if (header != null) {
                header.render(context, x, y);
            }
            int lineY = y + textTop;
            for (FormattedText line : lines) {
                if (line != null) {
                    context.line(line, x, lineY);
                }
                lineY += LINE;
            }
        }
    }

    /**
     * @brief Text after a header (and the entry's title), running on over more pages.
     *
     * Plain text carries on from the last page when it has a few lines to spare.
     *
     * @param pages The pages so far, which the new ones are added to.
     * @param font The font to measure text with.
     * @param title The entry's title, or null.
     * @param top How far down the first page the text starts.
     * @param text The text to add.
     * @param header What's drawn above the text, or null.
     */
    private static void addText(List<Page> pages, Font font, @Nullable Component title, int top, String text, @Nullable Header header) {
        List<@Nullable FormattedText> lines = wrap(font, text);
        TextPage page;
        if (title == null && header == null && !pages.isEmpty() && pages.get(pages.size() - 1) instanceof TextPage last
                && last.room() >= 3) {
            page = last;
            if (!page.lines.isEmpty()) {
                page.lines.add(null);
            }
        } else {
            page = new TextPage(title, header, top);
            pages.add(page);
        }
        for (FormattedText line : lines) {
            if (page.room() <= 0) {
                page = new TextPage(null, null, 0);
                pages.add(page);
            }
            if (line != null || !page.lines.isEmpty()) {
                page.lines.add(line);
            }
        }
    }

    /**
     * @brief A page's title, in bold if it fits and plain if not, cut short as a last resort.
     *
     * @param context The page's drawing context.
     * @param x The x position.
     * @param y The y position.
     * @param title The title to draw.
     */
    static void drawTitle(Context context, int x, int y, Component title) {
        Component bold = title.copy().withStyle(style -> style.withBold(true));
        Component shown = context.font.width(bold) <= WIDTH + 6 ? bold : clip(context, title, WIDTH + 6);
        context.centered(shown, x, y, TITLE_COLOR);
        context.graphics.fill(x + 8, y + 11, x + WIDTH - 8, y + 12, 0xFFB09A78);
    }

    private static void drawSpotlight(Context context, int x, int y, List<Item> items) {
        int width = items.size() * 22 - 4;
        int left = x + (WIDTH - width) / 2;
        for (int i = 0; i < items.size(); i++) {
            context.item(new ItemStack(items.get(i)), left + i * 22, y + 2, true);
        }
    }

    /**
     * @brief A 3×3 grid (or a furnace) with an arrow to the result, as the recipe book shows it.
     *
     * @param context The page's drawing context.
     * @param x The x position.
     * @param y The y position.
     * @param id The recipe's id.
     */
    private static void drawRecipe(Context context, int x, int y, Identifier id) {
        RecipeDisplay display = CompendiumRecipes.get(id);
        if (display == null) {
            context.centered(Component.translatable("compendium.xincraft.no_recipe"), x, y + 30, FAINT_COLOR);
            return;
        }
        ItemStack result = context.cycle(display.result());
        context.centered(result.getHoverName(), x, y, TITLE_COLOR);
        int gridX = x + 6;
        int gridY = y + 14;
        switch (display) {
            case ShapedCraftingRecipeDisplay shaped -> {
                for (int row = 0; row < shaped.height(); row++) {
                    for (int column = 0; column < shaped.width(); column++) {
                        context.item(context.cycle(shaped.ingredients().get(row * shaped.width() + column)),
                            gridX + column * 18, gridY + row * 18, true);
                    }
                }
                fillEmptySlots(context, gridX, gridY, shaped.width(), shaped.height());
            }
            case ShapelessCraftingRecipeDisplay shapeless -> {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = i < shapeless.ingredients().size() ? context.cycle(shapeless.ingredients().get(i)) : ItemStack.EMPTY;
                    context.item(stack, gridX + i % 3 * 18, gridY + i / 3 * 18, true);
                }
                context.graphics.text(context.font, Component.translatable("compendium.xincraft.shapeless"), gridX, gridY + 55, FAINT_COLOR, false);
            }
            case FurnaceRecipeDisplay furnace -> {
                context.item(context.cycle(furnace.ingredient()), gridX + 18, gridY, true);
                context.item(context.cycle(furnace.craftingStation()), gridX + 18, gridY + 18, false);
                context.item(context.cycle(furnace.fuel()), gridX + 18, gridY + 36, true);
            }
            case SmithingRecipeDisplay smithing -> {
                context.item(context.cycle(smithing.template()), gridX, gridY + 18, true);
                context.item(context.cycle(smithing.base()), gridX + 18, gridY + 18, true);
                context.item(context.cycle(smithing.addition()), gridX + 36, gridY + 18, true);
                context.item(context.cycle(smithing.craftingStation()), gridX + 18, gridY, false);
            }
            default -> context.centered(Component.translatable("compendium.xincraft.no_recipe"), x, gridY + 20, FAINT_COLOR);
        }
        drawArrow(context, x + 64, gridY + 19);
        context.item(result, x + 94, gridY + 18, true);
    }

    private static void fillEmptySlots(Context context, int gridX, int gridY, int width, int height) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row >= height || column >= width) {
                    context.slot(gridX + column * 18, gridY + row * 18);
                }
            }
        }
    }

    private static void drawArrow(Context context, int x, int y) {
        context.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("container/furnace/burn_progress"),
            24, 16, 0, 0, x, y, 24, 16);
    }

    /**
     * @brief The crucible's layout: a powder in each top corner, the catalyst between, and the mob
     *        below.
     *
     * @param context The page's drawing context.
     * @param x The x position.
     * @param y The y position.
     * @param mob The mob, or null if there's no recipe to show.
     */
    private static void drawCrucible(Context context, int x, int y, @Nullable EntityType<?> mob) {
        CrucibleRecipe recipe = mob == null ? null : MagicRecipes.crucible(mob);
        if (recipe == null) {
            context.centered(Component.translatable("compendium.xincraft.no_recipe"), x, y + 30, FAINT_COLOR);
            return;
        }
        context.centered(recipe.mob().getDescription(), x, y, TITLE_COLOR);
        int middle = x + WIDTH / 2 - 8;
        context.item(powder(recipe.elements().get(0)), middle - 26, y + 14, true);
        context.item(powder(recipe.elements().get(1)), middle + 26, y + 14, true);
        context.item(context.cycle(recipe.catalyst().display()), middle, y + 30, true);
        context.item(new ItemStack(CommonItems.MOB_FILTER.get()), middle, y + 48, true);
        if (recipe.baby()) {
            context.centered(Component.translatable("compendium.xincraft.crucible_baby"), x, y + 66, FAINT_COLOR);
        }
    }

    /**
     * @brief An altar transmutation: base and material, an arrow to the result, and the essence it
     *        draws.
     *
     * @param context The page's drawing context.
     * @param x The x position.
     * @param y The y position.
     * @param id The recipe's id.
     */
    private static void drawAltar(Context context, int x, int y, Identifier id) {
        AltarRecipe recipe = MagicRecipes.altar(id);
        if (recipe == null) {
            context.centered(Component.translatable("compendium.xincraft.no_recipe"), x, y + 30, FAINT_COLOR);
            return;
        }
        ItemStack result = new ItemStack(recipe.result());
        context.centered(result.getHoverName(), x, y, TITLE_COLOR);
        context.item(context.cycle(recipe.base().display()), x + 6, y + 14, true);
        recipe.material().ifPresent(material -> {
            ItemStack stack = context.cycle(material.display());
            context.item(stack.isEmpty() ? stack : stack.copyWithCount(recipe.materialCount()), x + 28, y + 14, true);
        });
        drawArrow(context, x + 62, y + 14);
        context.item(result, x + 94, y + 14, true);
        // Elements in order, so a recipe always reads the same way
        int at = 0;
        for (Element element : Element.values()) {
            Integer amount = recipe.essence().get(element);
            if (amount != null) {
                int costX = x + 6 + at * 54;
                context.item(powder(element), costX, y + 36, false);
                context.graphics.text(context.font, Component.literal("\u00d7" + amount), costX + 18, y + 40, TEXT_COLOR, false);
                at++;
            }
        }
    }

    private static ItemStack powder(Element element) {
        return new ItemStack(MagicItems.ELEMENT_POWDERS.get(element).get());
    }

    // ---- Lists ----

    /**
     * @brief A row in a list: an icon and a name, opening an entry or category when clicked.
     *
     * @param icon The item shown as its icon.
     * @param name The name to show.
     * @param target The entry or category it opens.
     */
    record Row(ItemStack icon, Component name, String target) {
        static Row of(CompendiumData.Entry entry) {
            return new Row(entry.icon(), Component.literal(entry.title()), entry.id());
        }

        static Row of(CompendiumData.Category category) {
            return new Row(category.icon(), Component.literal(category.title()).withStyle(style -> style.withBold(true)),
                CompendiumScreen.CATEGORY_PREFIX + category.id());
        }
    }

    int ROW = 18;

    static void addLinks(List<Page> pages, @Nullable Component title, String heading, List<Row> rows) {
        int at = 0;
        boolean first = true;
        do {
            int top = (first && title != null ? 16 : 0) + (first && !heading.isEmpty() ? LINE + 2 : 0);
            int count = Math.max(1, (HEIGHT - top) / ROW);
            List<Row> pageRows = rows.subList(at, Math.min(rows.size(), at + count));
            at += pageRows.size();
            Component pageTitle = first ? title : null;
            String pageHeading = first ? heading : "";
            pages.add((context, x, y) -> {
                if (pageTitle != null) {
                    drawTitle(context, x, y, pageTitle);
                }
                if (!pageHeading.isEmpty()) {
                    context.graphics.text(context.font, Component.literal(pageHeading).withStyle(style -> style.withItalic(true)),
                        x, y + top - LINE - 2, FAINT_COLOR, false);
                }
                drawRows(context, x, y + top, pageRows);
            });
            first = false;
        } while (at < rows.size());
    }

    static void drawRows(Context context, int x, int y, List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowY = y + i * ROW;
            boolean hovered = context.hovering(x, rowY, WIDTH, ROW - 1);
            if (hovered) {
                context.graphics.fill(x - 1, rowY - 1, x + WIDTH + 1, rowY + ROW - 2, 0x30804020);
                context.hoveredLink = row.target();
            }
            context.graphics.item(row.icon(), x, rowY);
            Component shown = clip(context, row.name(), WIDTH - 19);
            if (hovered && shown != row.name()) {
                context.graphics.setTooltipForNextFrame(row.name(), context.mouseX, context.mouseY);
            }
            context.graphics.text(context.font, shown, x + 19, rowY + 4, hovered ? LINK_COLOR : TEXT_COLOR, false);
        }
    }

    /**
     * @brief The text cut short with an ellipsis to fit the width, or the text itself if it already
     *        fits.
     *
     * @param context The page's drawing context.
     * @param text The text to fit.
     * @param width The width available.
     * @return The text, shortened if needed.
     */
    static Component clip(Context context, Component text, int width) {
        if (context.font.width(text) <= width) {
            return text;
        }
        String clipped = context.font.plainSubstrByWidth(text.getString(), width - context.font.width("\u2026"));
        return Component.literal(clipped.stripTrailing() + "\u2026").withStyle(text.getStyle());
    }

    /**
     * @brief Every crucible recipe, a row each: its powders, catalyst and mob.
     *
     * @param pages The pages so far, which the new ones are added to.
     * @param title The entry's title, for its first page, or null.
     */
    private static void addCrucibleIndex(List<Page> pages, @Nullable Component title) {
        List<CrucibleRecipe> recipes = MagicRecipes.crucible();
        if (recipes.isEmpty()) {
            pages.add((context, x, y) -> {
                if (title != null) {
                    drawTitle(context, x, y, title);
                }
                context.centered(Component.translatable("compendium.xincraft.no_recipe"), x, y + 30, FAINT_COLOR);
            });
            return;
        }
        int at = 0;
        boolean first = true;
        while (at < recipes.size()) {
            int top = first && title != null ? 16 : 0;
            int count = (HEIGHT - top) / ROW;
            List<CrucibleRecipe> pageRecipes = recipes.subList(at, Math.min(recipes.size(), at + count));
            at += pageRecipes.size();
            Component pageTitle = first ? title : null;
            pages.add((context, x, y) -> {
                if (pageTitle != null) {
                    drawTitle(context, x, y, pageTitle);
                }
                for (int i = 0; i < pageRecipes.size(); i++) {
                    CrucibleRecipe recipe = pageRecipes.get(i);
                    int rowY = y + top + i * ROW;
                    context.item(powder(recipe.elements().get(0)), x, rowY, false);
                    context.item(powder(recipe.elements().get(1)), x + 14, rowY, false);
                    context.item(context.cycle(recipe.catalyst().display()), x + 31, rowY, false);
                    Component baby = Component.literal(" ✿").withStyle(style -> style.withColor(0xFF4F9A3A));
                    int room = WIDTH - 49 - (recipe.baby() ? context.font.width(baby) : 0);
                    Component name = clip(context, recipe.mob().getDescription(), room);
                    if (name != recipe.mob().getDescription() && context.hovering(x + 49, rowY, room, ROW - 1)) {
                        context.graphics.setTooltipForNextFrame(recipe.mob().getDescription(), context.mouseX, context.mouseY);
                    }
                    if (recipe.baby()) {
                        name = name.copy().append(baby);
                    }
                    context.graphics.text(context.font, name, x + 49, rowY + 4, TEXT_COLOR, false);
                }
            });
            first = false;
        }
    }
}
