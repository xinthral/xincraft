package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// A bronze powered rail that also holds Forge Energy (see ChargingRailBlockEntity). It draws energy
// from any energy block beside, above or below it (or takes what cables push in) and passes it to a
// cart on it that takes energy, such as a locomotive cart with the energy upgrade. Otherwise it's a
// powered rail: unpowered, it brakes and holds carts, so a locomotive stops on it and charges;
// powered, it boosts them on their way.
public class ChargingRailBlock extends ModPoweredRailBlock implements EntityBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(ChargingRailBlock::new);

    public ChargingRailBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PoweredRailBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingRailBlockEntity(pos, state);
    }

    // Only the server moves energy
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.CHARGING_RAIL.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ChargingRailBlockEntity>) (tickLevel, pos, tickState, rail) -> rail.serverTick(tickLevel);
    }
}
