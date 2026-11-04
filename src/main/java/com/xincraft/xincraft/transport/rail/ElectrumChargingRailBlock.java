package com.xincraft.xincraft.transport.rail;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @brief A charging rail made of electrum, which moves energy faster, reaches further for power,
 *        and passes charge along a line of its kind.
 *
 * Otherwise the same as ChargingRailBlock.
 *
 * @see ElectrumChargingRailBlockEntity
 */
public class ElectrumChargingRailBlock extends ChargingRailBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(ElectrumChargingRailBlock::new);

    public ElectrumChargingRailBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PoweredRailBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrumChargingRailBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends ChargingRailBlockEntity> blockEntityType() {
        return TransportBlockEntities.ELECTRUM_CHARGING_RAIL.get();
    }
}
