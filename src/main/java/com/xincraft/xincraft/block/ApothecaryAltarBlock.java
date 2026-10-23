package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// A stone altar with a fire basin that burns plants into elemental essence for the flora around it
// (see ApothecaryAltarBlockEntity). Right-click opens it; its contents drop when it's broken.
public class ApothecaryAltarBlock extends BaseEntityBlock {
    public static final MapCodec<ApothecaryAltarBlock> CODEC = simpleCodec(ApothecaryAltarBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ApothecaryAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<ApothecaryAltarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ApothecaryAltarBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel) || type != ModBlockEntities.APOTHECARY_ALTAR.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ApothecaryAltarBlockEntity>)
            (tickLevel, pos, tickState, altar) -> ApothecaryAltarBlockEntity.serverTick(serverLevel, pos, tickState, altar);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ApothecaryAltarBlockEntity altar) {
            player.openMenu((MenuProvider) altar);
        }
        return InteractionResult.SUCCESS;
    }

    // Flames and smoke rising from the basin, with the odd crackle
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
        }
        double dx = random.nextDouble() * 0.5 - 0.25;
        double dz = random.nextDouble() * 0.5 - 0.25;
        level.addParticle(ParticleTypes.FLAME, x + dx, y, z + dz, 0.0, 0.01, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + dx, y + 0.1, z + dz, 0.0, 0.03, 0.0);
        }
    }
}
