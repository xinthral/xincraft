package com.xincraft.xincraft.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

// Grinds flowers and vines into elemental powders (item.ElementPowderItem) in the crafting grid. It
// isn't used up: it stays in the grid, a little more worn each time, until its durability runs out.
public class MortarAndPestleItem extends Item {
    public MortarAndPestleItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemStack stack) {
        ItemStack worn = stack.copyWithCount(1);
        worn.setDamageValue(worn.getDamageValue() + 1);
        return worn.getDamageValue() >= worn.getMaxDamage() ? null : ItemStackTemplate.fromNonEmptyStack(worn);
    }
}
