package com.xincraft.xincraft.inventory;

import com.xincraft.xincraft.entity.LocomotiveCart;
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

// The locomotive cart's fuel slot (with the furnace flame above it) and single storage slot,
// laid out like the furnace screen (see client.LocomotiveCartScreen)
public class LocomotiveCartMenu extends AbstractContainerMenu {
    private static final int CART_SLOTS_END = LocomotiveCart.SLOT_COUNT;
    private static final int INVENTORY_SLOTS_END = CART_SLOTS_END + 36;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    // Client side: the real slots and burn data are filled in by syncing from the server
    public LocomotiveCartMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(LocomotiveCart.SLOT_COUNT), new SimpleContainerData(1));
    }

    public LocomotiveCartMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.LOCOMOTIVE_CART.get(), containerId);
        checkContainerSize(container, LocomotiveCart.SLOT_COUNT);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        container.startOpen(inventory.player);

        addSlot(new FuelSlot(container, LocomotiveCart.FUEL_SLOT, 56, 53));
        addSlot(new Slot(container, LocomotiveCart.STORAGE_SLOT, 116, 53));
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

    private boolean isFuel(ItemStack stack) {
        return level.fuelValues().isFuel(stack);
    }

    // Shift-click: cart slots go to the player's inventory; from the inventory, fuel goes to
    // the fuel slot (overflowing into storage) and everything else to the storage slot
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < CART_SLOTS_END) {
            if (!moveItemStackTo(stack, CART_SLOTS_END, INVENTORY_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isFuel(stack)) {
            if (!moveItemStackTo(stack, LocomotiveCart.FUEL_SLOT, LocomotiveCart.FUEL_SLOT + 1, false)
                    && !moveItemStackTo(stack, LocomotiveCart.STORAGE_SLOT, LocomotiveCart.STORAGE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, LocomotiveCart.STORAGE_SLOT, LocomotiveCart.STORAGE_SLOT + 1, false)) {
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
