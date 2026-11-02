package com.xincraft.xincraft.magic;

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

// The apothecary altar's plant slot and fuel slot (with the flame between them), the burn progress
// arrow, which elements have flora in range, and the transmutation's base, material and result slots
// (see magic.client.ApothecaryAltarScreen)
public class ApothecaryAltarMenu extends AbstractContainerMenu {
    private static final int ALTAR_SLOTS_END = ApothecaryAltarBlockEntity.SLOT_COUNT;
    private static final int INVENTORY_SLOTS_END = ALTAR_SLOTS_END + 36;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    // Client side: the real slots and data are filled in by syncing from the server
    public ApothecaryAltarMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(ALTAR_SLOTS_END), new SimpleContainerData(ApothecaryAltarBlockEntity.DATA_COUNT));
    }

    public ApothecaryAltarMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MagicMenus.APOTHECARY_ALTAR.get(), containerId);
        checkContainerSize(container, ALTAR_SLOTS_END);
        checkContainerDataCount(data, ApothecaryAltarBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        container.startOpen(inventory.player);

        addSlot(new PlantSlot(container, ApothecaryAltarBlockEntity.PLANT_SLOT, 56, 17));
        addSlot(new FuelSlot(container, ApothecaryAltarBlockEntity.FUEL_SLOT, 56, 53));
        addSlot(new BaseSlot(container, ApothecaryAltarBlockEntity.BASE_SLOT, 85, 56));
        addSlot(new MaterialSlot(container, ApothecaryAltarBlockEntity.MATERIAL_SLOT, 103, 56));
        addSlot(new ResultSlot(container, ApothecaryAltarBlockEntity.RESULT_SLOT, 150, 56));
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

    // How far through burning the current plant it is, for the arrow
    public float getBurnProgress() {
        return Mth.clamp(data.get(1) / 1000.0F, 0.0F, 1.0F);
    }

    // Bitmasks by Element ordinal: elements with a flora in range, and with one that has room
    public int getFloraMask() {
        return data.get(2);
    }

    public int getRoomMask() {
        return data.get(3);
    }

    // The elements the plant in the slot gives
    public int getPlantMask() {
        return ApothecaryAltarBlockEntity.elementMask(slots.get(ApothecaryAltarBlockEntity.PLANT_SLOT).getItem());
    }

    // How far through the transmutation it is, for its arrow
    public float getTransmuteProgress() {
        return Mth.clamp(data.get(4) / 1000.0F, 0.0F, 1.0F);
    }

    // Bitmasks by Element ordinal: the elements the transmutation needs, and those it's short of
    public int getCostMask() {
        return data.get(5);
    }

    public int getShortMask() {
        return data.get(6);
    }

    private boolean isFuel(ItemStack stack) {
        return level.fuelValues().isFuel(stack);
    }

    // Shift-click: the altar's slots go to the player's inventory; from the inventory, plants go to
    // the plant slot (even ones that also burn as fuel, like saplings), fuel to the fuel slot, and
    // transmutation ingredients to their slots
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < ALTAR_SLOTS_END) {
            if (!moveItemStackTo(stack, ALTAR_SLOTS_END, INVENTORY_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ApothecaryAltarBlockEntity.isPlant(stack)) {
            int plant = ApothecaryAltarBlockEntity.PLANT_SLOT;
            if (!moveItemStackTo(stack, plant, plant + 1, false)
                    && !(isFuel(stack) && moveItemStackTo(stack, ApothecaryAltarBlockEntity.FUEL_SLOT, ApothecaryAltarBlockEntity.FUEL_SLOT + 1, false))) {
                return ItemStack.EMPTY;
            }
        } else if (!(isFuel(stack) && moveItemStackTo(stack, ApothecaryAltarBlockEntity.FUEL_SLOT, ApothecaryAltarBlockEntity.FUEL_SLOT + 1, false))
                && !moveItemStackTo(stack, ApothecaryAltarBlockEntity.BASE_SLOT, ApothecaryAltarBlockEntity.MATERIAL_SLOT + 1, false)) {
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

    // Takes what starts a transmutation
    private class BaseSlot extends Slot {
        BaseSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ApothecaryAltarBlockEntity.isBase(level, stack);
        }
    }

    // Takes any transmutation's material, so it can go in before the base
    private class MaterialSlot extends Slot {
        MaterialSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ApothecaryAltarBlockEntity.isMaterial(level, stack);
        }
    }

    // What's been made, to take out
    private static class ResultSlot extends Slot {
        ResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    // Takes only plants the altar can burn
    private static class PlantSlot extends Slot {
        PlantSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ApothecaryAltarBlockEntity.isPlant(stack);
        }
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
