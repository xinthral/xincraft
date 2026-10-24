package com.xincraft.xincraft.block;

import java.util.ArrayList;
import java.util.List;
import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The links from one buoy (BuoyBlock, lower half) to the buoys next to it on its path: up to
// MAX_LINKS, each no more than LINK_RANGE blocks away, made with a linking bar (item.LinkingBarItem).
// Both buoys hold the link. Synced to clients, which steer ridden boats along the path and draw it
// for rose-tinted glasses. Removed, a buoy unlinks its loaded partners and drops a bar per link; a
// link to a buoy that's gone, or doesn't link back, is ignored and dropped (see links).
public class BuoyBlockEntity extends BlockEntity {
    public static final int MAX_LINKS = 2;
    public static final double LINK_RANGE = 32.0;

    private final List<BlockPos> links = new ArrayList<>(MAX_LINKS);

    public BuoyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUOY.get(), pos, state);
    }

    // The buoy linked at pos, if it's loaded
    public static @Nullable BuoyBlockEntity at(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof BuoyBlockEntity buoy ? buoy : null;
    }

    // The buoys this one links to. A loaded partner that's no longer a buoy linked back to this one
    // is dropped (on the server, for good); one in an unloaded chunk is kept, as its links can't be checked.
    public List<BlockPos> links() {
        if (level != null) {
            boolean dropped = links.removeIf(pos -> level.isLoaded(pos) && !(at(level, pos) instanceof BuoyBlockEntity other
                && other.links.contains(worldPosition)));
            if (dropped && !level.isClientSide()) {
                changed();
            }
        }
        return List.copyOf(links);
    }

    public boolean isLinkedTo(BlockPos pos) {
        return links.contains(pos);
    }

    public boolean isFull() {
        return links().size() >= MAX_LINKS;
    }

    // Joins two buoys, both of which have room; the caller checks range and room
    public static void link(BuoyBlockEntity a, BuoyBlockEntity b) {
        a.links.add(b.worldPosition);
        b.links.add(a.worldPosition);
        a.changed();
        b.changed();
    }

    // Removes all of this buoy's links, from both ends, and returns how many there were
    public int unlinkAll() {
        int count = links.size();
        if (level != null) {
            for (BlockPos pos : links) {
                BuoyBlockEntity other = at(level, pos);
                if (other != null && other.links.remove(worldPosition)) {
                    other.changed();
                }
            }
        }
        links.clear();
        changed();
        return count;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Broken (or washed away), it unhitches from the buoys either side and drops their bars
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            int bars = unlinkAll();
            if (bars > 0) {
                Block.popResource(level, pos, new ItemStack(ModItems.LINKING_BAR.get(), bars));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        links.clear();
        input.read("Links", BlockPos.CODEC.listOf()).ifPresent(links::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Links", BlockPos.CODEC.listOf(), List.copyOf(links));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
