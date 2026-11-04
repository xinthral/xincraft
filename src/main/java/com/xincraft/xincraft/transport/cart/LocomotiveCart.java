package com.xincraft.xincraft.transport.cart;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.transport.rail.ChargingRailBlockEntity;
import com.xincraft.xincraft.transport.TransportItems;
import com.xincraft.xincraft.transport.train.CartUpgrade;
import com.xincraft.xincraft.transport.train.CartUpgrades;
import com.xincraft.xincraft.transport.train.LinkableTransport;
import com.xincraft.xincraft.transport.train.LinkedTransport;
import com.xincraft.xincraft.transport.train.UpgradableCart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.npc.villager.Villager;
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

/**
 * @brief A rideable minecart that drives itself forward while it has fuel burning.
 *
 * It has a fuel slot and one storage slot, and takes upgrades. With the energy upgrade, the engine
 * runs only on stored Forge Energy, charged from charging rails, energy blocks or its own fuel.
 *
 * It tows a train of up to MAX_TOWED carts. A rider pressing jump throws the handbrake, which holds
 * the cart still, even on a powered rail, until a rider presses jump again. Only players and
 * villagers ride it.
 *
 * @see transport.train.CartUpgrades
 * @see transport.train.LinkedTransport
 */
public class LocomotiveCart extends AbstractMinecartContainer implements HasCustomInventoryScreen, UpgradableCart, LinkableTransport {
    public static final int FUEL_SLOT = 0;
    public static final int STORAGE_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    /**
     * @brief Most carts it tows in a train behind it.
     *
     * An advanced locomotive is planned that tows more.
     */
    public static final int MAX_TOWED = 3;

    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HANDBRAKE = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HITCHED = SynchedEntityData.defineId(LocomotiveCart.class, EntityDataSerializers.BOOLEAN);
    /**
     * @brief Engine force added each tick while burning.
     *
     * Strong enough to hold the cart at the speed cap, including the raised cap on tin rails.
     */
    private static final double PUSH = 0.25;
    /** @brief Below this horizontal speed (squared) the cart counts as stopped. */
    private static final double STOPPED_SPEED_SQR = 1.0E-4;
    /**
     * @brief Handbrake: the share of its speed the cart keeps each tick.
     *
     * Together with BRAKE_DROP, this brings it from full speed to a stop in under a second.
     */
    private static final double BRAKE_KEEP = 0.8;
    /** @brief Handbrake: the speed taken off each tick, on top of BRAKE_KEEP. */
    private static final double BRAKE_DROP = 0.02;
    /**
     * @brief Below this speed the handbrake holds the cart dead still.
     *
     * A powered rail's boost would otherwise balance the braking, leaving the cart rolling along
     * forever at a few hundredths of a block per tick.
     */
    private static final double BRAKE_HOLD_SPEED = 0.06;
    /**
     * @brief Most Forge Energy the energy upgrade takes in per tick, from blocks or anything else.
     */
    private static final int MAX_ENERGY_RECEIVE = 1_000;
    /**
     * @brief What a rail or other source can push in per tick: enough for the electrum charging
     *        rail.
     */
    private static final int MAX_ENERGY_PUSHED = 4_000;
    /**
     * @brief Menu data entries.
     *
     * Burn left, then stored energy and capacity (each split in two, as menu data travels as 16-bit
     * numbers), then whether the energy upgrade is fitted.
     */
    public static final int DATA_COUNT = 6;

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final LinkedTransport transport = new LinkedTransport(this, DATA_TOWED, DATA_HITCHED);
    private int litTime;
    private int litDuration;
    private int energy;
    /** @brief Whether the engine is running on energy this tick. */
    private boolean electric;
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new Battery());
    /**
     * @brief Horizontal unit vector the engine drives along; zero while parked, so no fuel is used.
     */
    private Vec3 heading = Vec3.ZERO;
    /**
     * @brief Whether the jump key was down last tick, so that holding it throws the handbrake only
     *        once.
     */
    private boolean jumpHeld;

    /**
     * @brief Synced to an open menu for the flame.
     *
     * Menu data travels as 16-bit numbers, too small for long burn times, so this sends the
     * fraction of burn left in thousandths instead.
     */
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
        builder.define(DATA_HANDBRAKE, false);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_TOWED, LinkedTransport.NONE);
        builder.define(DATA_HITCHED, false);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public LinkedTransport transport() {
        return transport;
    }

    @Override
    public int maxTowed() {
        return MAX_TOWED;
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
            transport.tick(serverLevel);
            upgrades.tickShimmer(serverLevel);
            ServerPlayer rider = getFirstPassenger() instanceof ServerPlayer player ? player : null;
            updateHandbrake(rider, rider != null && rider.getLastClientInput().jump());
            updateHeading();
            if (litTime > 0) {
                litTime--;
            }
            electric = false;
            if (upgrades.has(CartUpgrade.ENERGY)) {
                // Fuel and the track charge the battery, and the engine runs only on the battery
                chargeFromTrack(serverLevel);
                chargeFromFuel(serverLevel);
                if (heading.lengthSqr() > 0 && energy >= Config.locomotiveEnergyPerTick) {
                    energy -= Config.locomotiveEnergyPerTick;
                    electric = true;
                }
            } else if (litTime <= 0 && heading.lengthSqr() > 0) {
                burnFuel(serverLevel);
            }
            entityData.set(DATA_LIT, litTime > 0);
            if (electric && random.nextInt(4) == 0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.6, getZ(), 2, 0.3, 0.1, 0.3, 0.0);
            }
            if (isHandbrakeOn() && getDeltaMovement().horizontalDistanceSqr() > STOPPED_SPEED_SQR && random.nextInt(2) == 0) {
                // Sparks off the braking wheels
                serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.1, getZ(), 2, 0.4, 0.0, 0.4, 0.05);
            }
        }
        super.tick();
        if (!level().isClientSide() && isHandbrakeOn()) {
            holdBrake();
        }
        if (isLit() && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 0.0, 0.0, 0.0);
        }
    }

    public boolean isHandbrakeOn() {
        return entityData.get(DATA_HANDBRAKE);
    }

    /**
     * @brief Throws the handbrake each time the rider presses jump.
     *
     * @param rider The player riding it, or null.
     * @param jumping Whether the rider is pressing jump.
     */
    private void updateHandbrake(@Nullable ServerPlayer rider, boolean jumping) {
        if (jumping && !jumpHeld) {
            boolean on = !isHandbrakeOn();
            entityData.set(DATA_HANDBRAKE, on);
            playSound(SoundEvents.LEVER_CLICK, 0.5F, on ? 0.6F : 0.5F);
            if (rider != null) {
                rider.sendOverlayMessage(Component.translatable(on ? "message.xincraft.locomotive.handbrake_on" : "message.xincraft.locomotive.handbrake_off"));
            }
        }
        jumpHeld = jumping;
    }

    /**
     * @brief Brakes again after the physics have run, then holds the cart still once it's slow
     *        enough.
     *
     * The physics add a powered rail's boost after applyNaturalSlowdown's braking, so that alone
     * can't stop it.
     */
    private void holdBrake() {
        Vec3 motion = getDeltaMovement();
        double speed = motion.horizontalDistance();
        double slowed = speed < BRAKE_HOLD_SPEED ? 0.0 : speed * BRAKE_KEEP - BRAKE_DROP;
        double scale = speed > 0.0 ? slowed / speed : 0.0;
        setDeltaMovement(motion.multiply(scale, 1.0, scale));
    }

    /**
     * @brief With the energy upgrade, burning fuel charges the battery rather than driving the
     *        cart, moving or parked.
     *
     * Each tick of burn stores as much energy as a tick of driving uses, so fuel goes as far as it
     * would without the upgrade. Lights new fuel only while the battery isn't full.
     *
     * @param level The level to work in.
     */
    private void chargeFromFuel(ServerLevel level) {
        if (energy >= energyCapacity()) {
            return;
        }
        if (litTime <= 0) {
            burnFuel(level);
        }
        if (litTime > 0) {
            energy = Math.min(energyCapacity(), energy + Config.locomotiveEnergyPerTick);
        }
    }

    /**
     * @brief Draws energy from whatever's under the cart that gives it out.
     *
     * The rail itself (for other mods' charging rails), then the block beneath the rail (e.g. a
     * battery).
     *
     * The mod's own charging rail pushes energy into the cart instead, so it's skipped here rather
     * than charging the cart twice over.
     *
     * @param level The level to work in.
     * @see transport.rail.ChargingRailBlockEntity
     */
    private void chargeFromTrack(ServerLevel level) {
        if (energy >= energyCapacity()) {
            return;
        }
        BlockPos rail = getCurrentBlockPosOrRailBelow();
        if (!(level.getBlockEntity(rail) instanceof ChargingRailBlockEntity)) {
            drawFrom(level, rail, null);
        }
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

    /** @brief Works out which way the engine should drive this tick. */
    private void updateHeading() {
        Vec3 motion = getDeltaMovement().horizontal();
        boolean stopped = motion.lengthSqr() < STOPPED_SPEED_SQR;
        if (isHandbrakeOn()) {
            // The handbrake cuts the engine, and no new fuel is lit while it's on
            heading = Vec3.ZERO;
        } else if (stopped && getFirstPassenger() instanceof ServerPlayer rider
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

    /**
     * @brief Lights the next fuel item, the same way a furnace does, but burning longer.
     *
     * @param level The level to work in.
     */
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

    /**
     * @brief Same engine force as the furnace minecart, applied along the heading; or the
     *        handbrake.
     */
    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        if (isHandbrakeOn()) {
            double speed = deltaMovement.horizontalDistance();
            double slowed = Math.max(0.0, speed * BRAKE_KEEP - BRAKE_DROP);
            double scale = speed > 0.0 ? slowed / speed : 0.0;
            return super.applyNaturalSlowdown(deltaMovement.multiply(scale, 1.0, scale));
        }
        // With the energy upgrade, burning fuel only charges the battery; the battery drives
        boolean powered = upgrades.has(CartUpgrade.ENERGY) ? electric : litTime > 0;
        if (powered && heading.lengthSqr() > 0) {
            deltaMovement = deltaMovement.multiply(0.8, 0.0, 0.8).add(heading.scale(PUSH));
            if (isInWater()) {
                deltaMovement = deltaMovement.scale(0.1);
            }
        }
        return super.applyNaturalSlowdown(deltaMovement);
    }

    /**
     * @brief The linking bar and upgrades first; then right-click to ride, shift-right-click to
     *        open the fuel and storage slots.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult linked = transport.interact(player, hand);
        if (linked != InteractionResult.PASS) {
            return linked;
        }
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

    /** @brief Opened by the inventory key while riding. */
    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (player.level() instanceof ServerLevel) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
    }

    @Override
    public void push(Entity entity) {
        if (!transport.isLinkedTo(entity) && !upgrades.pushAside(entity)) {
            super.push(entity);
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !transport.isLinkedTo(entity) && super.canCollideWith(entity);
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        upgrades.dropAll(level);
    }

    @Override
    public void remove(RemovalReason reason) {
        transport.onRemove(reason);
        super.remove(reason);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(user.getLivingEntity()));
    }

    /** @brief Only players and villagers ride a locomotive: it doesn't pick up other mobs. */
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return (passenger instanceof Player || passenger instanceof Villager) && super.canAddPassenger(passenger);
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    /**
     * @brief Hoppers (and anything else inserting) can only put burnable items in the fuel slot.
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != FUEL_SLOT || level().fuelValues().isFuel(stack);
    }

    /** @brief Like a furnace, only empty buckets can be pulled back out of the fuel slot. */
    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack stack) {
        return slot != FUEL_SLOT || stack.is(Items.BUCKET);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LocomotiveCartMenu(containerId, inventory, this, data);
    }

    /** @brief The energy upgrade's storage, for other mods' cables, chargers and the like. */
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

    /**
     * @brief Takes energy in only; nothing can draw it back out.
     *
     * Takes nothing once the upgrade is removed.
     */
    private class Battery implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) {
                return 0;
            }
            int received = Math.max(0, Math.min(Math.min(maxReceive, MAX_ENERGY_PUSHED), energyCapacity() - energy));
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
        return TransportItems.LOCOMOTIVE_CART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(TransportItems.LOCOMOTIVE_CART.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
        output.putInt("Energy", energy);
        output.putBoolean("Handbrake", isHandbrakeOn());
        output.putDouble("HeadingX", heading.x);
        output.putDouble("HeadingZ", heading.z);
        upgrades.save(output);
        transport.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        energy = input.getIntOr("Energy", 0);
        entityData.set(DATA_HANDBRAKE, input.getBooleanOr("Handbrake", false));
        heading = new Vec3(input.getDoubleOr("HeadingX", 0.0), 0.0, input.getDoubleOr("HeadingZ", 0.0));
        upgrades.load(input);
        transport.load(input);
    }
}
