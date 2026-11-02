package com.xincraft.xincraft.transport.train;

import java.util.function.Consumer;
import com.xincraft.xincraft.transport.TransportComponents;
import com.xincraft.xincraft.transport.TransportItems;
import com.xincraft.xincraft.transport.power.ChargingStationBlock;
import com.xincraft.xincraft.transport.power.ChargingStationBlockEntity;
import com.xincraft.xincraft.XincraftMod;
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
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// A bronze drawbar that hitches carts and boats into trains, or links two buoys into a path for
// motorboats. The vehicles handle using it on them (transport.train.LinkedTransport); buoys are handled here.
// Either way, the bar remembers the first vehicle or buoy it was used on, glinting until it's used
// on the second, and one bar is used up per link.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public class LinkingBarItem extends Item {
    public LinkingBarItem(Item.Properties properties) {
        super(properties);
    }

    // Whether the bar is halfway through linking carts, or blocks
    public static boolean isLinkingCarts(ItemStack stack) {
        return stack.has(TransportComponents.LINKING_BAR_START.get());
    }

    public static boolean isLinkingBlocks(ItemStack stack) {
        return stack.has(TransportComponents.LINKING_BAR_BLOCK.get());
    }

    // Forgets the first cart or block
    public static void clearPending(ItemStack stack) {
        stack.remove(TransportComponents.LINKING_BAR_START.get());
        stack.remove(TransportComponents.LINKING_BAR_BLOCK.get());
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    // Used on a block that links (LinkableBlock, such as a buoy): shift-use unlinks it, handing back
    // its bars (except in creative, where linking didn't use any up); otherwise the first block is
    // remembered and the second is linked to it
    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof LinkableBlock linkable)) {
            return connectSource(context);
        }
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = linkable.linkPos(state, context.getClickedPos());
        LinkedBlockEntity block = LinkedBlockEntity.linkedAt(level, pos);
        if (block == null) {
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            int bars = block.unlinkAll();
            if (bars == 0) {
                player.sendOverlayMessage(block.message("not_linked"));
                return InteractionResult.FAIL;
            }
            if (!player.hasInfiniteMaterials()) {
                ItemStack returned = new ItemStack(TransportItems.LINKING_BAR.get(), bars);
                if (!player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendOverlayMessage(block.message("unlinked"));
            return InteractionResult.SUCCESS;
        }
        if (isLinkingCarts(stack)) {
            clearPending(stack);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.mixed"));
            return InteractionResult.FAIL;
        }
        GlobalPos first = stack.get(TransportComponents.LINKING_BAR_BLOCK.get());
        if (first == null) {
            if (block.isFull() && !block.connectsSources()) {
                player.sendOverlayMessage(block.message("full", block.maxLinks()));
                return InteractionResult.FAIL;
            }
            stack.set(TransportComponents.LINKING_BAR_BLOCK.get(), GlobalPos.of(level.dimension(), pos));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0F, 1.2F);
            player.sendOverlayMessage(block.message("started"));
            return InteractionResult.SUCCESS;
        }
        clearPending(stack);
        if (first.pos().equals(pos)) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.cancelled"));
            return InteractionResult.SUCCESS;
        }
        if (!first.dimension().equals(level.dimension())) {
            player.sendOverlayMessage(block.message("other_dimension"));
            return InteractionResult.FAIL;
        }
        LinkedBlockEntity other = LinkedBlockEntity.linkedAt(level, first.pos());
        if (other == null) {
            player.sendOverlayMessage(block.message("missing"));
            return InteractionResult.FAIL;
        }
        if (!block.accepts(other)) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.mixed_blocks"));
            return InteractionResult.FAIL;
        }
        // As far as the shorter-reaching of the two
        double distance = LinkedBlockEntity.distance(pos, first.pos());
        double range = Math.min(block.linkRange(), other.linkRange());
        if (distance > range) {
            player.sendOverlayMessage(block.message("too_far", (int) Math.ceil(distance), (int) range));
            return InteractionResult.FAIL;
        }
        if (block.isLinkedTo(first.pos())) {
            player.sendOverlayMessage(block.message("already_linked"));
            return InteractionResult.FAIL;
        }
        if (block.isFull() || other.isFull()) {
            player.sendOverlayMessage(block.message("full", block.maxLinks()));
            return InteractionResult.FAIL;
        }
        LinkedBlockEntity.link(other, block);
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendOverlayMessage(block.message("linked"));
        return InteractionResult.SUCCESS;
    }

    // With a charging station's end already tied on, used on an energy block right beside the
    // station: connects the two
    private InteractionResult connectSource(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        GlobalPos first = stack.get(TransportComponents.LINKING_BAR_BLOCK.get());
        if (first == null || !isHoldingStation(context.getLevel(), first)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ChargingStationBlockEntity station = (ChargingStationBlockEntity) level.getBlockEntity(first.pos());
        BlockPos pos = context.getClickedPos();
        clearPending(stack);
        Direction side = null;
        for (Direction direction : ChargingStationBlock.CONNECTED.keySet()) {
            if (first.pos().relative(direction).equals(pos)) {
                side = direction;
            }
        }
        if (side == null) {
            player.sendOverlayMessage(station.message("source_not_adjacent"));
            return InteractionResult.FAIL;
        }
        if (station.isConnected(side)) {
            player.sendOverlayMessage(station.message("source_already_connected"));
            return InteractionResult.FAIL;
        }
        if (!ChargingStationBlockEntity.isSource(level, pos, side.getOpposite())) {
            player.sendOverlayMessage(station.message("source_no_energy"));
            return InteractionResult.FAIL;
        }
        station.connect(side);
        stack.consume(1, player);
        level.playSound(null, first.pos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendOverlayMessage(station.message("source_connected"));
        return InteractionResult.SUCCESS;
    }

    private static boolean isHoldingStation(Level level, GlobalPos first) {
        return first.dimension().equals(level.dimension()) && level.isLoaded(first.pos())
            && level.getBlockEntity(first.pos()) instanceof ChargingStationBlockEntity;
    }

    // With a charging station's end tied on, a block with a menu of its own, such as a turbine, is
    // connected to rather than opened. Runs on both sides, as both decide whether to use the block.
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        GlobalPos first = stack.is(TransportItems.LINKING_BAR.get()) ? stack.get(TransportComponents.LINKING_BAR_BLOCK.get()) : null;
        if (first != null && isHoldingStation(level, first) && !(level.getBlockState(event.getPos()).getBlock() instanceof LinkableBlock)) {
            event.setUseBlock(Result.DENY);
        }
    }

    // What it does, from item.xincraft.linking_bar.desc, and whether it's halfway through a link
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (isLinkingCarts(stack) || isLinkingBlocks(stack)) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".started").withStyle(ChatFormatting.GOLD));
        }
    }
}
