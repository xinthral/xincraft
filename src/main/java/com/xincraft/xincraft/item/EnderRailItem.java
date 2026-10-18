package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

// Ender rail item; can carry an attunement to a placed ender rail (see block.EnderRailBlock)
public class EnderRailItem extends BlockItem {
    private static final int GLINT_TICKS = 20;

    public EnderRailItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    // Gives the stack an enchantment glint for one second; inventoryTick takes it away again
    public static void flashGlint(ItemStack stack, Level level) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(ModDataComponents.GLINT_UNTIL.get(), level.getGameTime() + GLINT_TICKS);
    }

    public static void clearGlint(ItemStack stack) {
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        stack.remove(ModDataComponents.GLINT_UNTIL.get());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        Long until = stack.get(ModDataComponents.GLINT_UNTIL.get());
        if (until != null && level.getGameTime() >= until) {
            clearGlint(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        GlobalPos link = stack.get(ModDataComponents.ENDER_RAIL_LINK.get());
        if (link != null) {
            BlockPos pos = link.pos();
            builder.accept(Component.translatable("item.xincraft.ender_rail.attuned", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
