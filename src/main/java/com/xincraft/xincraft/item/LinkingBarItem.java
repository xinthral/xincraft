package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.block.BuoyBlock;
import com.xincraft.xincraft.block.BuoyBlockEntity;
import com.xincraft.xincraft.registry.ModDataComponents;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

// A bronze drawbar that hitches carts and boats into trains, or links two buoys into a path for
// motorboats. The vehicles handle using it on them (entity.LinkedTransport); buoys are handled here.
// Either way, the bar remembers the first vehicle or buoy it was used on, glinting until it's used
// on the second, and one bar is used up per link.
public class LinkingBarItem extends Item {
    public LinkingBarItem(Item.Properties properties) {
        super(properties);
    }

    // Whether the bar is halfway through linking carts, or buoys
    public static boolean isLinkingCarts(ItemStack stack) {
        return stack.has(ModDataComponents.LINKING_BAR_START.get());
    }

    public static boolean isLinkingBuoys(ItemStack stack) {
        return stack.has(ModDataComponents.LINKING_BAR_BUOY.get());
    }

    // Forgets the first cart or buoy
    public static void clearPending(ItemStack stack) {
        stack.remove(ModDataComponents.LINKING_BAR_START.get());
        stack.remove(ModDataComponents.LINKING_BAR_BUOY.get());
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    // Used on a buoy: shift-use unlinks it, handing back its bars (except in creative, where linking
    // didn't use any up); otherwise the first buoy is remembered and the second is linked to it
    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof BuoyBlock)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = BuoyBlock.lowerPos(state, context.getClickedPos());
        BuoyBlockEntity buoy = BuoyBlockEntity.at(level, pos);
        if (buoy == null) {
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            int bars = buoy.unlinkAll();
            if (bars == 0) {
                player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_not_linked"));
                return InteractionResult.FAIL;
            }
            if (!player.hasInfiniteMaterials()) {
                ItemStack returned = new ItemStack(ModItems.LINKING_BAR.get(), bars);
                if (!player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_unlinked"));
            return InteractionResult.SUCCESS;
        }
        if (isLinkingCarts(stack)) {
            clearPending(stack);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.mixed"));
            return InteractionResult.FAIL;
        }
        GlobalPos first = stack.get(ModDataComponents.LINKING_BAR_BUOY.get());
        if (first == null) {
            if (buoy.isFull()) {
                player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_full", BuoyBlockEntity.MAX_LINKS));
                return InteractionResult.FAIL;
            }
            stack.set(ModDataComponents.LINKING_BAR_BUOY.get(), GlobalPos.of(level.dimension(), pos));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_started"));
            return InteractionResult.SUCCESS;
        }
        clearPending(stack);
        if (first.pos().equals(pos)) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.cancelled"));
            return InteractionResult.SUCCESS;
        }
        if (!first.dimension().equals(level.dimension())) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_other_dimension"));
            return InteractionResult.FAIL;
        }
        BuoyBlockEntity other = BuoyBlockEntity.at(level, first.pos());
        if (other == null) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_missing"));
            return InteractionResult.FAIL;
        }
        // As far as the shorter-reaching of the two buoys
        double distance = BuoyBlockEntity.legLength(pos, first.pos());
        double range = Math.min(buoy.linkRange(), other.linkRange());
        if (distance > range) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_too_far",
                (int) Math.ceil(distance), (int) range));
            return InteractionResult.FAIL;
        }
        if (buoy.isLinkedTo(first.pos())) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_already_linked"));
            return InteractionResult.FAIL;
        }
        if (buoy.isFull() || other.isFull()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_full", BuoyBlockEntity.MAX_LINKS));
            return InteractionResult.FAIL;
        }
        BuoyBlockEntity.link(other, buoy);
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.buoy_linked"));
        return InteractionResult.SUCCESS;
    }

    // What it does, from item.xincraft.linking_bar.desc, and whether it's halfway through a link
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (isLinkingCarts(stack) || isLinkingBuoys(stack)) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".started").withStyle(ChatFormatting.GOLD));
        }
    }
}
