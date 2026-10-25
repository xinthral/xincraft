package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// A marker buoy, two blocks tall: a float sitting in a still water source, and a pole with a lamp
// above it. Buoys are linked together with a linking bar (item.LinkingBarItem, holding the links in
// BuoyBlockEntity, on the lower half) into paths that motorboats with the RC upgrade follow
// (entity.Motorboat). It has no collision, so boats and swimmers pass straight through it.
// Right-clicking either half switches its lamp off, and again back on: a boat coming to an unlit
// buoy stops short of it and waits, engine off, until it's lit again. Both halves carry LIT. Links
// reach linkRange() blocks; the advanced buoy (AdvancedBuoyBlock) reaches further.
public class BuoyBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<BuoyBlock> CODEC = simpleCodec(BuoyBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape LOWER_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0);
    private static final VoxelShape UPPER_SHAPE = Shapes.or(
        Block.box(7.0, 0.0, 7.0, 9.0, 10.0, 9.0),
        Block.box(6.0, 10.0, 6.0, 10.0, 14.0, 10.0));

    public BuoyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, false)
            .setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends BuoyBlock> codec() {
        return CODEC;
    }

    // Furthest this buoy links to another; a link reaches only as far as the shorter of its two buoys
    public double linkRange() {
        return BuoyBlockEntity.LINK_RANGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, WATERLOGGED, LIT);
    }

    // The lower half of the buoy at pos, where its links are kept
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    // Whether the buoy at pos (either half) is a lit buoy; false for anything else
    public static boolean isLit(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BuoyBlock && state.getValue(LIT);
    }

    // Whether there's an unlit buoy at pos, where boats stop
    public static boolean isUnlit(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BuoyBlock && !state.getValue(LIT);
    }

    // A linking bar links or unlinks it (item.LinkingBarItem) rather than switching it; anything
    // else, or an empty hand, switches it
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return stack.is(ModItems.LINKING_BAR.get()) ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // Switches the lamp off, or back on, on both halves
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            boolean lit = !state.getValue(LIT);
            BlockPos lower = lowerPos(state, pos);
            for (BlockPos half : new BlockPos[] {lower, lower.above()}) {
                BlockState halfState = level.getBlockState(half);
                if (halfState.is(this)) {
                    level.setBlock(half, halfState.setValue(LIT, lit), Block.UPDATE_ALL);
                }
            }
            level.playSound(null, lower.above(), lit ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // Placed in a still water source (the clicked water, see item.BuoyItem) with room above it
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        FluidState fluid = level.getFluidState(pos);
        if (fluid.getType() != Fluids.WATER || pos.getY() >= level.getMaxY()
                || !level.getBlockState(pos.above()).canBeReplaced(context) || !level.getFluidState(pos.above()).isEmpty()) {
            return null;
        }
        return defaultBlockState().setValue(WATERLOGGED, true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlockAndUpdate(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(LIT, state.getValue(LIT)));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return state.getValue(WATERLOGGED);
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    // Each half goes with the other, the float leaving its water behind
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        DoubleBlockHalf half = state.getValue(HALF);
        boolean partnerSide = direction == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (partnerSide && !(neighborState.is(this) && neighborState.getValue(HALF) != half)) {
            return removed(state);
        }
        return state;
    }

    private static BlockState removed(BlockState state) {
        return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    // Broken from either half, it drops once (from the lower half's loot table), as tall flowers do.
    // Broken in creative, it drops nothing, not even the bars of its links (BuoyBlockEntity).
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos lower = lowerPos(state, pos);
            BlockState lowerState = level.getBlockState(lower);
            if (lowerState.is(this)) {
                if (player.preventsBlockDrops() && BuoyBlockEntity.at(level, lower) instanceof BuoyBlockEntity buoy) {
                    // Unlinked now, so there are no bars left to drop when it's removed
                    buoy.unlinkAll();
                }
                if (!player.preventsBlockDrops()) {
                    dropResources(lowerState, level, lower, level.getBlockEntity(lower), player, player.getMainHandItem());
                }
                if (!lower.equals(pos)) {
                    level.setBlock(lower, removed(lowerState), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    level.levelEvent(player, 2001, lower, Block.getId(lowerState));
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, Blocks.AIR.defaultBlockState(), blockEntity, tool);
    }

    // The water stays in the float: buckets can't scoop it out
    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new BuoyBlockEntity(pos, state) : null;
    }
}
