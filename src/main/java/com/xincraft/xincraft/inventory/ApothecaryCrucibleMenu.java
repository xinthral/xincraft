package com.xincraft.xincraft.inventory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.item.ElementPowderItem;
import com.xincraft.xincraft.item.MobFilterContents;
import com.xincraft.xincraft.item.MobFilterItem;
import com.xincraft.xincraft.magic.CrucibleInput;
import com.xincraft.xincraft.magic.CrucibleRecipe;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.magic.EssenceHelper;
import com.xincraft.xincraft.magic.EssenceReservoir;
import com.xincraft.xincraft.registry.ModBlocks;
import com.xincraft.xincraft.registry.ModMenuTypes;
import com.xincraft.xincraft.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

// The apothecary crucible: a powder in each top corner, the catalyst between them, and a mob filter
// below. When they make a crucible recipe (magic.CrucibleRecipe), the output is the filter with the
// mob inscribed. Taking it burns the powders and catalyst and, for nature's balance, draws
// Config.crucibleBalanceEssence essence per powder of every other element from the flora around the
// altar beneath (within Config.altarRange). Nothing comes out if any of those flora are short, the
// filter is full, or it already lists the mob; the screen (client.ApothecaryCrucibleScreen) shows
// which.
public class ApothecaryCrucibleMenu extends AbstractContainerMenu {
    public static final int LEFT_SLOT = 0;
    public static final int RIGHT_SLOT = 1;
    public static final int CATALYST_SLOT = 2;
    public static final int FILTER_SLOT = 3;
    public static final int RESULT_SLOT = 4;
    private static final int INVENTORY_SLOTS_START = 5;
    private static final int INVENTORY_SLOTS_END = INVENTORY_SLOTS_START + 36;
    private static final int POWDERS = 2;

    // What's stopping (or not) an inscription, synced for the screen
    public static final int STATUS_NONE = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_NO_FILTER = 2;
    public static final int STATUS_FULL = 3;
    public static final int STATUS_DUPLICATE = 4;
    public static final int STATUS_SHORT = 5;

    private final ContainerLevelAccess access;
    private final Level level;
    private final Container container = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    // Bitmask (by Element ordinal) of the elements whose flora are short, and the status
    private final DataSlot shortMask = DataSlot.standalone();
    private final DataSlot status = DataSlot.standalone();

