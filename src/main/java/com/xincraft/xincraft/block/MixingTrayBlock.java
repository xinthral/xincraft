package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A low wooden tray for mixing flowers into exotic pollen (see MixingTrayBlockEntity). Right-click
// with a small flower to put it on; right-click with an empty hand to take the pollen, or the last
// flower put on.
public class MixingTrayBlock extends BaseEntityBlock {
    public static final MapCodec<MixingTrayBlock> CODEC = simpleCodec(MixingTrayBlock::new);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 2.0);

    public MixingTrayBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MixingTrayBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixingTrayBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!MixingTrayBlockEntity.isMixable(stack) || !(level.getBlockEntity(pos) instanceof MixingTrayBlockEntity tray)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (!tray.addFlower(serverLevel, stack)) {
                return InteractionResult.FAIL;
            }
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Only an empty hand takes things off, not any item that isn't a flower
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof MixingTrayBlockEntity tray)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack taken = tray.takeTop(serverLevel);
            if (taken.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
