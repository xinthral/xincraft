package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

// A rideable minecart that drives itself forward while it has fuel burning, like a furnace
// minecart you can sit in. It has a fuel slot (anything that burns in a furnace, lasting
// Config.locomotiveFuelMultiplier times as long) and one storage slot, both reachable by
// shift-right-clicking the cart, pressing the inventory key while riding, or a hopper above it.
// Takes upgrades (see CartUpgrades). With the energy upgrade it also stores Forge Energy, charged
// from energy blocks under the track (or by anything that hands energy to entities), and runs on
// that before burning any fuel.
public class LocomotiveCart extends AbstractMinecartContainer implements HasCustomInventoryScreen, UpgradableCart {
    public static final int FUEL_SLOT = 0;
    public static final int STORAGE_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.INT);
    // Engine force added each tick while burning. Strong enough to hold the cart at the speed
    // cap, including the raised cap on tin rails.
    private static final double PUSH = 0.25;
    // Below this horizontal speed (squared) the cart counts as stopped
    private static final double STOPPED_SPEED_SQR = 1.0E-4;
    // Most Forge Energy the energy upgrade takes in per tick, from blocks or anything else
    private static final int MAX_ENERGY_RECEIVE = 1_000;
    // Menu data entries: burn left, then stored energy and capacity (each split in two, as menu
    // data travels as 16-bit numbers), then whether the energy upgrade is fitted
    public static final int DATA_COUNT = 6;

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private int litTime;
    private int litDuration;
    private int energy;
    // Whether the engine is running on energy this tick
    private boolean electric;
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new Battery());
    // Horizontal unit vector the engine drives along; zero while parked, so no fuel is used
    private Vec3 heading = Vec3.ZERO;

    // Synced to an open menu for the flame. Menu data travels as 16-bit numbers, too small for
    // long burn times, so this sends the fraction of burn left in thousandths instead.
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime > 0 ? Math.max(1, (int) ((long) litTime * 1000 / Math.max(1, litDuration))) : 0;
                case 1 -> energy & 0x7FFF;
                case 2 -> energy >>> 15;
                case 3 -> energyCapacity() & 0x7FFF;
                case 4 -> energyCapacity() >>> 15;
                default -> upgrades.has(CartUpgrade.ENERGY) ? 1 : 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LocomotiveCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LIT, false);
        builder.define(DATA_UPGRADES, 0);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    public boolean isLit() {
        return entityData.get(DATA_LIT);
    }

    private static int energyCapacity() {
        return Config.locomotiveEnergyCapacity;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            updateHeading();
            if (litTime > 0) {
                litTime--;
            }
            electric = false;
            if (upgrades.has(CartUpgrade.ENERGY)) {
                chargeFromTrack(serverLevel);
                // Runs on energy whenever it has some, saving fuel for when it runs out
                if (litTime <= 0 && heading.lengthSqr() > 0 && energy >= Config.locomotiveEnergyPerTick) {
                    energy -= Config.locomotiveEnergyPerTick;
                    electric = true;
                }
            }
            if (litTime <= 0 && !electric && heading.lengthSqr() > 0) {
                burnFuel(serverLevel);
            }
            entityData.set(DATA_LIT, litTime > 0);
            if (electric && random.nextInt(4) == 0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.6, getZ(), 2, 0.3, 0.1, 0.3, 0.0);
            }
        }
        super.tick();
        if (isLit() && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 0.0, 0.0, 0.0);
        }
    }

    // Draws energy from whatever's under the cart that gives it out: the rail itself (for other
    // mods' charging rails), then the block beneath the rail (e.g. a battery)
    private void chargeFromTrack(ServerLevel level) {
        if (energy >= energyCapacity()) {
            return;
        }
        BlockPos rail = getCurrentBlockPosOrRailBelow();
        drawFrom(level, rail, null);
        drawFrom(level, rail.below(), Direction.UP);
    }

    private void drawFrom(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        int space = Math.min(MAX_ENERGY_RECEIVE, energyCapacity() - energy);
        if (space <= 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ENERGY, side).ifPresent(source -> {
                if (source.canExtract()) {
                    energy += source.extractEnergy(space, false);
                }
            });
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
        if ((litTime > 0 || electric) && heading.lengthSqr() > 0) {
            deltaMovement = deltaMovement.multiply(0.8, 0.0, 0.8).add(heading.scale(PUSH));
            if (isInWater()) {
                deltaMovement = deltaMovement.scale(0.1);
            }
        }
        return super.applyNaturalSlowdown(deltaMovement);
    }

    // Upgrades first; then right-click to ride, shift-right-click to open the fuel and storage slots
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult upgraded = upgrades.interact(this, player, hand);
        if (upgraded != InteractionResult.PASS) {
            return upgraded;
        }
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
    public void push(Entity entity) {
        if (!upgrades.pushAside(entity)) {
            super.push(entity);
        }
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        upgrades.dropAll(level);
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

    // The energy upgrade's storage, for other mods' cables, chargers and the like
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY && isAlive() && upgrades.has(CartUpgrade.ENERGY)) {
            return energyCap.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> new Battery());
    }

    // Takes energy in only; nothing can draw it back out. Takes nothing once the upgrade is removed.
    private class Battery implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) {
                return 0;
            }
            int received = Math.max(0, Math.min(Math.min(maxReceive, MAX_ENERGY_RECEIVE), energyCapacity() - energy));
            if (!simulate) {
                energy += received;
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return upgrades.has(CartUpgrade.ENERGY) && isAlive();
        }
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
        output.putInt("Energy", energy);
        output.putDouble("HeadingX", heading.x);
        output.putDouble("HeadingZ", heading.z);
        upgrades.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        energy = input.getIntOr("Energy", 0);
        heading = new Vec3(input.getDoubleOr("HeadingX", 0.0), 0.0, input.getDoubleOr("HeadingZ", 0.0));
        upgrades.load(input);
    }
}
