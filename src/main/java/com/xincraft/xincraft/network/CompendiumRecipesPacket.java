package com.xincraft.xincraft.network;

import java.util.ArrayList;
import java.util.List;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.client.compendium.CompendiumRecipes;
import com.xincraft.xincraft.magic.CrucibleRecipe;
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

// Sends the compendium (client.compendium.CompendiumScreen) the recipes it shows, since 26.x doesn't
// send recipes to clients: how to make each of the mod's items (as the recipe book would show it),
// and every apothecary crucible recipe. Sent on login and /reload, so data packs that change them
// show in the book.
public record CompendiumRecipesPacket(List<Shown> recipes, List<CrucibleRecipe> crucible) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CompendiumRecipesPacket> STREAM_CODEC = StreamCodec.composite(
        Shown.STREAM_CODEC.apply(ByteBufCodecs.list()), CompendiumRecipesPacket::recipes,
        CrucibleRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()), CompendiumRecipesPacket::crucible,
        CompendiumRecipesPacket::new);

    // A recipe as the recipe book would show it
    public record Shown(Identifier id, RecipeDisplay display) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Shown> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Shown::id,
            RecipeDisplay.STREAM_CODEC, Shown::display,
            Shown::new);
    }

    // The mod's own recipes, and any recipe that makes one of its items
    public static CompendiumRecipesPacket collect(ServerLevel level) {
        List<Shown> recipes = new ArrayList<>();
        List<CrucibleRecipe> crucible = new ArrayList<>();
        var context = SlotDisplayContext.fromLevel(level);
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            if (holder.value() instanceof CrucibleRecipe recipe) {
                crucible.add(recipe);
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
        return new CompendiumRecipesPacket(recipes, crucible);
    }

    public void handle(CustomPayloadEvent.Context context) {
        CompendiumRecipes.set(recipes, crucible);
        context.setPacketHandled(true);
    }
}
