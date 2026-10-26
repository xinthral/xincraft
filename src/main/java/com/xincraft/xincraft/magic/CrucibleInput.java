package com.xincraft.xincraft.magic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

// What's in the apothecary crucible (inventory.ApothecaryCrucibleMenu): two powders, the catalyst
// between them, and the mob filter to inscribe
public record CrucibleInput(ItemStack left, ItemStack right, ItemStack catalyst, ItemStack filter) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> left;
            case 1 -> right;
            case 2 -> catalyst;
            case 3 -> filter;
            default -> throw new IllegalArgumentException("No item for index " + index);
        };
    }

    @Override
    public int size() {
        return 4;
    }
}
