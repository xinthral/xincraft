package com.xincraft.xincraft.transport.boat;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import com.xincraft.xincraft.transport.train.LinkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * @brief The links from one buoy (BuoyBlock, lower half) to the buoys next to it on its path.
 *
 * Up to MAX_LINKS, each no further than both buoys reach, made with a linking bar.
 *
 * Clients steer ridden boats along the path and draw it for rose-tinted glasses.
 *
 * @see BuoyBlock#linkRange
 * @see transport.train.LinkingBarItem
 */
public class BuoyBlockEntity extends LinkedBlockEntity {
    public static final int MAX_LINKS = 2;
    /**
     * @brief How far an ordinary buoy links.
     *
     * Motorboats follow legs up to this long, or with the advanced antenna upgrade, up to
     * ADVANCED_LINK_RANGE.
     */
    public static final double LINK_RANGE = 32.0;
    /** @brief How far an advanced buoy links to another advanced one. */
    public static final double ADVANCED_LINK_RANGE = 64.0;

    public BuoyBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlockEntities.BUOY.get(), pos, state);
    }

    /**
     * @brief The buoy linked at pos, if it's loaded.
     *
     * @param level The level to work in.
     * @param pos The block's position.
     * @return The buoy, or null if it isn't loaded.
     */
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

    /**
     * @brief How long a leg between two buoys is.
     *
     * @param a One buoy.
     * @param b The other buoy.
     * @return The length, in blocks.
     */
    public static double legLength(BlockPos a, BlockPos b) {
        return distance(a, b);
    }
}
