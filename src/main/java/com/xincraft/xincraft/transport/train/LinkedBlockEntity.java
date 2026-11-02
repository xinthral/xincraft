package com.xincraft.xincraft.transport.train;

import java.util.ArrayList;
import java.util.List;
import com.xincraft.xincraft.transport.TransportItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// A block joined to others of its kind with a linking bar (see LinkingBarItem): up to maxLinks, each
// no further than both blocks reach. Both blocks hold the link. Synced to clients. Removed, it
// unlinks its loaded partners and drops a bar per link; a link to a block that's gone, or doesn't
// link back, is ignored and dropped (see links).
public abstract class LinkedBlockEntity extends BlockEntity {
    private final List<BlockPos> links = new ArrayList<>();

    protected LinkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract int maxLinks();

    // How far this block's links reach
    public abstract double linkRange();

    // Whether this block can link to that one
    public abstract boolean accepts(LinkedBlockEntity other);

    // Names this kind of block's linking bar messages: message.xincraft.linking_bar.<key>_started and so on
    protected abstract String messageKey();

    public Component message(String suffix, Object... args) {
        return Component.translatable("message.xincraft.linking_bar." + messageKey() + "_" + suffix, args);
    }

    // The linked block at pos, if it's loaded
    public static @Nullable LinkedBlockEntity linkedAt(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof LinkedBlockEntity linked ? linked : null;
    }

    // How far apart two linked blocks are
    public static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    // The blocks this one links to. A loaded partner that no longer links back to this one is dropped
    // (on the server, for good); one in an unloaded chunk is kept, as its links can't be checked.
    public List<BlockPos> links() {
        if (level != null) {
            boolean dropped = links.removeIf(pos -> level.isLoaded(pos)
                && !(linkedAt(level, pos) instanceof LinkedBlockEntity other && accepts(other) && other.links.contains(worldPosition)));
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
        return links().size() >= maxLinks();
    }

    // Joins two blocks, both of which have room; the caller checks range and room
    public static void link(LinkedBlockEntity a, LinkedBlockEntity b) {
        a.links.add(b.worldPosition);
        b.links.add(a.worldPosition);
        a.changed();
        b.changed();
    }

    // Removes all of this block's links, from both ends, and returns how many there were
    public int unlinkAll() {
        int count = links.size();
        if (level != null) {
            for (BlockPos pos : links) {
                LinkedBlockEntity other = linkedAt(level, pos);
                if (other != null && other.links.remove(worldPosition)) {
                    other.changed();
                }
            }
        }
        links.clear();
        changed();
        return count;
    }

    protected void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Broken (or washed away), it unlinks from the blocks around it and drops their bars
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            int bars = unlinkAll();
            if (bars > 0) {
                Block.popResource(level, pos, new ItemStack(TransportItems.LINKING_BAR.get(), bars));
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
