package com.xincraft.xincraft.block;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.magic.EssenceReservoir;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The essence an elemental flora holds (see FloraBlock), up to Config.floraCapacity. Only the server
// knows the amount; the block state's FILL shows roughly how full it is.
public class FloraBlockEntity extends BlockEntity implements EssenceReservoir {
    private int essence;

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
        return Config.floraCapacity;
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Essence", essence);
    }
}
