package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// A low wooden tray for making exotic pollen, filled like a composter. Each different small flower
// right-clicked onto it is mashed in, raising the layer of flower mash a step; four make a full
// tray. Honeycomb, right-clicked on or thrown onto a full tray, turns the mash into pollen, which an
// empty hand takes off. Which flowers are in the mash is kept by MixingTrayBlockEntity.
public class MixingTrayBlock extends BaseEntityBlock {
    public static final MapCodec<MixingTrayBlock> CODEC = simpleCodec(MixingTrayBlock::new);
    // Flowers mashed in so far (0 to FULL), then READY once honeycomb has made it pollen
    public static final int FULL = 4;
    public static final int READY = 5;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, READY);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 3.0);

    public MixingTrayBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<MixingTrayBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixingTrayBlockEntity(pos, state);
    }

    // Only a full tray needs ticking, to catch thrown honeycomb
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel) || state.getValue(LEVEL) != FULL || type != ModBlockEntities.MIXING_TRAY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MixingTrayBlockEntity>)
            (tickLevel, pos, tickState, tray) -> tray.serverTick(serverLevel, tickState);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        int fill = state.getValue(LEVEL);
        if (stack.is(Items.HONEYCOMB)) {
            if (fill != FULL) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level instanceof ServerLevel serverLevel) {
                finish(serverLevel, pos, state);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!MixingTrayBlockEntity.isMixable(stack) || !(level.getBlockEntity(pos) instanceof MixingTrayBlockEntity tray)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (fill >= FULL || !tray.accepts(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            tray.addFlower(stack);
            stack.consume(1, player);
            serverLevel.setBlock(pos, state.setValue(LEVEL, fill + 1), Block.UPDATE_ALL);
            serverLevel.playSound(null, pos, fill + 1 == FULL ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL,
                SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5,
                6, 0.25, 0.05, 0.25, 0.0);
        }
        return InteractionResult.SUCCESS;
    }

    // An empty hand takes the finished pollen off
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty() || state.getValue(LEVEL) != READY) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack pollen = new ItemStack(ModItems.EXOTIC_POLLEN.get());
            if (!player.getInventory().add(pollen)) {
                player.drop(pollen, false);
            }
            if (level.getBlockEntity(pos) instanceof MixingTrayBlockEntity tray) {
                tray.clearFlowers();
            }
            serverLevel.setBlock(pos, state.setValue(LEVEL, 0), Block.UPDATE_ALL);
            serverLevel.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // Honeycomb on a full tray: the mash becomes pollen
    static void finish(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(LEVEL, READY), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 10, 0.3, 0.05, 0.3, 0.0);
    }
}
