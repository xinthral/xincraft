package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, XincraftMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, XincraftMod.MODID);
    public static final DeferredRegister<RecipeBookCategory> RECIPE_BOOK_CATEGORIES =
        DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, XincraftMod.MODID);

    // The apothecary crucible's mob inscriptions (magic.CrucibleRecipe)
    public static final RegistryObject<RecipeType<CrucibleRecipe>> CRUCIBLE = RECIPE_TYPES.register("crucible",
        () -> RecipeType.simple(Identifier.fromNamespaceAndPath(XincraftMod.MODID, "crucible")));
    public static final RegistryObject<RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER = RECIPE_SERIALIZERS.register("crucible",
        () -> new RecipeSerializer<>(CrucibleRecipe.MAP_CODEC, CrucibleRecipe.STREAM_CODEC));
    public static final RegistryObject<RecipeBookCategory> CRUCIBLE_BOOK_CATEGORY = RECIPE_BOOK_CATEGORIES.register("crucible",
        RecipeBookCategory::new);

    private ModRecipes() {}
}
