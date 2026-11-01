package com.xincraft.xincraft.client.compendium;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.xincraft.xincraft.magic.AltarRecipe;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import com.xincraft.xincraft.network.CompendiumRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.jspecify.annotations.Nullable;

// The recipes the server sent for the compendium (network.CompendiumRecipesPacket), by recipe id, and
// the crucible recipes by mob, and the altar recipes by id. Empty until the first sync, so pages showing recipes say so.
public final class CompendiumRecipes {
    private static Map<Identifier, RecipeDisplay> byId = Map.of();
    private static Map<EntityType<?>, CrucibleRecipe> crucibleByMob = Map.of();
    private static List<CrucibleRecipe> crucible = List.of();
    private static Map<Identifier, AltarRecipe> altar = Map.of();

    private CompendiumRecipes() {}

    public static void set(List<CompendiumRecipesPacket.Shown> recipes, List<CrucibleRecipe> crucibleRecipes,
            List<CompendiumRecipesPacket.ShownAltar> altarRecipes) {
        Map<Identifier, RecipeDisplay> ids = new HashMap<>();
        for (CompendiumRecipesPacket.Shown shown : recipes) {
            ids.put(shown.id(), shown.display());
        }
        Map<EntityType<?>, CrucibleRecipe> mobs = new HashMap<>();
        List<CrucibleRecipe> sorted = new ArrayList<>(crucibleRecipes);
        sorted.sort(Comparator.comparing(recipe -> recipe.mob().getDescription().getString()));
        for (CrucibleRecipe recipe : sorted) {
            mobs.putIfAbsent(recipe.mob(), recipe);
        }
        byId = ids;
        crucibleByMob = mobs;
        crucible = List.copyOf(sorted);
        Map<Identifier, AltarRecipe> altarIds = new HashMap<>();
        altarRecipes.forEach(shown -> altarIds.put(shown.id(), shown.recipe()));
        altar = altarIds;
    }

    public static @Nullable RecipeDisplay get(Identifier id) {
        return byId.get(id);
    }

    public static @Nullable AltarRecipe altar(Identifier id) {
        return altar.get(id);
    }

    public static @Nullable CrucibleRecipe crucible(EntityType<?> mob) {
        return crucibleByMob.get(mob);
    }

    // Every crucible recipe, sorted by mob name
    public static List<CrucibleRecipe> crucible() {
        return crucible;
    }
}
