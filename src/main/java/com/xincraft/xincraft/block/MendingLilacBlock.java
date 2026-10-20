package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// An exotic flower, grown by using exotic pollen on a lily of the valley (item.ExoticPollenItem).
// Like a small beacon, it gives regeneration to players and peaceful mobs around it
// (MendingLilacBlockEntity).
public class MendingLilacBlock extends FlowerBlock implements EntityBlock {
    public static final MapCodec<MendingLilacBlock> CODEC = simpleCodec(MendingLilacBlock::new);

    public MendingLilacBlock(BlockBehaviour.Properties properties) {
        super(MobEffects.REGENERATION, 8.0F, properties);
    }

    @Override
    public MapCodec<MendingLilacBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MendingLilacBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel) || type != ModBlockEntities.MENDING_LILAC.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MendingLilacBlockEntity>)
            (tickLevel, pos, tickState, lilac) -> lilac.serverTick(serverLevel);
    }

    // The odd green sparkle
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.4 + random.nextDouble() * 0.4,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
        }
    }
}
