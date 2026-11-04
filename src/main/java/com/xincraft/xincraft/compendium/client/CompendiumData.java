package com.xincraft.xincraft.compendium.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * @brief The compendium's contents, loaded from the client resources.
 *
 * Each language falls back to en_us for any file it lacks. Contents reload with the resource packs
 * (F3+T). Mistakes such as bad JSON or unknown items are logged rather than failing.
 */
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class CompendiumData implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "compendium";
    private static final String FALLBACK = "en_us";

    /**
     * @brief Icons are kept as items, since stacks can't be made before a world's components are
     *        bound.
     *
     * @param id The category's id.
     * @param title The category's title.
     * @param iconItem The item shown as its icon.
     * @param parent The parent category, or null at the top level.
     * @param sort Its position among its siblings.
     * @param description The overview text.
     */
    public record Category(String id, String title, Item iconItem, @Nullable String parent, int sort, String description) {
        public ItemStack icon() {
            return new ItemStack(iconItem);
        }
    }

    public record Entry(String id, String title, Item iconItem, String category, List<String> also, int sort,
            List<Item> items, List<Page.Spec> pages) {
        public ItemStack icon() {
            return new ItemStack(iconItem);
        }

        public boolean isIn(String categoryId) {
            return category.equals(categoryId) || also.contains(categoryId);
        }
    }

    private static Map<String, Category> categories = Map.of();
    private static Map<String, Entry> entries = Map.of();
    private static Map<Item, Entry> byItem = Map.of();
    private static Map<Block, Entry> byBlock = Map.of();

    private CompendiumData() {}

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new CompendiumData());
    }

    public static @Nullable Category category(String id) {
        return categories.get(id);
    }

    public static @Nullable Entry entry(String id) {
        return entries.get(id);
    }

    public static @Nullable Entry entryFor(Item item) {
        return byItem.get(item);
    }

    public static @Nullable Entry entryFor(Block block) {
        Entry entry = byBlock.get(block);
        return entry != null ? entry : byItem.get(block.asItem());
    }

    /**
     * @brief The categories under the given one (null for the top level), in order.
     *
     * @param parent The parent category, or null for the top level.
     * @return The categories, in order.
     */
    public static List<Category> children(@Nullable String parent) {
        return categories.values().stream()
            .filter(category -> parent == null ? category.parent() == null : parent.equals(category.parent()))
            .sorted(Comparator.comparingInt(Category::sort).thenComparing(Category::title))
            .toList();
    }

    /**
     * @brief The entries in a category ("" for the home page), in order.
     *
     * @param category The category, or "" for the home page.
     * @return The entries, in order.
     */
    public static List<Entry> entriesIn(String category) {
        return entries.values().stream()
            .filter(entry -> entry.isIn(category))
            .sorted(Comparator.comparingInt(Entry::sort).thenComparing(Entry::title))
            .toList();
    }

    /**
     * @brief Searches the entries by title and item name.
     *
     * @param query The text to look for.
     * @return The entries whose title, or one of whose items' names, contains the query.
     */
    public static List<Entry> search(String query) {
        String needle = query.trim().toLowerCase();
        if (needle.isEmpty()) {
            return List.of();
        }
        return entries.values().stream()
            .filter(entry -> entry.title().toLowerCase().contains(needle) || entry.items().stream()
                .anyMatch(item -> new ItemStack(item).getHoverName().getString().toLowerCase().contains(needle)))
            .sorted(Comparator.comparing(Entry::title))
            .toList();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        Map<String, Category> newCategories = new HashMap<>();
        for (JsonFile file : load(manager, "categories", language)) {
            try {
                JsonObject json = file.json();
                newCategories.put(file.id(), new Category(file.id(), GsonHelper.getAsString(json, "title"),
                    icon(json, file), json.has("parent") ? GsonHelper.getAsString(json, "parent") : null,
                    GsonHelper.getAsInt(json, "sort", 0), GsonHelper.getAsString(json, "description", "")));
            } catch (RuntimeException e) {
                LOGGER.error("Compendium category {}: {}", file.location(), e.getMessage());
            }
        }
        Map<String, Entry> newEntries = new HashMap<>();
        Map<Item, Entry> newByItem = new HashMap<>();
        Map<Block, Entry> newByBlock = new HashMap<>();
        for (JsonFile file : load(manager, "entries", language)) {
            try {
                JsonObject json = file.json();
                List<Page.Spec> pages = new ArrayList<>();
                for (JsonElement page : GsonHelper.getAsJsonArray(json, "pages")) {
                    pages.add(Page.Spec.parse(GsonHelper.convertToJsonObject(page, "page"), file.location().toString()));
                }
                List<Item> items = new ArrayList<>();
                for (JsonElement item : GsonHelper.getAsJsonArray(json, "items", new JsonArray())) {
                    item(item.getAsString(), file.location().toString()).ifPresent(items::add);
                }
                List<String> also = new ArrayList<>();
                for (JsonElement category : GsonHelper.getAsJsonArray(json, "also", new JsonArray())) {
                    also.add(category.getAsString());
                }
                Entry entry = new Entry(file.id(), GsonHelper.getAsString(json, "title"), icon(json, file),
                    GsonHelper.getAsString(json, "category", ""), List.copyOf(also), GsonHelper.getAsInt(json, "sort", 0),
                    List.copyOf(items), List.copyOf(pages));
                newEntries.put(file.id(), entry);
                for (Item item : items) {
                    newByItem.putIfAbsent(item, entry);
                }
                for (JsonElement block : GsonHelper.getAsJsonArray(json, "blocks", new JsonArray())) {
                    Identifier key = Identifier.tryParse(block.getAsString());
                    Optional<Block> found = key == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(key);
                    if (found.isPresent()) {
                        newByBlock.putIfAbsent(found.get(), entry);
                    } else {
                        LOGGER.warn("Compendium {}: unknown block {}", file.location(), block.getAsString());
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.error("Compendium entry {}: {}", file.location(), e.getMessage());
            }
        }
        for (Entry entry : newEntries.values()) {
            for (String category : concat(entry.category(), entry.also())) {
                if (!category.isEmpty() && !newCategories.containsKey(category)) {
                    LOGGER.warn("Compendium entry {} is in unknown category {}", entry.id(), category);
                }
            }
            for (Page.Spec page : entry.pages()) {
                for (String link : page.links()) {
                    if (!newEntries.containsKey(link)) {
                        LOGGER.warn("Compendium entry {} links to unknown entry {}", entry.id(), link);
                    }
                }
            }
        }
        categories = Map.copyOf(newCategories);
        entries = Map.copyOf(newEntries);
        byItem = Map.copyOf(newByItem);
        byBlock = Map.copyOf(newByBlock);
        LOGGER.info("Loaded {} compendium categories and {} entries", categories.size(), entries.size());
    }

    private static List<String> concat(String first, List<String> rest) {
        List<String> all = new ArrayList<>(rest);
        all.add(0, first);
        return all;
    }

    private record JsonFile(String id, Identifier location, JsonObject json) {}

    /**
     * @brief Loads every file of one kind for a language, filling in any missing ones from en_us.
     *
     * @param manager The resource manager to read from.
     * @param kind The kind of file: categories or entries.
     * @param language The language code.
     * @return The loaded files, with ids taken from their file names.
     */
    private static List<JsonFile> load(ResourceManager manager, String kind, String language) {
        Map<String, JsonFile> files = new LinkedHashMap<>();
        for (String lang : language.equals(FALLBACK) ? List.of(FALLBACK) : List.of(language, FALLBACK)) {
            String directory = ROOT + "/" + lang + "/" + kind;
            for (Map.Entry<Identifier, Resource> found : manager.listResources(directory, path -> path.getPath().endsWith(".json")).entrySet()) {
                String path = found.getKey().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                if (files.containsKey(id)) {
                    continue;
                }
                try (Reader reader = found.getValue().openAsReader()) {
                    files.put(id, new JsonFile(id, found.getKey(), JsonParser.parseReader(reader).getAsJsonObject()));
                } catch (Exception e) {
                    LOGGER.error("Couldn't read compendium file {}: {}", found.getKey(), e.getMessage());
                }
            }
        }
        return List.copyOf(files.values());
    }

    private static Item icon(JsonObject json, JsonFile file) {
        return item(GsonHelper.getAsString(json, "icon", "minecraft:book"), file.location().toString()).orElse(Items.BOOK);
    }

    /**
     * @brief Looks up an item by id, logging it if there isn't one.
     *
     * @param id The item's id.
     * @param where Where the id came from, for the log.
     * @return The item, if it exists.
     */
    static Optional<Item> item(String id, String where) {
        Identifier key = Identifier.tryParse(id);
        Optional<Item> item = key == null ? Optional.empty()
            : BuiltInRegistries.ITEM.getOptional(key).filter(found -> found != Items.AIR);
        if (item.isEmpty()) {
            LOGGER.warn("Compendium {}: unknown item {}", where, id);
        }
        return item;
    }

    static Optional<EntityType<?>> entityType(String id, String where) {
        Identifier key = Identifier.tryParse(id);
        Optional<EntityType<?>> type = key == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(key);
        if (type.isEmpty()) {
            LOGGER.warn("Compendium {}: unknown mob {}", where, id);
        }
        return type;
    }
}
