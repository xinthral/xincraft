package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.registry.ModBlockEntities;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import org.jspecify.annotations.Nullable;

// The heat and steam turbines (see TurbineBlockEntity). Built on the furnace block for its facing,
// lit state, placement and comparator output; right-click opens it, and right-clicking with a
// bucket fills (or empties) a turbine with a water tank.
public class TurbineBlock extends AbstractFurnaceBlock {
    private final Supplier<? extends BlockEntityType<? extends TurbineBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends TurbineBlockEntity> factory;
    private final boolean steam;

    public TurbineBlock(BlockBehaviour.Properties properties, Supplier<? extends BlockEntityType<? extends TurbineBlockEntity>> type,
            BiFunction<BlockPos, BlockState, ? extends TurbineBlockEntity> factory, boolean steam) {
        super(properties);
        this.type = type;
        this.factory = factory;
        this.steam = steam;
    }

    public static TurbineBlock heat(BlockBehaviour.Properties properties) {
        return new TurbineBlock(properties, ModBlockEntities.HEAT_TURBINE, HeatTurbineBlockEntity::new, false);
    }

    public static TurbineBlock steam(BlockBehaviour.Properties properties) {
        return new TurbineBlock(properties, ModBlockEntities.STEAM_TURBINE, SteamTurbineBlockEntity::new, true);
    }

    // Each turbine has its own codec, since which one it is isn't stored in its properties
    public static final MapCodec<TurbineBlock> HEAT_CODEC = simpleCodec(TurbineBlock::heat);
    public static final MapCodec<TurbineBlock> STEAM_CODEC = simpleCodec(TurbineBlock::steam);

    @Override
    protected MapCodec<TurbineBlock> codec() {
        return steam ? STEAM_CODEC : HEAT_CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actualType) {
        if (!(level instanceof ServerLevel serverLevel) || actualType != type.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<TurbineBlockEntity>)
            (tickLevel, pos, tickState, turbine) -> TurbineBlockEntity.serverTick(serverLevel, pos, tickState, turbine);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof TurbineBlockEntity turbine) {
            player.openMenu((MenuProvider) turbine);
        }
    }

    // A bucket (or anything else holding fluid) fills or empties the steam turbine's water tank
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (steam && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection())) {
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    // Furnace flames and crackle, plus steam from the steam turbine
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction facing = state.getValue(FACING);
        double side = random.nextDouble() * 0.6 - 0.3;
        double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : side;
        double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : side;
        double dy = random.nextDouble() * 6.0 / 16.0;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        if (steam && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x + random.nextDouble() * 0.4 - 0.2, y + 1.05, z + random.nextDouble() * 0.4 - 0.2,
                0.0, 0.05, 0.0);
        }
    }
}
