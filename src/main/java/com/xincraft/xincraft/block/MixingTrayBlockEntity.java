package com.xincraft.xincraft.block;

import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModItems;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// What's on a mixing tray: up to three different small flowers, which mix into one exotic pollen as
// soon as the third goes on. Synced to clients so the tray can show them (client.MixingTrayRenderer).
public class MixingTrayBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;

    // The flowers in the order they went on, or the pollen alone in the first slot
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public MixingTrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_TRAY.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    private boolean hasPollen() {
        return items.get(0).is(ModItems.EXOTIC_POLLEN.get());
    }

    private int count() {
        int count = 0;
        while (count < SLOTS && !items.get(count).isEmpty()) {
            count++;
        }
        return count;
    }

    public static boolean isMixable(ItemStack stack) {
        return stack.is(ModTags.SMALL_FLOWERS);
    }

    // Puts one of the stack's flower on the tray, if there's room and it isn't already there;
    // returns whether it went on. The third different flower mixes them all into pollen.
    boolean addFlower(ServerLevel level, ItemStack stack) {
        if (hasPollen() || count() >= SLOTS || !isMixable(stack)) {
            return false;
        }
        for (ItemStack item : items) {
            if (item.is(stack.getItem())) {
                return false;
            }
        }
        items.set(count(), stack.copyWithCount(1));
        level.playSound(null, worldPosition, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (count() == SLOTS) {
            items.clear();
            items.set(0, new ItemStack(ModItems.EXOTIC_POLLEN.get()));
            level.playSound(null, worldPosition, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 0.25,
                worldPosition.getZ() + 0.5, 10, 0.25, 0.1, 0.25, 0.0);
        }
        changed(level);
        return true;
    }

    // Takes the pollen, or else the last flower put on; empty if the tray is empty
    ItemStack takeTop(ServerLevel level) {
        int count = count();
        if (count == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = items.get(count - 1);
        items.set(count - 1, ItemStack.EMPTY);
        changed(level);
        return taken;
    }

    private void changed(ServerLevel level) {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // A fresh list, so slots emptied since the last sync don't linger on clients
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
