package com.xincraft.xincraft.entity;

import java.util.function.Supplier;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.block.BuoyBlock;
import com.xincraft.xincraft.block.BuoyBlockEntity;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

// A boat with a propeller and an engine. Press forward to set it cruising: it keeps going under
// power, burning fuel, while you steer, until you press back, it runs out of fuel, or it runs into
// something. The engine only pushes in water. It has the locomotive cart's fuel slot (fuel lasting
// Config.motorboatFuelMultiplier times as long as in a furnace) and storage slot, opened with the
// inventory key while riding or by shift-right-clicking. Takes upgrades (see CartUpgrades): shrieker,
// energy (a Forge Energy battery that fuel charges, and energy blocks on the shore top up), ice
// breaker and speed.
//
// With the RC upgrade, a cruising motorboat that comes across a buoy path (block.BuoyBlock, see
// BuoyPath) steers itself from buoy to buoy along it, slowing for sharp turns, and keeps cruising
// with nobody aboard until the path ends or it runs aground. Heading for a buoy that's switched off,
// it cuts its engine, stops short of the buoy and waits there, burning no fuel, until the buoy is
// switched back on. Rose-tinted glasses start and stop one on a path with nobody steering.
//
// A player steering a boat moves it on their own client, so the server decides whether it's
// cruising (from the rider's keys and its fuel) and syncs that, and the steering client pushes it.
public class Motorboat extends AbstractBoat implements UpgradableCart, HasCustomInventoryScreen, ContainerEntity {
    public static final int FUEL_SLOT = 0;
    public static final int STORAGE_SLOT = 1;

    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CRUISING = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.BOOLEAN);
    // Waiting at a switched-off buoy, engine idle: the propeller stops and the exhaust clears
    private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.BOOLEAN);

    // The engine's push each tick: twice a paddler's 0.04, so about twice paddling speed, and half
    // as much again with the speed upgrade
    private static final float THRUST = 0.08F;
    private static final float SPEED_UPGRADE_THRUST = 1.5F;
    // Cruising this slowly for this long means it's run into something, so the engine cuts out
    private static final double STUCK_SPEED = 0.02;
    private static final int STUCK_TICKS = 20;
    // Most Forge Energy taken in per tick, from the shore or anything else
    private static final int MAX_ENERGY_RECEIVE = 1_000;
    // Following a buoy path: most degrees it turns each tick, and how much of its sideways drift is kept
    private static final float PATH_TURN = 20.0F;
    private static final double PATH_DRIFT = 0.5;
    // How close to a buoy it gets before heading for the next one, which also rounds off the corner
    private static final double ARRIVE_DISTANCE = 2.5;
    // Turns sharper than this are taken slower: from FAST_CORNER_SPEED for a slight turn down to
    // SLOW_CORNER_SPEED for a hairpin (or the end of the path), in blocks a tick
    private static final double SLOW_TURN_DEGREES = 30.0;
    private static final double FAST_CORNER_SPEED = 0.5;
    private static final double SLOW_CORNER_SPEED = 0.15;
    // Share of its speed kept each tick while braking for a turn, and how many blocks (per block a
    // tick of speed) before the turn it starts braking, never less than MIN_BRAKE_DISTANCE
    private static final double CORNER_BRAKE = 0.6;
    private static final double BRAKE_DISTANCE_PER_SPEED = 3.0;
    private static final double MIN_BRAKE_DISTANCE = 2.0;
    private static final int JOIN_INTERVAL_TICKS = 10;
    // Heading for a buoy that's switched off: within HOLD_DISTANCE the engine cuts out and it slows
    // to stop STOP_DISTANCE short of the buoy, allowed HOLD_SLOWING blocks a tick of speed for each
    // block it still has to go
    private static final double HOLD_DISTANCE = 6.0;
    private static final double STOP_DISTANCE = 2.0;
    private static final double HOLD_SLOWING = 0.25;

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final boolean raft;
    private NonNullList<ItemStack> items = NonNullList.withSize(LocomotiveCart.SLOT_COUNT, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private int energy;
    private int slowTicks;
    private @Nullable Vec3 lastPos;
    // Set by rose-tinted glasses, for the next server tick
    private boolean startRequested;
    private boolean stopRequested;
    // The buoy path it's following: the buoy it's heading for, and the one it came from
    private @Nullable BlockPos pathTarget;
    private @Nullable BlockPos pathPrevious;
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new Battery());

    // The locomotive screen's data (LocomotiveCartMenu): burn left, stored energy and capacity
    // (split in two for 16-bit menu data), and whether the energy upgrade is fitted
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
            return LocomotiveCart.DATA_COUNT;
        }
    };

    public Motorboat(EntityType<? extends Motorboat> type, Level level, Supplier<Item> dropItem, boolean raft) {
        super(type, level, dropItem);
        this.raft = raft;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_CRUISING, false);
        builder.define(DATA_LIT, false);
        builder.define(DATA_WAITING, false);
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        // The same as vanilla's boat and raft
        return raft ? dimensions.height() * 0.8888889F : dimensions.height() / 3.0F;
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    public boolean isWaiting() {
        return entityData.get(DATA_WAITING);
    }

    public boolean isCruising() {
        return entityData.get(DATA_CRUISING);
    }

    private static int energyCapacity() {
        return Config.motorboatEnergyCapacity;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel);
        }
        super.tick();
        // The steering client (or the server, with nobody steering) pushes it while cruising
        if (isCruising() && isLocalInstanceAuthoritative() && isInWater()) {
            if (!followPath()) {
                float thrust = THRUST * (upgrades.has(CartUpgrade.SPEED) ? SPEED_UPGRADE_THRUST : 1.0F);
                float yaw = getYRot() * Mth.DEG_TO_RAD;
                setDeltaMovement(getDeltaMovement().add(-Mth.sin(yaw) * thrust, 0.0, Mth.cos(yaw) * thrust));
            }
        }
        if (level().isClientSide()) {
            spawnEngineParticles();
        }
    }

    private void serverTick(ServerLevel level) {
        // Its motion, worked out from where it's been: the server doesn't move a boat a player steers
        Vec3 motion = lastPos != null ? position().subtract(lastPos) : Vec3.ZERO;
        lastPos = position();
        if (!upgrades.has(CartUpgrade.RC)) {
            clearPath();
        } else if (getControllingPassenger() != null) {
            // The rider's client steers it along the path; the server keeps track of where it is
            // too, to know when it's waiting at a buoy
            if (isCruising()) {
                updatePath();
            }
        } else if (pathTarget == null && isCruising()) {
            // e.g. its rider just got out: carry on along the path it was on, if any
            tryJoinPath();
        }
        boolean held = isHeld();
        boolean cruising = updateCruising(motion, held);
        // Waiting at a switched-off buoy, the engine is off and no fuel burns at all
        boolean waiting = cruising && held;
        boolean speedy = upgrades.has(CartUpgrade.SPEED);
        if (litTime > 0 && !waiting) {
            // The speed upgrade burns through fuel twice as fast while cruising
            litTime = Math.max(0, litTime - (cruising && speedy ? 2 : 1));
        }
        boolean powered = false;
        if (upgrades.has(CartUpgrade.ENERGY)) {
            // Fuel and the shore charge the battery, and the engine runs only on the battery
            chargeFromShore(level);
            if (!waiting) {
                chargeFromFuel(level);
            }
            int cost = Config.motorboatEnergyPerTick * (speedy ? 2 : 1);
            if (cruising && !waiting && energy >= cost) {
                energy -= cost;
                powered = true;
            }
        } else if (cruising && !waiting) {
            if (litTime <= 0) {
                burnFuel(level);
            }
            powered = litTime > 0;
        }
        // Still cruising while it waits, so it sets off again once the buoy is switched back on
        entityData.set(DATA_CRUISING, cruising && (powered || waiting));
        entityData.set(DATA_WAITING, waiting);
        entityData.set(DATA_LIT, litTime > 0);
        if (upgrades.has(CartUpgrade.SHRIEKER)) {
            upgrades.clearTrackAhead(level, motion);
        }
        upgrades.tickShimmer(level);
        // Only in water, so it leaves ice roads for boats alone
        if (upgrades.has(CartUpgrade.ICE_BREAKER) && isInWater() && (isCruising() || motion.horizontalDistanceSqr() > 1.0E-3)) {
            breakIceAhead(level, motion);
        }
        onServerTick(level, motion);
    }

    // For subclasses: run at the end of each server tick, with how far the boat moved this tick
    protected void onServerTick(ServerLevel level, Vec3 motion) {}

    private Vec3 heading() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    // Joins the buoy path it's on, if any; returns whether it did
    private boolean tryJoinPath() {
        BuoyPath.Join join = BuoyPath.join(level(), position(), heading(), legReach());
        if (join == null) {
            return false;
        }
        pathTarget = join.target();
        pathPrevious = join.previous();
        return true;
    }

    // The longest leg between buoys it can follow: further with the advanced antenna
    private double legReach() {
        return upgrades.has(CartUpgrade.ANTENNA) ? BuoyBlockEntity.ADVANCED_LINK_RANGE : BuoyBlockEntity.LINK_RANGE;
    }

    private void clearPath() {
        pathTarget = null;
        pathPrevious = null;
    }

    // With the RC upgrade, steers along the buoy path: toward the buoy ahead, moving on to the next
    // as it comes up, and slowing for a sharp turn there. Without a path, it looks for one now and
    // then. Returns whether it's braking for a turn, when the engine shouldn't push.
    private boolean followPath() {
        if (!updatePath()) {
            return false;
        }
        if (horizontalCollision) {
            // Run aground (or into something): it just stops
            clearPath();
            if (!level().isClientSide()) {
                stopRequested = true;
            }
            return false;
        }
        Level level = level();
        Vec3 here = position();
        Vec3 target = BuoyPath.point(pathTarget);
        float targetYaw = (float) (Mth.atan2(-(target.x - getX()), target.z - getZ()) * Mth.RAD_TO_DEG);
        setYRot(getYRot() + Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -PATH_TURN, PATH_TURN));
        Vec3 forward = heading();
        Vec3 motion = getDeltaMovement();
        double along = motion.x * forward.x + motion.z * forward.z;
        Vec3 drift = new Vec3(motion.x - forward.x * along, 0.0, motion.z - forward.z * along);
        double toTarget = target.subtract(here).horizontalDistance();
        boolean braking;
        if (isHeld()) {
            // Engine off, easing to a stop short of the switched-off buoy
            along = Math.min(along, Math.max(0.0, (toTarget - STOP_DISTANCE) * HOLD_SLOWING));
            braking = true;
        } else {
            double cornerSpeed = cornerSpeed(level, here, target);
            braking = along > cornerSpeed
                && toTarget < ARRIVE_DISTANCE + Math.max(MIN_BRAKE_DISTANCE, along * BRAKE_DISTANCE_PER_SPEED);
            if (braking) {
                along = Math.max(cornerSpeed, along * CORNER_BRAKE);
            }
        }
        setDeltaMovement(forward.x * along + drift.x * PATH_DRIFT, motion.y, forward.z * along + drift.z * PATH_DRIFT);
        return braking;
    }

    // Keeps track of where it is on a buoy path (with the RC upgrade): looks for one to join now and
    // then if it isn't on one, and moves on to the next buoy as it comes up, but not past a buoy
    // that's switched off. Returns whether it's on a path.
    private boolean updatePath() {
        if (!upgrades.has(CartUpgrade.RC)) {
            clearPath();
            return false;
        }
        if (pathTarget == null && (tickCount % JOIN_INTERVAL_TICKS != 0 || !tryJoinPath())) {
            return false;
        }
        Level level = level();
        if (level.isLoaded(pathTarget) && BuoyBlockEntity.at(level, pathTarget) == null) {
            // The buoy it was heading for is gone
            clearPath();
            return false;
        }
        if (!BuoyBlock.isUnlit(level, pathTarget)
                && BuoyPath.point(pathTarget).subtract(position()).horizontalDistance() < ARRIVE_DISTANCE) {
            BlockPos next = BuoyPath.next(level, pathTarget, pathPrevious, legReach());
            if (next == null) {
                // The end of the path
                clearPath();
                return false;
            }
            pathPrevious = pathTarget;
            pathTarget = next;
        }
        return true;
    }

    // Whether it's waiting at (or pulling up to) a buoy that's switched off
    private boolean isHeld() {
        return pathTarget != null && BuoyBlock.isUnlit(level(), pathTarget)
            && BuoyPath.point(pathTarget).subtract(position()).horizontalDistance() < HOLD_DISTANCE;
    }

    // How fast it can take the turn at the buoy ahead: full speed for a slight one, slower the
    // sharper it is, and slowest at the end of the path
    private double cornerSpeed(Level level, Vec3 here, Vec3 target) {
        BlockPos after = BuoyPath.next(level, pathTarget, pathPrevious, legReach());
        if (after == null) {
            return level.isLoaded(pathTarget) ? SLOW_CORNER_SPEED : Double.MAX_VALUE;
        }
        Vec3 into = target.subtract(here).horizontal();
        Vec3 out = BuoyPath.point(after).subtract(target).horizontal();
        if (into.lengthSqr() < 1.0E-6 || out.lengthSqr() < 1.0E-6) {
            return Double.MAX_VALUE;
        }
        double turn = Math.toDegrees(Math.acos(Math.clamp(into.normalize().dot(out.normalize()), -1.0, 1.0)));
        if (turn <= SLOW_TURN_DEGREES) {
            return Double.MAX_VALUE;
        }
        double sharpness = Math.min(1.0, (turn - SLOW_TURN_DEGREES) / (180.0 - SLOW_TURN_DEGREES));
        return Mth.lerp(sharpness, FAST_CORNER_SPEED, SLOW_CORNER_SPEED);
    }

    // Forward sets it cruising and back stops it, as do rose-tinted glasses with nobody aboard. With
    // nobody steering it only keeps going along a buoy path, and it stops once it's been stuck
    // against something for a second (but not while it's waiting at a switched-off buoy).
    private boolean updateCruising(Vec3 motion, boolean held) {
        boolean cruising = (isCruising() || startRequested) && !stopRequested;
        if (startRequested) {
            slowTicks = 0;
        }
        startRequested = false;
        stopRequested = false;
        if (getControllingPassenger() instanceof ServerPlayer rider) {
            if (rider.getLastClientInput().forward()) {
                cruising = true;
                slowTicks = 0;
            }
            if (rider.getLastClientInput().backward()) {
                cruising = false;
            }
        } else if (pathTarget == null) {
            return false;
        }
        if (cruising) {
            // Stopped at a switched-off buoy isn't stuck
            slowTicks = motion.horizontalDistance() < STUCK_SPEED && !held ? slowTicks + 1 : 0;
            if (slowTicks > STUCK_TICKS) {
                cruising = false;
            }
        }
        return cruising;
    }

    // Lights the next fuel item, the same way a furnace does, but burning longer
    private void burnFuel(ServerLevel level) {
        ItemStack fuel = items.get(FUEL_SLOT);
        int duration = (int) Math.round(level.fuelValues().burnDuration(fuel) * Config.motorboatFuelMultiplier);
        if (duration <= 0) {
            return;
        }
        litTime = duration;
        litDuration = duration;
        if (fuel.getCount() == 1) {
            // e.g. a lava bucket leaves an empty bucket behind
            ItemStackTemplate remainder = fuel.getCraftingRemainder();
            items.set(FUEL_SLOT, remainder != null ? remainder.create() : ItemStack.EMPTY);
        } else {
            fuel.shrink(1);
        }
    }

    // With the energy upgrade, burning fuel charges the battery, moving or not. Each tick of burn
    // stores as much energy as a tick of cruising uses. Lights new fuel only while there's room.
    private void chargeFromFuel(ServerLevel level) {
        if (energy >= energyCapacity()) {
            return;
        }
        if (litTime <= 0) {
            burnFuel(level);
        }
        if (litTime > 0) {
            energy = Math.min(energyCapacity(), energy + Config.motorboatEnergyPerTick);
        }
    }

    // Draws energy from energy blocks right next to the boat, such as a turbine or charging block on
    // a dock
    private void chargeFromShore(ServerLevel level) {
        int space = Math.min(MAX_ENERGY_RECEIVE, energyCapacity() - energy);
        if (space <= 0) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(getBoundingBox().getMinPosition()).offset(-1, 0, -1),
                BlockPos.containing(getBoundingBox().getMaxPosition()).offset(1, 0, 1))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                continue;
            }
            int wanted = space;
            int taken = blockEntity.getCapability(ForgeCapabilities.ENERGY, null)
                .map(source -> source.canExtract() ? source.extractEnergy(wanted, false) : 0).orElse(0);
            energy += taken;
            space -= taken;
            if (space <= 0) {
                return;
            }
        }
    }

    // Turns ice just ahead of the boat, at the water's surface, back into water
    private void breakIceAhead(ServerLevel level, Vec3 motion) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 ahead = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x);
        double reach = 1.2 + motion.horizontalDistance() * 3.0;
        for (double forward = 1.0; forward <= reach; forward += 0.5) {
            for (double across = -0.6; across <= 0.6; across += 0.6) {
                Vec3 at = position().add(ahead.scale(forward)).add(side.scale(across));
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos pos = BlockPos.containing(at.x, getY() + dy * 0.6, at.z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.ICE)) {
                        level.levelEvent(2001, pos, Block.getId(state));
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private void spawnEngineParticles() {
        if (!isCruising() && !entityData.get(DATA_LIT) || isWaiting()) {
            return;
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double backX = getX() + Mth.sin(yaw) * 0.9;
        double backZ = getZ() - Mth.cos(yaw) * 0.9;
        if (entityData.get(DATA_LIT) && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.SMOKE, backX, getY() + 0.7, backZ, 0.0, 0.03, 0.0);
        }
        if (isCruising() && isInWater()) {
            level().addParticle(ParticleTypes.SPLASH, backX + random.nextGaussian() * 0.15, getY() + 0.1, backZ + random.nextGaussian() * 0.15,
                0.0, 0.05, 0.0);
            if (upgrades.has(CartUpgrade.ENERGY) && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, backX, getY() + 0.4, backZ, 0.0, 0.0, 0.0);
            }
        }
    }

    // Rose-tinted glasses start or stop a boat with the RC upgrade and nobody steering it that's on a
    // buoy path. Then upgrades; then shift-right-click opens the fuel and storage slots, and
    // right-click rides.
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(ModItems.ROSE_TINTED_GLASSES.get()) && upgrades.has(CartUpgrade.RC)
                && getControllingPassenger() == null) {
            if (!level().isClientSide()) {
                if (pathTarget == null && !tryJoinPath()) {
                    player.sendOverlayMessage(Component.translatable("message.xincraft.motorboat.no_path"));
                    return InteractionResult.FAIL;
                }
                if (isCruising()) {
                    stopRequested = true;
                } else {
                    startRequested = true;
                }
            }
            return InteractionResult.SUCCESS;
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
        return super.interact(player, hand, location);
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
        destroy(level, getDropItem());
        chestVehicleDestroyed(source, level, this);
        upgrades.dropAll(level);
    }

    // The energy upgrade's battery, for other mods' chargers and the like

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

    // The container: the locomotive cart's fuel and storage slots

    @Override
    public int getContainerSize() {
        return LocomotiveCart.SLOT_COUNT;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return removeChestVehicleItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setChestVehicleItem(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return getChestVehicleSlot(slot);
    }

    @Override
    public void clearContent() {
        clearChestVehicleContent();
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return isChestVehicleStillValid(player);
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
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LocomotiveCartMenu(containerId, inventory, this, data);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(user.getLivingEntity()));
    }

    @Override
    public @Nullable ResourceKey<LootTable> getContainerLootTable() {
        return null;
    }

    @Override
    public void setContainerLootTable(@Nullable ResourceKey<LootTable> lootTable) {}

    @Override
    public long getContainerLootTableSeed() {
        return 0;
    }

    @Override
    public void setContainerLootTableSeed(long seed) {}

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return items;
    }

    @Override
    public void clearItemStacks() {
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        addChestVehicleSaveData(output);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
        output.putInt("Energy", energy);
        upgrades.save(output);
        if (pathTarget != null) {
            output.store("PathTarget", BlockPos.CODEC, pathTarget);
        }
        if (pathPrevious != null) {
            output.store("PathPrevious", BlockPos.CODEC, pathPrevious);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        readChestVehicleSaveData(input);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        energy = input.getIntOr("Energy", 0);
        upgrades.load(input);
        pathTarget = input.read("PathTarget", BlockPos.CODEC).orElse(null);
        pathPrevious = input.read("PathPrevious", BlockPos.CODEC).orElse(null);
    }
}
