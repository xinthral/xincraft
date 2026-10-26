package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.client.compendium.CompendiumScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

// The Xincraft Compendium, the mod's guide book: using it opens it (client.compendium.CompendiumScreen)
// where it was left. With one anywhere in the inventory, the look-up key opens it at the entry for
// whatever's under the mouse or crosshair (client.CompendiumKeys).
public class CompendiumItem extends Item {
    public CompendiumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            CompendiumScreen.open();
        }
        return InteractionResult.SUCCESS;
    }

    // What it is, from item.xincraft.compendium.desc
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
