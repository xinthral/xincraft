package com.xincraft.xincraft.transport.boat;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import com.xincraft.xincraft.transport.train.LinkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// The links from one buoy (BuoyBlock, lower half) to the buoys next to it on its path: up to
// MAX_LINKS, each no further than both buoys reach (see BuoyBlock.linkRange), made with a linking bar
// (transport.train.LinkingBarItem). Clients steer ridden boats along the path and draw it for
// rose-tinted glasses.
public class BuoyBlockEntity extends LinkedBlockEntity {
    public static final int MAX_LINKS = 2;
    // How far an ordinary buoy links, and an advanced one to another advanced one. Motorboats follow
    // legs up to LINK_RANGE long, or with the advanced antenna upgrade, up to ADVANCED_LINK_RANGE.
    public static final double LINK_RANGE = 32.0;
    public static final double ADVANCED_LINK_RANGE = 64.0;

    public BuoyBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlockEntities.BUOY.get(), pos, state);
    }

    // The buoy linked at pos, if it's loaded
    public static @Nullable BuoyBlockEntity at(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof BuoyBlockEntity buoy ? buoy : null;
    }

    @Override
    public int maxLinks() {
        return MAX_LINKS;
    }

    @Override
    public double linkRange() {
        return getBlockState().getBlock() instanceof BuoyBlock buoy ? buoy.linkRange() : LINK_RANGE;
    }

    @Override
    public boolean accepts(LinkedBlockEntity other) {
        return other instanceof BuoyBlockEntity;
    }

    @Override
    protected String messageKey() {
        return "buoy";
    }

    // How long a leg between two buoys is
    public static double legLength(BlockPos a, BlockPos b) {
        return distance(a, b);
    }
}
