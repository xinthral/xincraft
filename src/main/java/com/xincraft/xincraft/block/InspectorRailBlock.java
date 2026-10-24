package com.xincraft.xincraft.block;

import java.util.List;
import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.entity.Carriage;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

// A detector rail whose redstone signal depends on which kind of minecart is on it, rather than
// always 15. The strength for each cart type comes from the entity type tags
// xincraft:inspector_rail_signal/<1-15> (see ModTags.inspectorRailSignal); untagged carts give 0.
// A carriage with someone aboard gives one more than its tag's signal.
// Otherwise it behaves like vanilla's DetectorRailBlock, which this is adapted from.
public class InspectorRailBlock extends BaseRailBlock {
    public static final MapCodec<InspectorRailBlock> CODEC = simpleCodec(InspectorRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    // While powered, how often to check whether the cart has left (same as the detector rail)
    private static final int PRESSED_CHECK_PERIOD = 20;

    public InspectorRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        registerDefaultState(stateDefinition.any()
            .setValue(SHAPE, RailShape.NORTH_SOUTH)
            .setValue(POWER, 0)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<InspectorRailBlock> codec() {
        return CODEC;
    }

    // The signal strength a cart produces, from the highest inspector_rail_signal tag its type is in;
    // one more for an occupied carriage, so a station can tell a full carriage from an empty one
    public static int signalFor(AbstractMinecart cart) {
        for (int signal = 15; signal > 0; signal--) {
            if (cart.is(ModTags.inspectorRailSignal(signal))) {
                return cart instanceof Carriage && cart.isVehicle() ? Math.min(signal + 1, 15) : signal;
            }
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWER);
    }

    // Strong power into the block below, like the detector rail
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP ? state.getValue(POWER) : 0;
    }

    // A cart arriving that gives a stronger signal updates right away; weaker signals and carts
    // leaving are picked up by the periodic check
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof AbstractMinecart cart && signalFor(cart) > state.getValue(POWER)) {
            checkPressed(level, pos, state);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWER) > 0) {
            checkPressed(level, pos, state);
        }
    }

    // Sets the power to the strongest signal among the carts on the rail
    private void checkPressed(Level level, BlockPos pos, BlockState state) {
        if (!canSurvive(state, level, pos)) {
            return;
        }
        int power = 0;
        for (AbstractMinecart cart : getCarts(level, pos)) {
            power = Math.max(power, signalFor(cart));
        }
        if (power != state.getValue(POWER)) {
            BlockState newState = state.setValue(POWER, power);
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            updatePowerToConnected(level, pos, newState);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
            level.setBlocksDirty(pos, state, newState);
        }
        if (power > 0) {
            level.scheduleTick(pos, this, PRESSED_CHECK_PERIOD);
        }
        level.updateNeighbourForOutputSignal(pos, this);
    }

    private void updatePowerToConnected(Level level, BlockPos pos, BlockState state) {
        for (BlockPos connectionPos : new RailState(level, pos, state).getConnections()) {
            BlockState connectionState = level.getBlockState(connectionPos);
            level.neighborChanged(connectionState, connectionPos, connectionState.getBlock(), null, false);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            checkPressed(level, pos, updateState(state, level, pos, movedByPiston));
        }
    }

    // Comparators read how full a container cart on the rail is, as with the detector rail
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        List<AbstractMinecart> carts = getCarts(level, pos);
        if (!carts.isEmpty() && carts.get(0).getComparatorLevel() > -1) {
            return carts.get(0).getComparatorLevel();
        }
        return carts.stream()
            .filter(EntitySelector.CONTAINER_ENTITY_SELECTOR)
            .findFirst()
            .map(cart -> AbstractContainerMenu.getRedstoneSignalFromContainer((Container) cart))
            .orElse(0);
    }

    private static List<AbstractMinecart> getCarts(Level level, BlockPos pos) {
        AABB area = new AABB(pos.getX() + 0.2, pos.getY(), pos.getZ() + 0.2, pos.getX() + 0.8, pos.getY() + 0.8, pos.getZ() + 0.8);
        return level.getEntitiesOfClass(AbstractMinecart.class, area, Entity::isAlive);
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWER, WATERLOGGED);
    }
}
