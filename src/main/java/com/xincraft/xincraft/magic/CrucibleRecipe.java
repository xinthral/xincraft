package com.xincraft.xincraft.magic;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xincraft.xincraft.item.ElementPowderItem;
import com.xincraft.xincraft.item.MobFilterContents;
import com.xincraft.xincraft.item.MobFilterItem;
import com.xincraft.xincraft.registry.ModDataComponents;
import com.xincraft.xincraft.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// An apothecary crucible recipe (data/<namespace>/recipe/crucible/*.json): two elemental powders and
// a catalyst that together name a mob, inscribed onto the mob filter in the crucible. Vita stands for
// youth: no recipe uses it, and for a mob that has babies, swapping either powder for Vita inscribes
// the baby instead. In a recipe with Umbra, though, Vita has to take the Umbra's place, life driving
// out death; that keeps undead kin that share a catalyst (zombie and husk) apart. The powders may go
// in either top slot.
//
//   {"type": "xincraft:crucible", "elements": ["aer", "terra"], "catalyst": "minecraft:feather",
//    "mob": "minecraft:chicken", "baby": true}
public record CrucibleRecipe(List<Element> elements, Ingredient catalyst, EntityType<?> mob, boolean baby)
        implements Recipe<CrucibleInput> {
    public static final Element YOUTH = Element.VITA;
    public static final Element DEATH = Element.UMBRA;

    private static final Codec<List<Element>> ELEMENTS_CODEC = Element.CODEC.listOf().validate(elements ->
        elements.size() != 2 ? DataResult.error(() -> "A crucible recipe takes exactly 2 elements")
            : elements.contains(YOUTH) ? DataResult.error(() -> "Vita marks a baby, so a crucible recipe can't use it")
            : DataResult.success(elements));

    public static final MapCodec<CrucibleRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ELEMENTS_CODEC.fieldOf("elements").forGetter(CrucibleRecipe::elements),
            Ingredient.CODEC.fieldOf("catalyst").forGetter(CrucibleRecipe::catalyst),
            EntityType.CODEC.fieldOf("mob").forGetter(CrucibleRecipe::mob),
            Codec.BOOL.optionalFieldOf("baby", false).forGetter(CrucibleRecipe::baby))
        .apply(i, CrucibleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.<Element>map(ordinal -> Element.values()[ordinal], Element::ordinal).apply(ByteBufCodecs.list()),
        CrucibleRecipe::elements,
        Ingredient.CONTENTS_STREAM_CODEC, CrucibleRecipe::catalyst,
        EntityType.STREAM_CODEC, CrucibleRecipe::mob,
        ByteBufCodecs.BOOL, CrucibleRecipe::baby,
        CrucibleRecipe::new);

    @Override
    public boolean matches(CrucibleInput input, Level level) {
        return catalyst.test(input.catalyst()) && age(input) != null;
    }

    // Whether the powders in the crucible inscribe the adult (false) or the baby (true); null if
    // they don't make this mob at all
    public @Nullable Boolean age(CrucibleInput input) {
        Element left = element(input.left());
        Element right = element(input.right());
        if (left == null || right == null) {
            return null;
        }
        Element first = elements.get(0);
        Element second = elements.get(1);
        if (left == first && right == second || left == second && right == first) {
            return false;
        }
        if (baby) {
            for (int i = 0; i < 2; i++) {
                Element swapped = elements.get(i);
                Element kept = elements.get(1 - i);
                boolean allowed = !elements.contains(DEATH) || swapped == DEATH;
                if (allowed && (left == YOUTH && right == kept || right == YOUTH && left == kept)) {
                    return true;
                }
            }
        }
        return null;
    }

    // The entry this inscribes onto the filter in the crucible, or null if the powders don't match
    public MobFilterContents.@Nullable Entry entry(CrucibleInput input) {
        Boolean isBaby = age(input);
        return isBaby == null ? null : MobFilterItem.entryFor(input.filter(), mob, isBaby);
    }

    // Nature's balance: each powder burnt draws on every element that isn't among the powders
    public static Set<Element> balancing(CrucibleInput input) {
        Set<Element> others = EnumSet.allOf(Element.class);
        others.remove(element(input.left()));
        others.remove(element(input.right()));
        return others;
    }

    private static @Nullable Element element(ItemStack stack) {
        return stack.getItem() instanceof ElementPowderItem powder ? powder.element() : null;
    }

    // The filter with the mob added; nothing if there's no filter (the crucible itself checks it
    // has room and doesn't already list the mob)
    @Override
    public ItemStack assemble(CrucibleInput input) {
        MobFilterContents.Entry entry = entry(input);
        if (entry == null || !(input.filter().getItem() instanceof MobFilterItem)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.filter().copyWithCount(1);
        result.set(ModDataComponents.MOB_FILTER.get(), MobFilterItem.contents(result).with(entry));
        return result;
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
    public RecipeSerializer<CrucibleRecipe> getSerializer() {
        return ModRecipes.CRUCIBLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrucibleRecipe> getType() {
        return ModRecipes.CRUCIBLE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipes.CRUCIBLE_BOOK_CATEGORY.get();
    }
}
