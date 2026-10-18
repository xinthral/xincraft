package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A rideable minecart that drives itself forward while it has fuel burning, like a furnace
// minecart you can sit in. It has a fuel slot (anything that burns in a furnace, lasting
// Config.locomotiveFuelMultiplier times as long) and one storage slot, both reachable by
// shift-right-clicking the cart, pressing the inventory key while riding, or a hopper above it.
public class LocomotiveCart extends AbstractMinecartContainer implements HasCustomInventoryScreen {
    public static final int FUEL_SLOT = 0;
    public static final int STORAGE_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.BOOLEAN);
    // Engine force added each tick while burning. Strong enough to hold the cart at the speed
    // cap, including the raised cap on tin rails.
    private static final double PUSH = 0.25;
    // Below this horizontal speed (squared) the cart counts as stopped
    private static final double STOPPED_SPEED_SQR = 1.0E-4;

    private int litTime;
    private int litDuration;
    // Horizontal unit vector the engine drives along; zero while parked, so no fuel is used
    private Vec3 heading = Vec3.ZERO;

    // Synced to an open menu for the flame. Menu data travels as 16-bit numbers, too small for
    // long burn times, so this sends the fraction of burn left in thousandths instead.
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return litTime > 0 ? Math.max(1, (int) ((long) litTime * 1000 / Math.max(1, litDuration))) : 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 1;
        }
    };

    public LocomotiveCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LIT, false);
    }

    public boolean isLit() {
        return entityData.get(DATA_LIT);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            updateHeading();
            if (litTime > 0) {
                litTime--;
            }
            if (litTime <= 0 && heading.lengthSqr() > 0) {
                burnFuel(serverLevel);
            }
            entityData.set(DATA_LIT, litTime > 0);
        }
        super.tick();
        if (isLit() && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 0.0, 0.0, 0.0);
        }
    }

    // Works out which way the engine should drive this tick
    private void updateHeading() {
        Vec3 motion = getDeltaMovement().horizontal();
        boolean stopped = motion.lengthSqr() < STOPPED_SPEED_SQR;
        if (stopped && getFirstPassenger() instanceof ServerPlayer rider
                && rider.getLastClientMoveIntent().horizontalDistanceSqr() > 0) {
            // A rider pressing a movement key gets a stopped locomotive going that way
            heading = rider.getLastClientMoveIntent().horizontal().normalize();
        } else if (isOnUnpoweredPoweredRail()) {
            // Unpowered powered rails act as a brake, as for any cart; power the rail to set off again
            heading = Vec3.ZERO;
        } else if (!stopped) {
            // Otherwise keep driving whichever way it's already going, around curves too
            heading = motion.normalize();
        } else {
            heading = Vec3.ZERO;
        }
    }

    private boolean isOnUnpoweredPoweredRail() {
        BlockState state = level().getBlockState(getCurrentBlockPosOrRailBelow());
        return state.getBlock() instanceof PoweredRailBlock rail && !rail.isActivatorRail() && !state.getValue(PoweredRailBlock.POWERED);
    }

    // Lights the next fuel item, the same way a furnace does, but burning longer
    private void burnFuel(ServerLevel level) {
        ItemStack fuel = getItem(FUEL_SLOT);
        int duration = (int) Math.round(level.fuelValues().burnDuration(fuel) * Config.locomotiveFuelMultiplier);
        if (duration <= 0) {
            return;
        }
        litTime = duration;
        litDuration = duration;
        if (fuel.getCount() == 1) {
            // e.g. a lava bucket leaves an empty bucket behind
            ItemStackTemplate remainder = fuel.getCraftingRemainder();
            setItem(FUEL_SLOT, remainder != null ? remainder.create() : ItemStack.EMPTY);
        } else {
            fuel.shrink(1);
        }
    }

    // Same engine force as the furnace minecart, applied along the heading
    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        if (litTime > 0 && heading.lengthSqr() > 0) {
            deltaMovement = deltaMovement.multiply(0.8, 0.0, 0.8).add(heading.scale(PUSH));
            if (isInWater()) {
                deltaMovement = deltaMovement.scale(0.1);
            }
        }
        return super.applyNaturalSlowdown(deltaMovement);
    }

    // Right-click to ride; shift-right-click to open the fuel and storage slots
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive()) {
            InteractionResult result = interactWithContainerVehicle(player);
            if (result.consumesAction() && player.level() instanceof ServerLevel) {
                gameEvent(GameEvent.CONTAINER_OPEN, player);
            }
            return result;
        }
        if (isVehicle()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    // Opened by the inventory key while riding
    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (player.level() instanceof ServerLevel) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(user.getLivingEntity()));
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    // Hoppers (and anything else inserting) can only put burnable items in the fuel slot
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != FUEL_SLOT || level().fuelValues().isFuel(stack);
    }

    // Like a furnace, only empty buckets can be pulled back out of the fuel slot
    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack stack) {
        return slot != FUEL_SLOT || stack.is(Items.BUCKET);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LocomotiveCartMenu(containerId, inventory, this, data);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LOCOMOTIVE_CART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.LOCOMOTIVE_CART.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
        output.putDouble("HeadingX", heading.x);
        output.putDouble("HeadingZ", heading.z);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        heading = new Vec3(input.getDoubleOr("HeadingX", 0.0), 0.0, input.getDoubleOr("HeadingZ", 0.0));
    }
}
