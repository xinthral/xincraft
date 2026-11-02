package com.xincraft.xincraft.magic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.common.SidedContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The apothecary altar burns plants into elemental essence, which flows into the elemental flora
// (FloraBlock) within Config.altarRange blocks. Which elements a plant gives is set by the item tags
// xincraft:essence/<element> (magic.MagicTags); only plants in at least one of them go in. Each
// burnt plant gives Config.altarEssencePerElement of each of its elements, after
// Config.altarBurnTicks of burning furnace fuel.
//
// It only lights new fuel and makes progress while a flora of one of the plant's elements has room,
// so plants and fuel aren't wasted with nowhere for the essence to go. Fuel already burning burns
// out like in a furnace. Hoppers put plants in from above and fuel from the sides, and take empty
// buckets out from below.
//
// It also transmutes (magic.AltarRecipe): with a base item in its slot and the material beside it, it
// burns the same fuel for the recipe's time, then draws the recipe's essence from the flora, and the
// result comes out in the third slot. It waits, keeping its progress, while the flora are short or
// the result slot is full; taking the base out or changing it starts again. Hoppers put base items
// in from above and materials from the sides, and take results out from below.
public class ApothecaryAltarBlockEntity extends SidedContainerBlockEntity {
    public static final int PLANT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int BASE_SLOT = 2;
    public static final int MATERIAL_SLOT = 3;
    public static final int RESULT_SLOT = 4;
    public static final int SLOT_COUNT = 5;
    // Menu data entries: fuel burn left, plant burn progress (both out of 1000), and bitmasks (by
    // Element ordinal) of the elements with a flora in range, and with one that has room; then
    // transmutation progress (out of 1000), and bitmasks of the elements it needs, and is short of
    public static final int DATA_COUNT = 7;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int[] SLOTS_FOR_UP = {PLANT_SLOT, BASE_SLOT};
    private static final int[] SLOTS_FOR_SIDES = {FUEL_SLOT, MATERIAL_SLOT};
    private static final int[] SLOTS_FOR_DOWN = {FUEL_SLOT, RESULT_SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    // Burn time left in the current fuel item, in furnace ticks
    private int litTime;
    private int litDuration;
    private int burnProgress;
    // From the last scan for flora
    private int floraMask;
    private int roomMask;
    private int costMask;
    private int shortMask;
    private int transmuteProgress;
    private int transmuteTime = 1;
    // The recipe being worked on, to start again if it changes; not saved, so progress survives a reload
    private @Nullable ResourceKey<Recipe<?>> transmuting;
    // On the client, the server's Config.altarRange, for rose-tinted glasses (magic.client.EssenceReadout)
    private int syncedRange = Config.altarRange;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime > 0 ? Math.max(1, (int) ((long) litTime * 1000 / Math.max(1, litDuration))) : 0;
                case 1 -> (int) ((long) burnProgress * 1000 / Math.max(1, Config.altarBurnTicks));
                case 2 -> floraMask;
                case 3 -> roomMask;
                case 4 -> (int) ((long) transmuteProgress * 1000 / Math.max(1, transmuteTime));
                case 5 -> costMask;
                default -> shortMask;
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
        super(MagicBlockEntities.APOTHECARY_ALTAR.get(), pos, state);
    }

