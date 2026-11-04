package com.xincraft.xincraft.compendium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.compendium.client.CompendiumRecipes;
import com.xincraft.xincraft.magic.AltarRecipe;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import com.xincraft.xincraft.magic.client.MagicRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * @brief Sends the compendium the recipes it shows, since 26.x doesn't send recipes to clients.
 *
 * How to make each of the mod's items (as the recipe book would show it), and every apothecary
 * crucible and altar recipe.
 *
 * Sent on login and /reload, so data packs that change them show in the book.
 *
 * @param recipes How to make each of the mod's items.
 * @param crucible Every apothecary crucible recipe.
 * @param altar Every apothecary altar recipe.
 * @see compendium.client.CompendiumScreen
 */
public record CompendiumRecipesPacket(List<Shown> recipes, List<CrucibleRecipe> crucible, List<ShownAltar> altar) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CompendiumRecipesPacket> STREAM_CODEC = StreamCodec.composite(
        Shown.STREAM_CODEC.apply(ByteBufCodecs.list()), CompendiumRecipesPacket::recipes,
        CrucibleRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()), CompendiumRecipesPacket::crucible,
        ShownAltar.STREAM_CODEC.apply(ByteBufCodecs.list()), CompendiumRecipesPacket::altar,
        CompendiumRecipesPacket::new);

    /**
     * @brief A recipe as the recipe book would show it.
     *
     * @param id The recipe's id.
     * @param display How the recipe book would show it.
     */
    public record Shown(Identifier id, RecipeDisplay display) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Shown> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Shown::id,
            RecipeDisplay.STREAM_CODEC, Shown::display,
            Shown::new);
    }

    /**
     * @brief An altar transmutation, with the id the book's pages name it by.
     *
     * @param id The id the book's pages name it by.
     * @param recipe The recipe.
     */
    public record ShownAltar(Identifier id, AltarRecipe recipe) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShownAltar> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ShownAltar::id,
            AltarRecipe.STREAM_CODEC, ShownAltar::recipe,
            ShownAltar::new);
    }

    /**
     * @brief Gathers the mod's own recipes, and any recipe that makes one of its items.
     *
     * @param level The level to work in.
     * @return The packet to send.
     */
    public static CompendiumRecipesPacket collect(ServerLevel level) {
        List<Shown> recipes = new ArrayList<>();
        List<CrucibleRecipe> crucible = new ArrayList<>();
        List<ShownAltar> altar = new ArrayList<>();
        var context = SlotDisplayContext.fromLevel(level);
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            if (holder.value() instanceof CrucibleRecipe recipe) {
                crucible.add(recipe);
                continue;
            }
            if (holder.value() instanceof AltarRecipe recipe) {
                altar.add(new ShownAltar(id, recipe));
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                ItemStack result = display.result().resolveForFirstStack(context);
                boolean ours = id.getNamespace().equals(XincraftMod.MODID)
                    || !result.isEmpty() && BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(XincraftMod.MODID);
                if (ours) {
                    recipes.add(new Shown(id, display));
                    break;
                }
            }
        }
        return new CompendiumRecipesPacket(recipes, crucible, altar);
    }

    public void handle(CustomPayloadEvent.Context context) {
        CompendiumRecipes.set(recipes);
        Map<Identifier, AltarRecipe> altarById = new HashMap<>();
        altar.forEach(shown -> altarById.put(shown.id(), shown.recipe()));
        MagicRecipes.set(crucible, altarById);
        context.setPacketHandled(true);
    }
}
