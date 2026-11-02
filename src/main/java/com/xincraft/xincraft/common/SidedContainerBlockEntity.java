package com.xincraft.xincraft.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jspecify.annotations.Nullable;

// A block entity whose WorldlyContainer methods decide what hoppers and pipes can do on each side.
// Forge's hoppers and pipes go through the item handler capability rather than those methods, and the
// one BaseContainerBlockEntity gives out ignores them, so anything could be pulled out of any slot;
// this gives out handlers that follow them, as vanilla's furnace does. A horizontal side is asked
// about as north. Opening the menu isn't affected, and a caller naming no side gets the whole
// container, as it does from a furnace.
public abstract class SidedContainerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private LazyOptional<? extends IItemHandler>[] handlers = createHandlers();

    protected SidedContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private LazyOptional<? extends IItemHandler>[] createHandlers() {
        return SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && side != null && !remove) {
            return switch (side) {
                case UP -> handlers[0].cast();
                case DOWN -> handlers[1].cast();
                default -> handlers[2].cast();
            };
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<? extends IItemHandler> handler : handlers) {
            handler.invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        handlers = createHandlers();
    }
}
