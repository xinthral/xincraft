package com.xincraft.xincraft.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.inventory.ApothecaryAltarMenu;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.magic.EssenceHelper;
import com.xincraft.xincraft.magic.EssenceReservoir;
import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The apothecary altar burns plants into elemental essence, which flows into the elemental flora
// (FloraBlock) within Config.altarRange blocks. Which elements a plant gives is set by the item tags
// xincraft:essence/<element> (registry.ModTags); only plants in at least one of them go in. Each
// burnt plant gives Config.altarEssencePerElement of each of its elements, after
// Config.altarBurnTicks of burning furnace fuel.
//
// It only lights new fuel and makes progress while a flora of one of the plant's elements has room,
// so plants and fuel aren't wasted with nowhere for the essence to go. Fuel already burning burns
// out like in a furnace. Hoppers put plants in from above and fuel from the sides, and take empty
// buckets out from below.
public class ApothecaryAltarBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int PLANT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    // Menu data entries: fuel burn left, plant burn progress (both out of 1000), and bitmasks (by
    // Element ordinal) of the elements with a flora in range, and with one that has room
    public static final int DATA_COUNT = 4;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int[] SLOTS_FOR_UP = {PLANT_SLOT};
    private static final int[] SLOTS_FOR_SIDES = {FUEL_SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    // Burn time left in the current fuel item, in furnace ticks
    private int litTime;
    private int litDuration;
    private int burnProgress;
    // From the last scan for flora
    private int floraMask;
    private int roomMask;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime > 0 ? Math.max(1, (int) ((long) litTime * 1000 / Math.max(1, litDuration))) : 0;
                case 1 -> (int) ((long) burnProgress * 1000 / Math.max(1, Config.altarBurnTicks));
                case 2 -> floraMask;
                default -> roomMask;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ApothecaryAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APOTHECARY_ALTAR.get(), pos, state);
    }

    // Whether the altar burns this item: it's in at least one xincraft:essence/<element> tag
    public static boolean isPlant(ItemStack stack) {
        return elementMask(stack) != 0;
    }

    // The elements a plant gives, as a bitmask by Element ordinal
    public static int elementMask(ItemStack stack) {
        int mask = 0;
        if (!stack.isEmpty()) {
            for (Element element : Element.values()) {
                if (stack.is(ModTags.essence(element))) {
                    mask |= 1 << element.ordinal();
                }
            }
        }
        return mask;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ApothecaryAltarBlockEntity altar) {
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SCAN_INTERVAL_TICKS) == 0) {
            altar.scan(level);
        }
        boolean changed = false;
        boolean canBurn = (elementMask(altar.items.get(PLANT_SLOT)) & altar.roomMask) != 0;
        if (altar.litTime <= 0 && canBurn) {
            changed = altar.lightFuel(level);
        }
        if (altar.litTime > 0) {
            altar.litTime--;
            if (canBurn && ++altar.burnProgress >= Config.altarBurnTicks) {
                altar.burnProgress = 0;
                altar.burnPlant(level);
            }
            changed = true;
        }
        boolean lit = altar.litTime > 0;
        if (state.getValue(ApothecaryAltarBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(ApothecaryAltarBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) {
            altar.setChanged();
        }
    }

    private Map<Element, List<EssenceReservoir>> findFlora(ServerLevel level) {
        Map<Element, List<EssenceReservoir>> flora = new EnumMap<>(Element.class);
        for (EssenceReservoir reservoir : EssenceHelper.findReservoirs(level, worldPosition, Config.altarRange)) {
            flora.computeIfAbsent(reservoir.getElement(), element -> new ArrayList<>()).add(reservoir);
        }
        return flora;
    }

    private void scan(ServerLevel level) {
        floraMask = 0;
        roomMask = 0;
        findFlora(level).forEach((element, reservoirs) -> {
            floraMask |= 1 << element.ordinal();
            if (reservoirs.stream().anyMatch(reservoir -> reservoir.getEssence() < reservoir.getCapacity())) {
                roomMask |= 1 << element.ordinal();
            }
        });
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

    // Burns one plant, sending its essence to the flora around, with a stream of the element's
    // colour to each flora that takes some. Flora are looked up afresh, in case any have gone.
    private void burnPlant(ServerLevel level) {
        ItemStack plant = items.get(PLANT_SLOT);
        int mask = elementMask(plant);
        if (mask == 0) {
            return;
        }
        Map<Element, List<EssenceReservoir>> flora = findFlora(level);
        for (Element element : Element.values()) {
            if ((mask & 1 << element.ordinal()) == 0) {
                continue;
            }
            for (EssenceReservoir filled : EssenceHelper.fill(flora.getOrDefault(element, List.of()), Config.altarEssencePerElement)) {
                if (filled instanceof BlockEntity target) {
                    stream(level, element, target.getBlockPos());
                }
            }
        }
        plant.shrink(1);
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
        scan(level);
    }

    // A line of the element's dust from the basin to the flora
    private void stream(ServerLevel level, Element element, BlockPos target) {
        Vec3 from = Vec3.atCenterOf(worldPosition).add(0.0, 0.7, 0.0);
        Vec3 to = Vec3.atCenterOf(target);
        int steps = Math.max(1, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, i / (double) steps);
            level.sendParticles(element.particle(), point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // The container: a plant slot and a fuel slot

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == PLANT_SLOT) {
            return isPlant(stack);
        }
        return level != null && level.fuelValues().isFuel(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        // From below, the fuel slot too, to take out empty buckets
        return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot == FUEL_SLOT && stack.is(Items.BUCKET);
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ApothecaryAltarMenu(containerId, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        burnProgress = input.getIntOr("BurnProgress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
        output.putInt("BurnProgress", burnProgress);
    }
}
