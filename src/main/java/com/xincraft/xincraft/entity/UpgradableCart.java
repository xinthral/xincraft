package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.common.MobHolder;

// A minecart or boat that upgrades can be fitted to. Each one holds a CartUpgrades and hands it
// its interactions, pushes, saving and drops.
public interface UpgradableCart extends MobHolder {
    CartUpgrades upgrades();

    @Override
    default boolean holdsMobs() {
        return upgrades().has(CartUpgrade.CELL);
    }

    // Upgrades for certain vehicles (e.g. the sculk sensor's music cue) only go on those
    default boolean accepts(CartUpgrade upgrade) {
        return upgrade.fits().isEmpty() || upgrade.fits().stream().anyMatch(type -> type.isInstance(this));
    }

    // Whether a mob filter (common.MobFilterItem) can be fitted, to choose which mobs come aboard
    default boolean acceptsFilter() {
        return false;
    }
}
