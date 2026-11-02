package com.xincraft.xincraft.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xincraft.xincraft.common.CommonComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

// An apothecary altar transmutation (data/<namespace>/recipe/altar/*.json): the altar burns its fuel
// for time ticks, then draws the essence from the flora in range, uses up the material, and turns the
// base into the result. The base's components (enchantments, trims, name, damage) carry over, as with
// a netherite upgrade. A base belongs to one recipe, which the material then has to match; a recipe
// with no material ignores that slot. With store_material, the material goes into the result's
// xincraft:golem_sword component as it is, enchantments and wear included.
//
//   {"type": "xincraft:altar_transmutation", "base": "xincraft:bronze_boots",
//    "material": "xincraft:lonsdaleite_shard", "material_count": 4,
//    "essence": {"aer": 24, "vita": 24}, "time": 400, "result": "xincraft:lonsdaleite_boots"}
public record AltarRecipe(Ingredient base, Optional<Ingredient> material, int materialCount, Map<Element, Integer> essence,
        int time, Item result, boolean storeMaterial) implements Recipe<AltarInput> {
    public static final MapCodec<AltarRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("base").forGetter(AltarRecipe::base),
            Ingredient.CODEC.optionalFieldOf("material").forGetter(AltarRecipe::material),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("material_count", 1).forGetter(AltarRecipe::materialCount),
            Codec.unboundedMap(Element.CODEC, ExtraCodecs.POSITIVE_INT).optionalFieldOf("essence", Map.of()).forGetter(AltarRecipe::essence),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(AltarRecipe::time),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(AltarRecipe::result),
            Codec.BOOL.optionalFieldOf("store_material", false).forGetter(AltarRecipe::storeMaterial))
        .apply(i, AltarRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AltarRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, AltarRecipe::base,
        ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), AltarRecipe::material,
        ByteBufCodecs.VAR_INT, AltarRecipe::materialCount,
        ByteBufCodecs.<RegistryFriendlyByteBuf, Element, Integer, Map<Element, Integer>>map(HashMap::new,
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(ordinal -> Element.values()[ordinal], Element::ordinal),
            ByteBufCodecs.VAR_INT.cast()), AltarRecipe::essence,
        ByteBufCodecs.VAR_INT, AltarRecipe::time,
        ByteBufCodecs.registry(Registries.ITEM), AltarRecipe::result,
        ByteBufCodecs.BOOL, AltarRecipe::storeMaterial,
        AltarRecipe::new);

    // Only the base: it says which recipe the altar is working on, even before the material's in
    @Override
    public boolean matches(AltarInput input, Level level) {
        return base.test(input.base());
    }

    public boolean hasMaterial(AltarInput input) {
        return material.isEmpty() || material.get().test(input.material()) && input.material().getCount() >= materialCount;
    }

    public boolean usesMaterial(ItemStack stack) {
        return material.isPresent() && material.get().test(stack);
    }

    @Override
    public ItemStack assemble(AltarInput input) {
        ItemStack made = input.base().transmuteCopy(result, 1);
        if (storeMaterial) {
            made.set(CommonComponents.GOLEM_SWORD.get(), input.material().copyWithCount(1));
        }
        return made;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<AltarRecipe> getSerializer() {
        return ModRecipes.ALTAR_TRANSMUTATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<AltarRecipe> getType() {
        return ModRecipes.ALTAR_TRANSMUTATION.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipes.ALTAR_TRANSMUTATION_BOOK_CATEGORY.get();
    }
}
