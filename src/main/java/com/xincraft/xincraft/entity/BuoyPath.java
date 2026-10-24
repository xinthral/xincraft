package com.xincraft.xincraft.entity;

import java.util.ArrayList;
import java.util.List;
import com.xincraft.xincraft.block.BuoyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Finding and following buoy paths (block.BuoyBlock): the straight legs between linked buoys, which
// motorboats with the RC upgrade follow (entity.Motorboat) and rose-tinted glasses show
// (client.BuoyHighlight). Positions are compared on the level, ignoring height.
public final class BuoyPath {
    // How close to a leg a boat has to come to join the path
    public static final double JOIN_DISTANCE = 2.0;
    // Buoys this far above or below are left out
    private static final int MAX_HEIGHT_DIFFERENCE = 8;

    // Where a boat joining a path heads first, and the buoy behind it on that leg
    public record Join(BlockPos target, BlockPos previous) {}

    // The middle of a buoy at water level
    public static Vec3 point(BlockPos buoy) {
        return Vec3.atBottomCenterOf(buoy);
    }

    // Every loaded buoy within range blocks (on the level) of center, from the block entities of the
    // loaded chunks around it rather than block by block
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

    // The nearest leg within JOIN_DISTANCE of a boat at pos, heading toward whichever end the boat
    // is pointed at more; null if there's none
    public static @Nullable Join join(Level level, Vec3 pos, Vec3 heading) {
        Join best = null;
        double bestDistance = JOIN_DISTANCE;
        for (BuoyBlockEntity buoy : nearby(level, pos, BuoyBlockEntity.LINK_RANGE + JOIN_DISTANCE)) {
            BlockPos a = buoy.getBlockPos();
            for (BlockPos b : buoy.links()) {
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

    // The buoy after target on its path, coming from previous: its other link. Null at the end of the
    // line, or if target isn't loaded.
    public static @Nullable BlockPos next(Level level, BlockPos target, @Nullable BlockPos previous) {
        BuoyBlockEntity buoy = BuoyBlockEntity.at(level, target);
        if (buoy == null) {
            return null;
        }
        for (BlockPos link : buoy.links()) {
            if (!link.equals(previous)) {
                return link;
            }
        }
        return null;
    }

    // Distance on the level from pos to the leg between a and b
    private static double distanceToLeg(Vec3 pos, Vec3 a, Vec3 b) {
        Vec3 leg = b.subtract(a).horizontal();
        Vec3 toPos = pos.subtract(a).horizontal();
        double lengthSqr = leg.lengthSqr();
        double t = lengthSqr < 1.0E-6 ? 0.0 : Math.clamp(toPos.dot(leg) / lengthSqr, 0.0, 1.0);
        return toPos.subtract(leg.scale(t)).length();
    }

    private BuoyPath() {}
}
