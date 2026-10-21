package com.xincraft.xincraft.entity;

// A minecart or motorboat that upgrades can be fitted to. Each one holds a CartUpgrades and hands it
// its interactions, pushes, saving and drops.
public interface UpgradableCart {
    CartUpgrades upgrades();

    // Upgrades for certain vehicles (e.g. the sculk sensor's music cue) only go on those
    default boolean accepts(CartUpgrade upgrade) {
        return upgrade.fits().isEmpty() || upgrade.fits().stream().anyMatch(type -> type.isInstance(this));
    }
}
