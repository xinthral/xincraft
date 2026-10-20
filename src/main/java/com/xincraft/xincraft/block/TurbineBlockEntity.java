package com.xincraft.xincraft.block;

import com.xincraft.xincraft.inventory.TurbineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

// A generator: burns furnace fuel from its one slot into Forge Energy, held in an internal buffer and
// pushed out to whatever's next to it that takes energy (a charging rail, or another mod's cables and
// machines). New fuel is only lit while there's room in the buffer, so a full turbine doesn't eat
// its fuel; an item already burning burns out regardless. Hoppers fill the fuel slot. HeatTurbineBlockEntity and SteamTurbineBlockEntity set
// how fast it burns and how much energy each tick of burning makes.
public abstract class TurbineBlockEntity extends BaseContainerBlockEntity {
    public static final int FUEL_SLOT = 0;
    // Most energy pushed to each neighbour per tick
    public static final int OUTPUT_PER_SIDE = 1_000;
    // Menu data entries: burn left, stored energy and capacity (each split in two, as menu data
    // travels as 16-bit numbers), then water stored and the tank's size (0 with no tank)
    public static final int DATA_COUNT = 7;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    // Burn time left in the current fuel item, in the fuel's own (furnace) ticks
    private int litTime;
    private int litDuration;
    private int energy;
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new Output());

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime > 0 ? Math.max(1, (int) ((long) litTime * 1000 / Math.max(1, litDuration))) : 0;
                case 1 -> energy & 0x7FFF;
                case 2 -> energy >>> 15;
                case 3 -> energyCapacity() & 0x7FFF;
                case 4 -> energyCapacity() >>> 15;
                case 5 -> waterAmount();
                default -> waterCapacity();
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected TurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract int energyCapacity();

    // Energy made per tick while burning
    protected abstract int energyPerTick();

    // How many ticks of the fuel's burn time are used up each tick
    protected int burnSpeed() {
        return 1;
    }

    // Whether everything besides fuel is there to run this tick (the steam turbine needs water)
    protected boolean canRun() {
        return true;
    }

    // Called each tick the turbine runs, to use up anything besides fuel
    protected void onRun() {}

    protected int waterAmount() {
        return 0;
    }

    protected int waterCapacity() {
        return 0;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, TurbineBlockEntity turbine) {
        boolean changed = turbine.burn(level);
        turbine.pushEnergy(level);
        boolean lit = turbine.litTime > 0;
        if (state.getValue(AbstractFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) {
            turbine.setChanged();
        }
    }

    // Burns fuel into energy; returns whether anything changed. New fuel is only lit while there's
    // room in the buffer, but fuel that's already burning burns out like in a furnace, its energy
    // going to waste once the buffer is full.
    private boolean burn(ServerLevel level) {
        if (!canRun()) {
            return false;
        }
        boolean changed = false;
        if (litTime <= 0) {
            if (energy + energyPerTick() > energyCapacity()) {
                return false;
            }
            changed = lightFuel(level);
        }
        if (litTime > 0) {
            litTime = Math.max(0, litTime - burnSpeed());
            energy = Math.min(energyCapacity(), energy + energyPerTick());
            onRun();
            changed = true;
        }
        return changed;
    }

    // Lights the next fuel item, the same way a furnace does
    private boolean lightFuel(ServerLevel level) {
        ItemStack fuel = items.get(FUEL_SLOT);
        int duration = level.fuelValues().burnDuration(fuel);
        if (duration <= 0) {
            return false;
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
        return true;
    }

    // Hands energy to every neighbour that takes it, such as a charging rail
    private void pushEnergy(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            if (energy <= 0) {
                return;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null || neighbour instanceof TurbineBlockEntity) {
                continue;
            }
            neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(target -> {
                if (target.canReceive()) {
                    energy -= target.receiveEnergy(Math.min(OUTPUT_PER_SIDE, energy), false);
                }
            });
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
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
        energyCap = LazyOptional.of(() -> new Output());
    }

    // Lets other mods' cables and machines draw the stored energy out; takes none in
    private class Output implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.max(0, Math.min(Math.min(maxExtract, OUTPUT_PER_SIDE), energy));
            if (!simulate && extracted > 0) {
                energy -= extracted;
                setChanged();
            }
            return extracted;
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
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    // The container: one fuel slot

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    // Hoppers (and anything else inserting) can only put in fuel
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return level != null && level.fuelValues().isFuel(stack);
    }

    // Like a furnace, only empty buckets can be pulled back out
    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new TurbineMenu(containerId, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        energy = input.getIntOr("Energy", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
        output.putInt("Energy", energy);
    }
}
