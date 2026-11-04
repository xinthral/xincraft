package com.xincraft.xincraft.magic.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.xincraft.xincraft.magic.AltarRecipe;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * @brief The apothecary crucible and altar recipes the server sent, since 26.x doesn't send recipes
 *        to clients.
 *
 * Empty until the first sync, and replaced on /reload.
 */
public final class MagicRecipes {
    private static Map<EntityType<?>, CrucibleRecipe> crucibleByMob = Map.of();
    private static List<CrucibleRecipe> crucible = List.of();
    private static Map<Identifier, AltarRecipe> altarById = Map.of();
    private static List<AltarRecipe> altar = List.of();

    private MagicRecipes() {}

    public static void set(List<CrucibleRecipe> crucibleRecipes, Map<Identifier, AltarRecipe> altarRecipes) {
        Map<EntityType<?>, CrucibleRecipe> mobs = new HashMap<>();
        List<CrucibleRecipe> sorted = new ArrayList<>(crucibleRecipes);
        sorted.sort(Comparator.comparing(recipe -> recipe.mob().getDescription().getString()));
        for (CrucibleRecipe recipe : sorted) {
            mobs.putIfAbsent(recipe.mob(), recipe);
        }
        crucibleByMob = mobs;
        crucible = List.copyOf(sorted);
        altarById = Map.copyOf(altarRecipes);
        altar = altarRecipes.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .toList();
    }

    public static @Nullable CrucibleRecipe crucible(EntityType<?> mob) {
        return crucibleByMob.get(mob);
    }

    /**
     * @brief Every crucible recipe, sorted by mob name.
     *
     * @return The recipes, sorted by mob name.
     */
    public static List<CrucibleRecipe> crucible() {
        return crucible;
    }

    public static @Nullable AltarRecipe altar(Identifier id) {
        return altarById.get(id);
    }

    /**
     * @brief Every altar recipe, sorted by id.
     *
     * @return The recipes, sorted by id.
     */
    public static List<AltarRecipe> altar() {
        return altar;
    }
}
