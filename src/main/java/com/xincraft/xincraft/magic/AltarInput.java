package com.xincraft.xincraft.magic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

// What's in the apothecary altar's transmutation slots (magic.ApothecaryAltarBlockEntity): the item
// being changed, and the material going into it
public record AltarInput(ItemStack base, ItemStack material) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> base;
            case 1 -> material;
            default -> throw new IllegalArgumentException("No item for index " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