    // How far, in blocks, the altar reaches flora (a cube)
    public int getRange() {
        return level != null && level.isClientSide() ? syncedRange : Config.altarRange;
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
                if (stack.is(MagicTags.essence(element))) {
                    mask |= 1 << element.ordinal();
                }
            }
        }
        return mask;
    }

    private static Optional<RecipeHolder<AltarRecipe>> recipeFor(ServerLevel level, ItemStack base) {
        return base.isEmpty() ? Optional.empty()
            : level.getServer().getRecipeManager().getRecipeFor(ModRecipes.ALTAR_TRANSMUTATION.get(), new AltarInput(base, ItemStack.EMPTY), level);
    }

    // Whether the item starts a transmutation. Only the server knows the recipes; the client's guess
    // gets corrected.
    public static boolean isBase(Level level, ItemStack stack) {
        return !(level instanceof ServerLevel server) || recipeFor(server, stack).isPresent();
    }

    // Whether the item is a material in any transmutation, for a player placing it before the base
    public static boolean isMaterial(Level level, ItemStack stack) {
        return !(level instanceof ServerLevel server) || server.getServer().getRecipeManager().getRecipes().stream()
            .anyMatch(holder -> holder.value() instanceof AltarRecipe recipe && recipe.usesMaterial(stack));
    }

    private AltarInput transmuteInput() {
        return new AltarInput(items.get(BASE_SLOT), items.get(MATERIAL_SLOT));
    }

    // Whether it can get on with the recipe, going by the last scan of the flora; finishing checks
    // them again
    private boolean canTransmute(AltarRecipe recipe) {
        ItemStack output = items.get(RESULT_SLOT);
        boolean fits = output.isEmpty() || output.is(recipe.result()) && output.getCount() < output.getMaxStackSize()
            && ItemStack.isSameItemSameComponents(output, recipe.assemble(transmuteInput()));
        return fits && shortMask == 0 && recipe.hasMaterial(transmuteInput());
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ApothecaryAltarBlockEntity altar) {
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SCAN_INTERVAL_TICKS) == 0) {
            altar.scan(level);
        }
        boolean changed = false;
        boolean canBurn = (elementMask(altar.items.get(PLANT_SLOT)) & altar.roomMask) != 0;
        RecipeHolder<AltarRecipe> holder = recipeFor(level, altar.items.get(BASE_SLOT)).orElse(null);
        ResourceKey<Recipe<?>> current = holder == null ? null : holder.id();
        if (holder == null && altar.transmuteProgress > 0 || altar.transmuting != null && !altar.transmuting.equals(current)) {
            altar.transmuteProgress = 0;
            altar.setChanged();
        }
        // Scans at once for a new recipe (or after a reload), so it can't light fuel on an old scan
        if (current != null && !current.equals(altar.transmuting)) {
            altar.scan(level);
        }
        altar.transmuting = current;
        AltarRecipe recipe = holder == null ? null : holder.value();
        boolean canTransmute = recipe != null && altar.canTransmute(recipe);
        if (recipe != null) {
            altar.transmuteTime = recipe.time();
        }
        if (altar.litTime <= 0 && (canBurn || canTransmute)) {
            changed = altar.lightFuel(level);
        }
        if (altar.litTime > 0) {
            altar.litTime--;
            if (canBurn && ++altar.burnProgress >= Config.altarBurnTicks) {
                altar.burnProgress = 0;
                altar.burnPlant(level);
            }
            if (canTransmute && ++altar.transmuteProgress >= recipe.time()) {
                altar.finishTransmuting(level, recipe);
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
        return EssenceHelper.byElement(level, worldPosition, Config.altarRange);
    }

    private void scan(ServerLevel level) {
        floraMask = 0;
        roomMask = 0;
        Map<Element, List<EssenceReservoir>> flora = findFlora(level);
        flora.forEach((element, reservoirs) -> {
            floraMask |= 1 << element.ordinal();
            if (reservoirs.stream().anyMatch(reservoir -> reservoir.getEssence() < reservoir.getCapacity())) {
                roomMask |= 1 << element.ordinal();
            }
        });
        Map<Element, Integer> cost = recipeFor(level, items.get(BASE_SLOT)).map(holder -> holder.value().essence()).orElse(Map.of());
        costMask = 0;
        cost.keySet().forEach(element -> costMask |= 1 << element.ordinal());
        shortMask = EssenceHelper.shortfall(flora, cost);
    }

    // Draws the essence and makes the result; if the flora have been drawn on since the last scan, it
    // waits until they've refilled
    private void finishTransmuting(ServerLevel level, AltarRecipe recipe) {
        Map<Element, List<EssenceReservoir>> flora = findFlora(level);
        shortMask = EssenceHelper.shortfall(flora, recipe.essence());
        if (shortMask != 0) {
            transmuteProgress = recipe.time() - 1;
            return;
        }
        Vec3 basin = Vec3.atCenterOf(worldPosition).add(0.0, 0.7, 0.0);
        recipe.essence().forEach((element, amount) -> EssenceHelper.draw(level, element, flora.getOrDefault(element, List.of()), amount, basin));
        ItemStack result = recipe.assemble(transmuteInput());
        ItemStack output = items.get(RESULT_SLOT);
        if (output.isEmpty()) {
            items.set(RESULT_SLOT, result);
        } else {
            output.grow(1);
        }
        items.get(BASE_SLOT).shrink(1);
        if (recipe.material().isPresent()) {
            items.get(MATERIAL_SLOT).shrink(recipe.materialCount());
        }
        transmuteProgress = 0;
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        scan(level);
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
        EssenceHelper.stream(level, element, Vec3.atCenterOf(worldPosition).add(0.0, 0.7, 0.0), Vec3.atCenterOf(target));
    }

    // The container: a plant slot, a fuel slot, and the transmutation's base, material and result

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
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
        if (level == null) {
            return false;
        }
        return switch (slot) {
            case PLANT_SLOT -> isPlant(stack);
            case FUEL_SLOT -> level.fuelValues().isFuel(stack);
            case BASE_SLOT -> isBase(level, stack);
            // Hoppers only add what the base in the slot needs
            case MATERIAL_SLOT -> level instanceof ServerLevel server
                && recipeFor(server, items.get(BASE_SLOT)).filter(holder -> holder.value().usesMaterial(stack)).isPresent();
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        // From below, the fuel slot too, to take out empty buckets
        return side == Direction.UP ? SLOTS_FOR_UP : side == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && (slot == RESULT_SLOT || slot == FUEL_SLOT && stack.is(Items.BUCKET));
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
        transmuteProgress = input.getIntOr("TransmuteProgress", 0);
        syncedRange = input.getIntOr("Range", Config.altarRange);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
        output.putInt("BurnProgress", burnProgress);
        output.putInt("TransmuteProgress", transmuteProgress);
    }

    // Clients are only told the range, when the chunk loads; the contents go to the menu
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Range", Config.altarRange);
        return tag;
    }
}
