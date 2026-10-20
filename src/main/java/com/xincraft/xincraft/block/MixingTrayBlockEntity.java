package com.xincraft.xincraft.block;

import java.util.ArrayList;
import java.util.List;
import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModItems;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

// Remembers which flowers have gone into a mixing tray's mash, so each one has to be different. How
// full the tray is lives in its block state (MixingTrayBlock.LEVEL), which is what shows the mash.
// Also notices honeycomb thrown onto a full tray, which turns the mash into pollen.
public class MixingTrayBlockEntity extends BlockEntity {
    // How often a full tray looks for thrown honeycomb
    private static final int HONEYCOMB_CHECK_TICKS = 4;

    private final List<Item> flowers = new ArrayList<>();

    public MixingTrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_TRAY.get(), pos, state);
    }

    public static boolean isMixable(ItemStack stack) {
        return stack.is(ModTags.SMALL_FLOWERS);
    }

    // Whether this flower can go in: it's a small flower that isn't in the mash already
    boolean accepts(ItemStack stack) {
        return isMixable(stack) && !flowers.contains(stack.getItem());
    }

    void addFlower(ItemStack stack) {
        flowers.add(stack.getItem());
        setChanged();
    }

    void clearFlowers() {
        flowers.clear();
        setChanged();
    }

    // A full tray turns honeycomb thrown onto it into pollen, one honeycomb per batch
    void serverTick(ServerLevel level, BlockState state) {
        if (state.getValue(MixingTrayBlock.LEVEL) != MixingTrayBlock.FULL
                || Math.floorMod(level.getGameTime() + worldPosition.asLong(), HONEYCOMB_CHECK_TICKS) != 0) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition),
                item -> item.isAlive() && item.getItem().is(Items.HONEYCOMB))) {
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) {
                item.discard();
            }
            MixingTrayBlock.finish(level, worldPosition, state);
            return;
        }
    }

    // Breaking a tray of finished pollen drops the pollen; a half-made mash is lost, like compost
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && state.getValue(MixingTrayBlock.LEVEL) == MixingTrayBlock.READY) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModItems.EXOTIC_POLLEN.get()));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        flowers.clear();
        for (String id : input.getStringOr("Flowers", "").split(",")) {
            Identifier key = Identifier.tryParse(id);
            if (!id.isEmpty() && key != null) {
                BuiltInRegistries.ITEM.getOptional(key).ifPresent(flowers::add);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Flowers", String.join(",", flowers.stream().map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).toList()));
    }
}
