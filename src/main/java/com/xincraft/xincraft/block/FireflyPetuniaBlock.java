package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// An exotic flower, grown by using exotic pollen on a dandelion (item.ExoticPollenItem). It glows a
// soft yellow (its light level is set in ModBlocks) with fireflies drifting around it.
public class FireflyPetuniaBlock extends FlowerBlock {
    public static final MapCodec<FireflyPetuniaBlock> CODEC = simpleCodec(FireflyPetuniaBlock::new);

    public FireflyPetuniaBlock(BlockBehaviour.Properties properties) {
        super(MobEffects.NIGHT_VISION, 5.0F, properties);
    }

    @Override
    public MapCodec<FireflyPetuniaBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FIREFLY,
                pos.getX() + random.nextDouble() * 1.6 - 0.3, pos.getY() + 0.3 + random.nextDouble() * 1.2,
                pos.getZ() + random.nextDouble() * 1.6 - 0.3, 0.0, 0.0, 0.0);
        }
    }
}
