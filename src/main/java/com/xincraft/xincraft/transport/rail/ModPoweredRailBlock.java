package com.xincraft.xincraft.transport.rail;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * @brief A powered (booster) rail for the mod's powered rail variants (padded, tin), which differ
 *        only in their properties and tags.
 *
 * This exists because PoweredRailBlock's public constructor builds an activator rail, and its
 * powered-rail constructor is protected. It boosts and brakes carts exactly like the vanilla
 * powered rail, and shares power along chains with it.
 */
public class ModPoweredRailBlock extends PoweredRailBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(ModPoweredRailBlock::new);

    public ModPoweredRailBlock(BlockBehaviour.Properties properties) {
        super(properties, true);
    }

    @Override
    public MapCodec<PoweredRailBlock> codec() {
        return CODEC;
    }
}
