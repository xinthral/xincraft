package com.xincraft.xincraft.transport.boat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * @brief Finding and following buoy paths.
 *
 * The straight legs between linked buoys, which motorboats with the RC upgrade follow and
 * rose-tinted glasses show.
 *
 * Positions are compared on the level, ignoring height. A boat only follows legs up to its reach
 * long: a longer one is the end of the path for it.
 *
 * @see BuoyBlock
 * @see Motorboat
 * @see transport.boat.client.BuoyHighlight
 * @see Motorboat#legReach
 */
public final class BuoyPath {
    /** @brief How close to a leg a boat has to come to join the path. */
    public static final double JOIN_DISTANCE = 2.0;
    /** @brief Buoys this far above or below are left out. */
    private static final int MAX_HEIGHT_DIFFERENCE = 8;

    /**
     * @brief Where a boat joining a path heads first, and the buoy behind it on that leg.
     *
     * @param target The buoy it heads for first.
     * @param previous The buoy behind it on that leg.
     */
    public record Join(BlockPos target, BlockPos previous) {}

    /**
     * @brief The middle of a buoy at water level.
     *
     * @param buoy The buoy's position.
     * @return The point.
     */
    public static Vec3 point(BlockPos buoy) {
        return Vec3.atBottomCenterOf(buoy);
    }

    /**
     * @brief Every loaded buoy within range blocks (on the level) of center, from the block
     *        entities of the loaded chunks around it rather than block by block.
     *
     * @param level The level to work in.
     * @param center Where to search around.
     * @param range How far to search, in blocks.
     * @return The buoys found.
     */
    public static List<BuoyBlockEntity> nearby(Level level, Vec3 center, double range) {
        List<BuoyBlockEntity> found = new ArrayList<>();
        int minX = SectionPos.blockToSectionCoord(center.x - range);
        int maxX = SectionPos.blockToSectionCoord(center.x + range);
        int minZ = SectionPos.blockToSectionCoord(center.z - range);
        int maxZ = SectionPos.blockToSectionCoord(center.z + range);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!level.getChunkSource().hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (blockEntity instanceof BuoyBlockEntity buoy && !buoy.isRemoved()) {
                        BlockPos pos = buoy.getBlockPos();
                        Vec3 offset = point(pos).subtract(center);
                        if (offset.horizontalDistance() <= range && Math.abs(offset.y) <= MAX_HEIGHT_DIFFERENCE) {
                            found.add(buoy);
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * @brief The nearest leg no longer than reach within JOIN_DISTANCE of a boat at pos, heading
     *        toward whichever end the boat is pointed at more.
     *
     * Null if there's none.
     *
     * @param level The level to work in.
     * @param pos The block's position.
     * @param heading Which way the boat is pointed.
     * @param reach The longest leg the boat can follow.
     * @return The leg to join, or null if there's none.
     */
    public static @Nullable Join join(Level level, Vec3 pos, Vec3 heading, double reach) {
        Join best = null;
        double bestDistance = JOIN_DISTANCE;
        for (BuoyBlockEntity buoy : nearby(level, pos, reach + JOIN_DISTANCE)) {
            BlockPos a = buoy.getBlockPos();
            for (BlockPos b : buoy.links()) {
                if (BuoyBlockEntity.legLength(a, b) > reach) {
                    continue;
                }
                double distance = distanceToLeg(pos, point(a), point(b));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    boolean towardB = point(b).subtract(point(a)).dot(heading) >= 0.0;
                    best = towardB ? new Join(b, a) : new Join(a, b);
                }
            }
        }
        return best;
    }

    /**
     * @brief Finds the buoy after target on its path: its other link.
     *
     * @param level The level to work in.
     * @param target The buoy being reached.
     * @param previous The buoy it came from.
     * @param reach The longest leg the boat can follow.
     * @return The next buoy, or null at the end of the line, if the leg there is longer than reach,
     *     or if target isn't loaded.
     */
    public static @Nullable BlockPos next(Level level, BlockPos target, @Nullable BlockPos previous, double reach) {
        BuoyBlockEntity buoy = BuoyBlockEntity.at(level, target);
        if (buoy == null) {
            return null;
        }
        for (BlockPos link : buoy.links()) {
            if (!link.equals(previous)) {
                return BuoyBlockEntity.legLength(target, link) <= reach ? link : null;
            }
        }
        return null;
    }

    /**
     * @brief Distance on the level from pos to the leg between a and b.
     *
     * @param pos The point to measure from.
     * @param a One end of the leg.
     * @param b The other end of the leg.
     * @return The distance, in blocks.
     */
    private static double distanceToLeg(Vec3 pos, Vec3 a, Vec3 b) {
        Vec3 leg = b.subtract(a).horizontal();
        Vec3 toPos = pos.subtract(a).horizontal();
        double lengthSqr = leg.lengthSqr();
        double t = lengthSqr < 1.0E-6 ? 0.0 : Math.clamp(toPos.dot(leg) / lengthSqr, 0.0, 1.0);
        return toPos.subtract(leg.scale(t)).length();
    }

    private BuoyPath() {}
}
