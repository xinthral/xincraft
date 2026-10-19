package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.entity.CartUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// An upgrade for the mod's minecarts: used on a cart, it's fitted to it (see entity.CartUpgrades)
public class CartUpgradeItem extends Item {
    private final CartUpgrade upgrade;

    public CartUpgradeItem(CartUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public CartUpgrade upgrade() {
        return upgrade;
    }

    // What it does, from item.xincraft.<name>.desc
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
