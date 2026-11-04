package com.xincraft.xincraft.transport.power;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.transport.TransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @brief Stores a power network's energy and extends its reach.
 *
 * @see SubstationBlockEntity
 */
public class SubstationBlock extends PowerNodeBlock {
    public static final MapCodec<SubstationBlock> CODEC = simpleCodec(SubstationBlock::new);

    public SubstationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SubstationBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SubstationBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends PowerNodeBlockEntity> blockEntityType() {
        return TransportBlockEntities.SUBSTATION.get();
    }
}