    // Client side: the slots and data are filled in by syncing from the server
    public ApothecaryCrucibleMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ApothecaryCrucibleMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenuTypes.APOTHECARY_CRUCIBLE.get(), containerId);
        this.access = access;
        this.level = inventory.player.level();

        addSlot(new PowderSlot(container, LEFT_SLOT, 16, 18));
        addSlot(new PowderSlot(container, RIGHT_SLOT, 84, 18));
        addSlot(new Slot(container, CATALYST_SLOT, 50, 34));
        addSlot(new FilterSlot(container, FILTER_SLOT, 50, 55));
        addSlot(new InscribedSlot(result, 0, 124, 34));
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlot(shortMask);
        addDataSlot(status);
    }

    public int getShortMask() {
        return shortMask.get();
    }

    public int getStatus() {
        return status.get();
    }

    // Whether the filter in its slot can't take the mob: it's full, or already lists it
    public boolean isFilterBlocked() {
        return status.get() == STATUS_FULL || status.get() == STATUS_DUPLICATE;
    }

    private CrucibleInput input() {
        return new CrucibleInput(container.getItem(LEFT_SLOT), container.getItem(RIGHT_SLOT),
            container.getItem(CATALYST_SLOT), container.getItem(FILTER_SLOT));
    }

    @Override
    public void slotsChanged(Container changed) {
        access.execute((accessLevel, pos) -> {
            if (accessLevel instanceof ServerLevel serverLevel) {
                refresh(serverLevel, pos);
            }
        });
    }

    // Works out the output and status afresh from what's in the crucible and the flora around
    private void refresh(ServerLevel serverLevel, BlockPos pos) {
        CrucibleInput input = input();
        Optional<RecipeHolder<CrucibleRecipe>> recipe = serverLevel.getServer().getRecipeManager()
            .getRecipeFor(ModRecipes.CRUCIBLE.get(), input, serverLevel);
        ItemStack output = ItemStack.EMPTY;
        int newStatus = STATUS_NONE;
        int newShortMask = 0;
        if (recipe.isPresent()) {
            ItemStack filter = input.filter();
            MobFilterContents.Entry entry = recipe.get().value().entry(input);
            newShortMask = shortfall(serverLevel, pos, input);
            if (!(filter.getItem() instanceof MobFilterItem)) {
                newStatus = STATUS_NO_FILTER;
            } else if (MobFilterItem.contents(filter).contains(entry)) {
                newStatus = STATUS_DUPLICATE;
            } else if (MobFilterItem.isFull(filter)) {
                newStatus = STATUS_FULL;
            } else if (newShortMask != 0) {
                newStatus = STATUS_SHORT;
            } else {
                newStatus = STATUS_READY;
                output = recipe.get().value().assemble(input);
            }
        }
        result.setRecipeUsed(recipe.orElse(null));
        result.setItem(0, output);
        shortMask.set(newShortMask);
        status.set(newStatus);
        broadcastChanges();
    }

    // Essence the inscription draws from each other element's flora
    private static int balanceCost() {
        return Config.crucibleBalanceEssence * POWDERS;
    }

    // The flora in reach of the altar under the crucible, by element
    private static Map<Element, List<EssenceReservoir>> flora(Level level, BlockPos cruciblePos) {
        Map<Element, List<EssenceReservoir>> flora = new EnumMap<>(Element.class);
        for (EssenceReservoir reservoir : EssenceHelper.findReservoirs(level, cruciblePos.below(), Config.altarRange)) {
            flora.computeIfAbsent(reservoir.getElement(), element -> new ArrayList<>()).add(reservoir);
        }
        return flora;
    }

    // Bitmask of the elements whose flora between them hold too little essence for the balance
    private static int shortfall(Level level, BlockPos pos, CrucibleInput input) {
        Map<Element, List<EssenceReservoir>> flora = flora(level, pos);
        int mask = 0;
        for (Element element : CrucibleRecipe.balancing(input)) {
            if (EssenceHelper.total(flora.getOrDefault(element, List.of())) < balanceCost()) {
                mask |= 1 << element.ordinal();
            }
        }
        return mask;
    }

    // Burns the powders and catalyst and draws the balance from the flora, with a stream of each
    // element's dust from every flora drawn on to the crucible
    private void inscribe() {
        CrucibleInput input = input();
        Set<Element> balancing = CrucibleRecipe.balancing(input);
        access.execute((accessLevel, pos) -> {
            if (!(accessLevel instanceof ServerLevel serverLevel)) {
                return;
            }
            Map<Element, List<EssenceReservoir>> flora = flora(serverLevel, pos);
            Vec3 crucible = Vec3.atCenterOf(pos);
            for (Element element : balancing) {
                List<EssenceReservoir> reservoirs = flora.getOrDefault(element, List.of());
                Map<EssenceReservoir, Integer> before = new HashMap<>();
                reservoirs.forEach(reservoir -> before.put(reservoir, reservoir.getEssence()));
                EssenceHelper.drain(reservoirs, balanceCost());
                for (EssenceReservoir reservoir : reservoirs) {
                    if (reservoir.getEssence() < before.get(reservoir) && reservoir instanceof BlockEntity flower) {
                        EssenceHelper.stream(serverLevel, element, Vec3.atCenterOf(flower.getBlockPos()), crucible);
                    }
                }
            }
            serverLevel.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
        });
        // The filter is used up too: the output is its inscribed copy
        for (int slot : new int[] {LEFT_SLOT, RIGHT_SLOT, CATALYST_SLOT, FILTER_SLOT}) {
            container.removeItem(slot, 1);
        }
    }

    // Shift-click: the output and the crucible's slots go to the player's inventory; from the
    // inventory, powders go to the top corners, filters to the bottom and anything else to the
    // catalyst
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_SLOTS_START, INVENTORY_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (slotIndex < INVENTORY_SLOTS_START) {
            if (!moveItemStackTo(stack, INVENTORY_SLOTS_START, INVENTORY_SLOTS_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ElementPowderItem) {
            if (!moveItemStackTo(stack, LEFT_SLOT, RIGHT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof MobFilterItem) {
            if (!moveItemStackTo(stack, FILTER_SLOT, FILTER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, CATALYST_SLOT, CATALYST_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != result && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.APOTHECARY_CRUCIBLE.get());
    }

    // Like a crafting table, the crucible keeps nothing
    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((accessLevel, pos) -> clearContainer(player, container));
    }

    // Takes only elemental powders
    private static class PowderSlot extends Slot {
        PowderSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ElementPowderItem;
        }
    }

    // Takes one mob filter
    private static class FilterSlot extends Slot {
        FilterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof MobFilterItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // The inscribed filter. On the server it's checked again before it can be taken, in case the
    // flora have been drawn on since; taking it does the inscribing.
    private class InscribedSlot extends Slot {
        InscribedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            if (level.isClientSide()) {
                return true;
            }
            boolean[] ready = {true};
            access.execute((accessLevel, pos) -> {
                if (accessLevel instanceof ServerLevel serverLevel && shortfall(serverLevel, pos, input()) != 0) {
                    ready[0] = false;
                    refresh(serverLevel, pos);
                }
            });
            return ready[0];
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            carried.onCraftedBy(player, carried.getCount());
            inscribe();
            super.onTake(player, carried);
        }
    }
}
