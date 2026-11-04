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

/**
 * @brief A block joined to others of its kind with a linking bar: up to maxLinks, each no further
 *        than both blocks reach.
 *
 * Both blocks hold the link. Synced to clients. Removed, it unlinks its loaded partners and drops a
 * bar per link; a link to a block that's gone, or doesn't link back, is ignored and dropped (see
 * links).
 *
 * @see LinkingBarItem
 */
public abstract class LinkedBlockEntity extends BlockEntity {
    private final List<BlockPos> links = new ArrayList<>();

    protected LinkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract int maxLinks();

    /**
     * @brief How far this block's links reach.
     *
     * @return The range, in blocks.
     */
    public abstract double linkRange();

    /**
     * @brief Whether this block can link to that one.
     *
     * @param other The block to link to.
     * @return True if This block can link to that one.
     */
    public abstract boolean accepts(LinkedBlockEntity other);

    /**
     * @brief Whether a linking bar can also connect it to an energy block beside it, which takes no
     *        link.
     *
     * @return True if A linking bar can also connect it to an energy block beside it, which takes
     *     no link.
     */
    public boolean connectsSources() {
        return false;
    }

    /**
     * @brief Names this kind of block's linking bar messages.
     *
     * @return The key used in the message names.
     */
    protected abstract String messageKey();

    public Component message(String suffix, Object... args) {
        return Component.translatable("message.xincraft.linking_bar." + messageKey() + "_" + suffix, args);
    }

    /**
     * @brief The linked block at pos, if it's loaded.
     *
     * @param level The level to work in.
     * @param pos The block's position.
     * @return The block, or null if it isn't loaded.
     */
    public static @Nullable LinkedBlockEntity linkedAt(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof LinkedBlockEntity linked ? linked : null;
    }

    /**
     * @brief How far apart two linked blocks are.
     *
     * @param a One block.
     * @param b The other block.
     * @return The distance, in blocks.
     */
    public static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    /**
     * @brief The blocks this one links to.
     *
     * A loaded partner that no longer links back is dropped, for good on the server. One in an
     * unloaded chunk is kept, as its links can't be checked.
     *
     * @return The linked positions.
     */
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

    /**
     * @brief Joins two blocks, both of which have room; the caller checks range and room.
     *
     * @param a One block.
     * @param b The other block.
     */
    public static void link(LinkedBlockEntity a, LinkedBlockEntity b) {
        a.links.add(b.worldPosition);
        b.links.add(a.worldPosition);
        a.changed();
        b.changed();
    }

    /**
     * @brief Removes all of this block's links, from both ends, and returns how many there were.
     *
     * @return How many links there were.
     */
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

    /**
     * @brief Broken (or washed away), it unlinks from the blocks around it and drops their bars.
     */
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
