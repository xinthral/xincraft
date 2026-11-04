package com.xincraft.xincraft.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * @brief A vehicle that can keep mobs caged aboard.
 *
 * Lets other features tell a caged mob from a free one without depending on the vehicles
 * themselves.
 */
public interface MobHolder {
    boolean holdsMobs();

    static boolean isCaged(Entity entity) {
        return entity instanceof Mob && entity.getVehicle() instanceof MobHolder holder && holder.holdsMobs();
    }
}
