package com.xincraft.xincraft.magic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

// Finding, drawing from and filling essence reservoirs around a point (the apothecary altar fills
// them, and draws on them to transmute; the apothecary crucible draws on them)
public final class EssenceHelper {
    // Every loaded reservoir, of any element, within radius blocks of center (a cube)
    public static List<EssenceReservoir> findReservoirs(Level level, BlockPos center, int radius) {
        List<EssenceReservoir> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EssenceReservoir reservoir) {
                found.add(reservoir);
            }
        }
        return found;
    }

    // Every loaded reservoir of the given element within radius blocks of center (a cube)
    public static List<EssenceReservoir> findReservoirs(Level level, BlockPos center, int radius, Element element) {
        List<EssenceReservoir> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EssenceReservoir reservoir
                    && reservoir.getElement() == element) {
                found.add(reservoir);
            }
        }
        return found;
    }

    // Every loaded reservoir within radius blocks of center (a cube), by element
    public static Map<Element, List<EssenceReservoir>> byElement(Level level, BlockPos center, int radius) {
        Map<Element, List<EssenceReservoir>> flora = new EnumMap<>(Element.class);
        for (EssenceReservoir reservoir : findReservoirs(level, center, radius)) {
            flora.computeIfAbsent(reservoir.getElement(), element -> new ArrayList<>()).add(reservoir);
        }
        return flora;
    }

    // Bitmask (by Element ordinal) of the elements whose reservoirs between them hold too little for
    // the cost
    public static int shortfall(Map<Element, List<EssenceReservoir>> reservoirs, Map<Element, Integer> cost) {
        int mask = 0;
        for (Map.Entry<Element, Integer> entry : cost.entrySet()) {
            if (total(reservoirs.getOrDefault(entry.getKey(), List.of())) < entry.getValue()) {
                mask |= 1 << entry.getKey().ordinal();
            }
        }
        return mask;
    }

    // Takes amount essence of the element out of the reservoirs, with a stream of its dust from each
    // one drawn on to the point
    public static void draw(ServerLevel level, Element element, List<EssenceReservoir> reservoirs, int amount, Vec3 to) {
        Map<EssenceReservoir, Integer> before = new HashMap<>();
        reservoirs.forEach(reservoir -> before.put(reservoir, reservoir.getEssence()));
        drain(reservoirs, amount);
        for (EssenceReservoir reservoir : reservoirs) {
            if (reservoir.getEssence() < before.get(reservoir) && reservoir instanceof BlockEntity flower) {
                stream(level, element, Vec3.atCenterOf(flower.getBlockPos()), to);
            }
        }
    }

    // Total essence the reservoirs hold
    public static int total(List<EssenceReservoir> reservoirs) {
        return reservoirs.stream().mapToInt(EssenceReservoir::getEssence).sum();
    }

    // Takes amount essence out of the reservoirs, fullest first, but only if they hold enough
    // between them; returns whether it did
    public static boolean drain(List<EssenceReservoir> reservoirs, int amount) {
        if (total(reservoirs) < amount) {
            return false;
        }
        List<EssenceReservoir> fullestFirst = new ArrayList<>(reservoirs);
        fullestFirst.sort((a, b) -> Integer.compare(b.getEssence(), a.getEssence()));
        int remaining = amount;
        for (EssenceReservoir reservoir : fullestFirst) {
            if (remaining <= 0) {
                break;
            }
            remaining -= reservoir.extract(remaining, false);
        }
        return true;
    }

    // Puts amount essence into the reservoirs, emptiest first, and returns the ones that took some.
    // Whatever doesn't fit is left over.
    public static List<EssenceReservoir> fill(List<EssenceReservoir> reservoirs, int amount) {
        List<EssenceReservoir> emptiestFirst = new ArrayList<>(reservoirs);
        emptiestFirst.sort((a, b) -> Integer.compare(a.getEssence(), b.getEssence()));
        List<EssenceReservoir> filled = new ArrayList<>();
        int remaining = amount;
        for (EssenceReservoir reservoir : emptiestFirst) {
            if (remaining <= 0) {
                break;
            }
            int added = reservoir.insert(remaining, false);
            if (added > 0) {
                remaining -= added;
                filled.add(reservoir);
            }
        }
        return filled;
    }

    // A line of the element's dust between two points, showing essence flowing along it
    public static void stream(ServerLevel level, Element element, Vec3 from, Vec3 to) {
        int steps = Math.max(1, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, i / (double) steps);
            level.sendParticles(element.particle(), point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private EssenceHelper() {}
}
