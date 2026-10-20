package com.xincraft.xincraft.inventory;

import com.xincraft.xincraft.block.TurbineBlockEntity;
import com.xincraft.xincraft.registry.ModMenuTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

// A turbine's fuel slot (with the furnace flame above it), plus the energy and water readouts
// (see client.TurbineScreen). Shared by the heat and steam turbines; the heat turbine reports a
// water tank of size 0, so the screen leaves the water gauge out.
public class TurbineMenu extends AbstractContainerMenu {
    private static final int TURBINE_SLOTS_END = 1;
    private static final int INVENTORY_SLOTS_END = TURBINE_SLOTS_END + 36;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    // Client side: the real slot and data are filled in by syncing from the server
    public TurbineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(TurbineBlockEntity.DATA_COUNT));
    }

    public TurbineMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.TURBINE.get(), containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, TurbineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        container.startOpen(inventory.player);

        addSlot(new FuelSlot(container, TurbineBlockEntity.FUEL_SLOT, 80, 53));
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    // Fraction of the current fuel item left to burn, for the flame
    public float getLitProgress() {
        return Mth.clamp(data.get(0) / 1000.0F, 0.0F, 1.0F);
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    public int getEnergy() {
        return data.get(1) | data.get(2) << 15;
    }

    public int getEnergyCapacity() {
        return data.get(3) | data.get(4) << 15;
    }

    public int getWater() {
        return data.get(5);
    }

    public int getWaterCapacity() {
        return data.get(6);
    }

    public boolean hasTank() {
        return getWaterCapacity() > 0;
    }

    private boolean isFuel(ItemStack stack) {
        return level.fuelValues().isFuel(stack);
    }

    // Shift-click: the fuel slot goes to the player's inventory, and fuel from the inventory goes in
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < TURBINE_SLOTS_END) {
            if (!moveItemStackTo(stack, TURBINE_SLOTS_END, INVENTORY_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!isFuel(stack) || !moveItemStackTo(stack, TurbineBlockEntity.FUEL_SLOT, TurbineBlockEntity.FUEL_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    // Takes fuel, plus the empty bucket a burnt-out lava bucket leaves behind
    private class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack) || stack.is(Items.BUCKET);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return stack.is(Items.BUCKET) ? 1 : super.getMaxStackSize(stack);
        }
    }
}
