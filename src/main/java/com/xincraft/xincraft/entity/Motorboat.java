package com.xincraft.xincraft.entity;

import java.util.function.Supplier;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
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
// A player steering a boat moves it on their own client, so the server decides whether it's
// cruising (from the rider's keys and its fuel) and syncs that, and the steering client pushes it.
public class Motorboat extends AbstractBoat implements UpgradableCart, HasCustomInventoryScreen, ContainerEntity {
    public static final int FUEL_SLOT = 0;
    public static final int STORAGE_SLOT = 1;

    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CRUISING = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(Motorboat.class, EntityDataSerializers.BOOLEAN);

    // The engine's push each tick: twice a paddler's 0.04, so about twice paddling speed, and half
    // as much again with the speed upgrade
    private static final float THRUST = 0.08F;
    private static final float SPEED_UPGRADE_THRUST = 1.5F;
    // Cruising this slowly for this long means it's run into something, so the engine cuts out
    private static final double STUCK_SPEED = 0.02;
    private static final int STUCK_TICKS = 20;
    // Most Forge Energy taken in per tick, from the shore or anything else
    private static final int MAX_ENERGY_RECEIVE = 1_000;

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final boolean raft;
    private NonNullList<ItemStack> items = NonNullList.withSize(LocomotiveCart.SLOT_COUNT, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private int energy;
    private int slowTicks;
    private @Nullable Vec3 lastPos;
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
            float thrust = THRUST * (upgrades.has(CartUpgrade.SPEED) ? SPEED_UPGRADE_THRUST : 1.0F);
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            setDeltaMovement(getDeltaMovement().add(-Mth.sin(yaw) * thrust, 0.0, Mth.cos(yaw) * thrust));
        }
        if (level().isClientSide()) {
            spawnEngineParticles();
        }
    }

    private void serverTick(ServerLevel level) {
        // Its motion, worked out from where it's been: the server doesn't move a boat a player steers
        Vec3 motion = lastPos != null ? position().subtract(lastPos) : Vec3.ZERO;
        lastPos = position();
        boolean cruising = updateCruising(motion);
        boolean speedy = upgrades.has(CartUpgrade.SPEED);
        if (litTime > 0) {
            // The speed upgrade burns through fuel twice as fast while cruising
            litTime = Math.max(0, litTime - (cruising && speedy ? 2 : 1));
        }
        boolean powered = false;
        if (upgrades.has(CartUpgrade.ENERGY)) {
            // Fuel and the shore charge the battery, and the engine runs only on the battery
            chargeFromShore(level);
            chargeFromFuel(level);
            int cost = Config.motorboatEnergyPerTick * (speedy ? 2 : 1);
            if (cruising && energy >= cost) {
                energy -= cost;
                powered = true;
            }
        } else if (cruising) {
            if (litTime <= 0) {
                burnFuel(level);
            }
            powered = litTime > 0;
        }
        entityData.set(DATA_CRUISING, cruising && powered);
        entityData.set(DATA_LIT, litTime > 0);
        if (upgrades.has(CartUpgrade.SHRIEKER)) {
            upgrades.clearTrackAhead(level, motion);
        }
        upgrades.tickShimmer(level);
        // Only in water, so it leaves ice roads for boats alone
        if (upgrades.has(CartUpgrade.ICE_BREAKER) && isInWater() && (isCruising() || motion.horizontalDistanceSqr() > 1.0E-3)) {
            breakIceAhead(level, motion);
        }
    }

    // Forward sets it cruising and back stops it; it also stops with nobody steering, or once it's
    // been stuck against something for a second
    private boolean updateCruising(Vec3 motion) {
        boolean cruising = isCruising();
        if (!(getControllingPassenger() instanceof ServerPlayer rider)) {
            return false;
        }
        if (rider.getLastClientInput().forward()) {
            cruising = true;
            slowTicks = 0;
        }
        if (rider.getLastClientInput().backward()) {
            cruising = false;
        }
        if (cruising) {
            slowTicks = motion.horizontalDistance() < STUCK_SPEED ? slowTicks + 1 : 0;
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
        if (!isCruising() && !entityData.get(DATA_LIT)) {
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

    // Upgrades first; then shift-right-click opens the fuel and storage slots, and right-click rides
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        readChestVehicleSaveData(input);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        energy = input.getIntOr("Energy", 0);
        upgrades.load(input);
    }
}
