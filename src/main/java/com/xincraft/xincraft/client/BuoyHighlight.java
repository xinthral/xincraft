package com.xincraft.xincraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.BuoyBlockEntity;
import com.xincraft.xincraft.entity.BuoyPath;
import com.xincraft.xincraft.item.RoseTintedGlassesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// While the player wears or holds rose-tinted glasses, the links between buoys (block.BuoyBlock)
// around them are traced in dust over the water: yellow, like glowstone, along a path that makes a
// complete loop (every buoy linked to two others), and red, like powered redstone, along one that
// doesn't (it has an end, or a link to a buoy that isn't loaded).
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class BuoyHighlight {
    // How far around the player buoys are found
    private static final double RANGE = 48.0;
    private static final int RESCAN_TICKS = 10;
    // Dust specks put along each leg each tick, per 8 blocks of its length
    private static final double BLOCKS_PER_SPECK = 8.0;
    // Height above the water the line is drawn at
    private static final double HEIGHT = 0.3;
    private static final DustParticleOptions LOOP = new DustParticleOptions(0xFFD84A, 0.9F);
    private static final DustParticleOptions BROKEN = new DustParticleOptions(0xFF2A1A, 0.9F);

    private record Leg(Vec3 from, Vec3 to, boolean loop) {}

    private static List<Leg> legs = List.of();
    private static int rescanIn;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !RoseTintedGlassesItem.isUsing(player)) {
            legs = List.of();
            rescanIn = 0;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        if (--rescanIn <= 0) {
            rescanIn = RESCAN_TICKS;
            legs = scan(level, player.position());
        }
        RandomSource random = level.getRandom();
        for (Leg leg : legs) {
            Vec3 span = leg.to().subtract(leg.from());
            double specks = span.length() / BLOCKS_PER_SPECK;
            // Whole specks, plus a chance of one more for the fraction left over
            int count = (int) specks + (random.nextDouble() < specks - (int) specks ? 1 : 0);
            for (int i = 0; i < count; i++) {
                Vec3 at = leg.from().add(span.scale(random.nextDouble()));
                level.addParticle(leg.loop() ? LOOP : BROKEN, at.x, at.y + HEIGHT, at.z, 0.0, 0.0, 0.0);
            }
        }
    }

    // The legs between the buoys around center, each once, marked as part of a loop or not
    private static List<Leg> scan(Level level, Vec3 center) {
        Map<BlockPos, List<BlockPos>> links = new HashMap<>();
        for (BuoyBlockEntity buoy : BuoyPath.nearby(level, center, RANGE)) {
            links.put(buoy.getBlockPos(), buoy.links());
        }
        Map<BlockPos, Boolean> inLoop = new HashMap<>();
        for (BlockPos start : links.keySet()) {
            if (!inLoop.containsKey(start)) {
                markPath(level, start, links, inLoop);
            }
        }
        List<Leg> found = new ArrayList<>();
        links.forEach((pos, linked) -> {
            for (BlockPos other : linked) {
                // Each leg once, from the lower position; a leg to a buoy out of range is still drawn
                if (!links.containsKey(other) || pos.compareTo(other) < 0) {
                    found.add(new Leg(BuoyPath.point(pos), BuoyPath.point(other), inLoop.getOrDefault(pos, false)));
                }
            }
        });
        return found;
    }

    // Follows the whole path start is on, marking every buoy as part of a complete loop or not
    private static void markPath(Level level, BlockPos start, Map<BlockPos, List<BlockPos>> known, Map<BlockPos, Boolean> inLoop) {
        Set<BlockPos> path = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        path.add(start);
        queue.add(start);
        boolean loop = true;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            List<BlockPos> linked = known.get(pos);
            if (linked == null) {
                // Beyond the scan: look it up directly if it's loaded
                BuoyBlockEntity buoy = BuoyBlockEntity.at(level, pos);
                linked = buoy != null ? buoy.links() : List.of();
            }
            if (linked.size() != BuoyBlockEntity.MAX_LINKS) {
                loop = false;
            }
            for (BlockPos next : linked) {
                if (path.add(next)) {
                    queue.add(next);
                }
            }
        }
        // A loop needs at least three buoys to close
        boolean complete = loop && path.size() >= 3;
        for (BlockPos pos : path) {
            inLoop.put(pos, complete);
        }
    }

    private BuoyHighlight() {}
}
