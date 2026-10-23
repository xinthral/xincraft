package com.xincraft.xincraft.magic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// Finding and drawing from essence reservoirs around a point, for the ritual multiblock to come
public final class EssenceHelper {
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

    private EssenceHelper() {}
}
