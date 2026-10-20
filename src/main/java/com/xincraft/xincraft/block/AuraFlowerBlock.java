package com.xincraft.xincraft.block;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
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

// An exotic flower that, like a small beacon, keeps giving a status effect to what's around it
// (AuraFlowerBlockEntity). Grown with exotic pollen (item.ExoticPollenItem):
// - the mending lilac (from lily of the valley): Regeneration for players and peaceful mobs
// - the moonlit dandelion (from golden dandelion): Night Vision for players
public class AuraFlowerBlock extends FlowerBlock implements EntityBlock {
    public static final MapCodec<AuraFlowerBlock> MENDING_LILAC_CODEC = simpleCodec(AuraFlowerBlock::mendingLilac);
    public static final MapCodec<AuraFlowerBlock> MOONLIT_DANDELION_CODEC = simpleCodec(AuraFlowerBlock::moonlitDandelion);

    private final Holder<MobEffect> aura;
    // How long each refresh of the effect lasts
    private final int effectTicks;
    // Whether mobs that aren't hostile get the effect too, or only players
    private final boolean includesMobs;
    private final IntSupplier range;
    private final SimpleParticleType particle;
    private final Supplier<? extends BlockEntityType<AuraFlowerBlockEntity>> type;
    private final MapCodec<AuraFlowerBlock> codec;

    private AuraFlowerBlock(BlockBehaviour.Properties properties, Holder<MobEffect> stewEffect, float stewSeconds,
            Holder<MobEffect> aura, int effectTicks, boolean includesMobs, IntSupplier range, SimpleParticleType particle,
            Supplier<? extends BlockEntityType<AuraFlowerBlockEntity>> type, MapCodec<AuraFlowerBlock> codec) {
        super(stewEffect, stewSeconds, properties);
        this.aura = aura;
        this.effectTicks = effectTicks;
        this.includesMobs = includesMobs;
        this.range = range;
        this.particle = particle;
        this.type = type;
        this.codec = codec;
    }

    public static AuraFlowerBlock mendingLilac(BlockBehaviour.Properties properties) {
        return new AuraFlowerBlock(properties, MobEffects.REGENERATION, 8.0F, MobEffects.REGENERATION, 100, true,
            () -> Config.mendingLilacRange, ParticleTypes.HAPPY_VILLAGER, ModBlockEntities.MENDING_LILAC, MENDING_LILAC_CODEC);
    }

    // Night Vision makes the screen flicker in its last 10 seconds, so each refresh lasts longer than that
    public static AuraFlowerBlock moonlitDandelion(BlockBehaviour.Properties properties) {
        return new AuraFlowerBlock(properties, MobEffects.NIGHT_VISION, 5.0F, MobEffects.NIGHT_VISION, 300, false,
            () -> Config.moonlitDandelionRange, ParticleTypes.END_ROD, ModBlockEntities.MOONLIT_DANDELION, MOONLIT_DANDELION_CODEC);
    }

    public Holder<MobEffect> getAura() {
        return aura;
    }

    public int getEffectTicks() {
        return effectTicks;
    }

    public boolean includesMobs() {
        return includesMobs;
    }

    public int getRange() {
        return range.getAsInt();
    }

    BlockEntityType<AuraFlowerBlockEntity> getBlockEntityType() {
        return type.get();
    }

    @Override
    public MapCodec<AuraFlowerBlock> codec() {
        return codec;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuraFlowerBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actualType) {
        if (!(level instanceof ServerLevel serverLevel) || actualType != type.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<AuraFlowerBlockEntity>)
            (tickLevel, pos, tickState, flower) -> flower.serverTick(serverLevel);
    }

    // The odd sparkle
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(particle,
                pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.4 + random.nextDouble() * 0.4,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, particle == ParticleTypes.END_ROD ? 0.01 : 0.0, 0.0);
        }
    }
}
