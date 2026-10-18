package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

// A powered (booster) rail for the mod's powered rail variants (padded, tin), which differ only
// in their block properties and tags. Exists only because PoweredRailBlock's public constructor
// builds an activator rail; the powered-rail constructor is protected. Minecart physics check
// `instanceof PoweredRailBlock && !isActivatorRail()`, so this boosts and brakes carts exactly
// like the vanilla powered rail and shares power along chains with it.
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
