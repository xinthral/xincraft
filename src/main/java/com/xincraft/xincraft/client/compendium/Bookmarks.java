package com.xincraft.xincraft.client.compendium;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

// The compendium's bookmarks and the page it was last left open at, kept on this computer in
// config/xincraft-compendium.json so they last between sessions and worlds
public final class Bookmarks {
    public static final int MAX = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("xincraft-compendium.json");

    private static final List<String> bookmarks = new ArrayList<>();
    private static String lastView = "";
    private static int lastSpread;
    private static boolean loaded;

    private Bookmarks() {}

    public static List<String> all() {
        load();
        return List.copyOf(bookmarks);
    }

    public static boolean has(String entry) {
        load();
        return bookmarks.contains(entry);
    }

    // Adds or removes a bookmark; false if there's no room for another
    public static boolean toggle(String entry) {
        load();
        if (!bookmarks.remove(entry)) {
            if (bookmarks.size() >= MAX) {
                return false;
            }
            bookmarks.add(entry);
        }
        save();
        return true;
    }

    // Where the book was left open: a view key (see CompendiumScreen.View) and the spread
    public static String lastView() {
        load();
        return lastView;
    }

    public static int lastSpread() {
        load();
        return lastSpread;
    }

    public static void setLast(String view, int spread) {
        load();
        lastView = view;
        lastSpread = spread;
        save();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            GsonHelper.getAsJsonArray(json, "bookmarks", new JsonArray()).forEach(entry -> bookmarks.add(entry.getAsString()));
            lastView = GsonHelper.getAsString(json, "last", "");
            lastSpread = GsonHelper.getAsInt(json, "spread", 0);
        } catch (Exception e) {
            LOGGER.warn("Couldn't read {}: {}", FILE, e.getMessage());
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        JsonArray list = new JsonArray();
        bookmarks.forEach(list::add);
        json.add("bookmarks", list);
        json.addProperty("last", lastView);
        json.addProperty("spread", lastSpread);
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            LOGGER.warn("Couldn't save {}: {}", FILE, e.getMessage());
        }
    }
}
