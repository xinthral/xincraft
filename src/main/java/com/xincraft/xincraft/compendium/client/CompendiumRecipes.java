package com.xincraft.xincraft.compendium.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.xincraft.xincraft.compendium.CompendiumRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.jspecify.annotations.Nullable;

/**
 * @brief The recipes the server sent for the compendium, by recipe id.
 *
 * Empty until the first sync, so pages showing recipes say so.
 *
 * @see compendium.CompendiumRecipesPacket
 */
public final class CompendiumRecipes {
    private static Map<Identifier, RecipeDisplay> byId = Map.of();

    private CompendiumRecipes() {}

    public static void set(List<CompendiumRecipesPacket.Shown> recipes) {
        Map<Identifier, RecipeDisplay> ids = new HashMap<>();
        for (CompendiumRecipesPacket.Shown shown : recipes) {
            ids.put(shown.id(), shown.display());
        }
        byId = ids;
    }

    public static @Nullable RecipeDisplay get(Identifier id) {
        return byId.get(id);
    }
}
