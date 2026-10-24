package com.xincraft.xincraft.block;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.magic.EssenceReservoir;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The essence an elemental flora holds (see FloraBlock), up to Config.floraCapacity. The block
// state's FILL shows roughly how full it is; the amount and capacity are also synced to clients, for
// rose-tinted glasses to read (client.EssenceReadout).
public class FloraBlockEntity extends BlockEntity implements EssenceReservoir {
    private int essence;
    // On the client, the server's capacity, which may be configured differently from the client's
    private int syncedCapacity = Config.floraCapacity;

    public FloraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLORA.get(), pos, state);
    }

    @Override
    public Element getElement() {
        return ((FloraBlock) getBlockState().getBlock()).getElement();
    }

    @Override
    public int getEssence() {
        return essence;
    }

    @Override
    public int getCapacity() {
        return level != null && level.isClientSide() ? syncedCapacity : Config.floraCapacity;
    }

    @Override
    public int insert(int amount, boolean simulate) {
        int accepted = Mth.clamp(getCapacity() - essence, 0, Math.max(amount, 0));
        if (!simulate && accepted > 0) {
            setEssence(essence + accepted);
        }
        return accepted;
    }

    @Override
    public int extract(int amount, boolean simulate) {
        int taken = Mth.clamp(amount, 0, essence);
        if (!simulate && taken > 0) {
            setEssence(essence - taken);
        }
        return taken;
    }

    private void setEssence(int value) {
        essence = value;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            int fill = fillFor(essence, getCapacity());
            if (state.getValue(FloraBlock.FILL) != fill) {
                level.setBlock(worldPosition, state.setValue(FloraBlock.FILL, fill), Block.UPDATE_ALL);
            } else {
                // Still let clients know the new amount
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    // Any essence at all shows as at least the first quarter
    private static int fillFor(int essence, int capacity) {
        if (essence <= 0) {
            return 0;
        }
        return Mth.clamp(Mth.ceil(essence * (float) FloraBlock.MAX_FILL / capacity), 1, FloraBlock.MAX_FILL);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        essence = Math.max(input.getIntOr("Essence", 0), 0);
        syncedCapacity = input.getIntOr("Capacity", Config.floraCapacity);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Essence", essence);
    }

    // Sent to clients when the chunk loads, and with each change: the amount, plus the capacity
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putInt("Capacity", getCapacity());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
